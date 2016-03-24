package biocode.fims.auth;

import biocode.fims.bcid.BcidDatabase;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.settings.SettingsManager;
import biocode.fims.utils.StringGenerator;
import org.apache.commons.cli.*;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.Calendar;
import java.util.Hashtable;

/**
 * Used for all authentication duties such as login, changing passwords, creating users, resetting passwords, etc.
 */
public class Authenticator {
    protected SettingsManager sm;
    private static Logger logger = LoggerFactory.getLogger(Authenticator.class);

    /**
     * Constructor that initializes the class level variables
     */
    public Authenticator() {
        // Initialize settings manager
        sm = SettingsManager.getInstance();
    }


    /**
     * Public method to verify a users password
     *
     * @return
     */
    public Boolean login(String username, String password) {

        String hashedPass = getHashedPass(username);

        if (!hashedPass.isEmpty()) {
            try {
                return PasswordHash.validatePassword(password, hashedPass);
            } catch (InvalidKeySpecException e) {
                throw new ServerErrorException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new ServerErrorException(e);
            }
        }

        return false;
    }

    /**
     * retrieve the user's hashed password from the db
     *
     * @return
     */
    private String getHashedPass(String username) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();
        try {
            String selectString = "SELECT password FROM users WHERE username = ?";
            //System.out.println(selectString + " " + username);
            stmt = conn.prepareStatement(selectString);

            stmt.setString(1, username);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("password");
            }

        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
        return null;
    }

    /**
     * Takes a new password for a user and stores a hashed version
     *
     * @param password
     *
     * @return true upon successful update, false when nothing was updated (most likely due to user not being found)
     */
    public Boolean setHashedPass(String username, String password) {
        PreparedStatement stmt = null;
        Connection conn = BcidDatabase.getConnection();

        String hashedPass = createHash(password);

        // Store the hashed password in the db
        try {
            String updateString = "UPDATE users SET password = ? WHERE username = ?";
            stmt = conn.prepareStatement(updateString);

            stmt.setString(1, hashedPass);
            stmt.setString(2, username);
            Integer result = stmt.executeUpdate();

            if (result == 1) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            BcidDatabase.close(conn, stmt, null);
        }
    }

    /**
     * Update the user's password associated with the given token.
     *
     * @param token
     * @param password
     *
     * @return
     */
    public Boolean resetPass(String token, String password) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();
        try {
            String username = null;
            String sql = "SELECT username FROM users where passwordResetToken = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, token);
            rs = stmt.executeQuery();

            if (rs.next()) {
                username = rs.getString("username");
            }
            if (username != null) {
                BcidDatabase.close(null, stmt, null);
                String updateSql = "UPDATE users SET passwordResetToken = null, passwordResetExpiration = null WHERE username = \"" + username + "\"";
                stmt = conn.prepareStatement(updateSql);
                stmt.executeUpdate();

                return setHashedPass(username, password);
            }
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error resetting password.", e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
        return false;
    }

    /**
     * create a hash of a password string to be stored in the db
     *
     * @param password
     *
     * @return
     */
    public String createHash(String password) {
        try {
            return PasswordHash.createHash(password);
        } catch (NoSuchAlgorithmException e) {
            throw new ServerErrorException(e);
        } catch (InvalidKeySpecException e) {
            throw new ServerErrorException(e);
        }
    }

    /**
     * create a user given a username and password
     *
     * @param userInfo
     *
     * @return
     */
    public void createUser(Hashtable<String, String> userInfo) {
        PreparedStatement stmt = null;
        String hashedPass = createHash(userInfo.get("password"));
        Connection conn = BcidDatabase.getConnection();

        try {
            String insertString = "INSERT INTO users (username, password, email, firstName, lastName, institution)" +
                    " VALUES(?,?,?,?,?,?)";
            stmt = conn.prepareStatement(insertString);

            stmt.setString(1, userInfo.get("username"));
            stmt.setString(2, hashedPass);
            stmt.setString(3, userInfo.get("email"));
            stmt.setString(4, userInfo.get("firstName"));
            stmt.setString(5, userInfo.get("lastName"));
            stmt.setString(6, userInfo.get("institution"));

            stmt.execute();
            return;
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            BcidDatabase.close(conn, stmt, null);
        }
    }

    /**
     * Check if the user has set their own password or if they are using a temporary password
     *
     * @return
     */
    public Boolean userSetPass(String username) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();
        Boolean hasSetPassword = false;
        try {
            String selectString = "SELECT hasSetPassword FROM users WHERE username = ?";
            stmt = conn.prepareStatement(selectString);

            stmt.setString(1, username);

            rs = stmt.executeQuery();

            if (rs.next()) {
                hasSetPassword = rs.getBoolean("hasSetPassword");
            }
        } catch (SQLException e) {
            logger.warn("SQLException thrown", e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
        return hasSetPassword;
    }

    /**
     * In the case where a user has forgotten their password, generate a token that can be used to create a new
     * password.
     *
     * @param username
     *
     * @return JSONObject containing the user's email and the resetToken
     */
    public JSONObject generateResetToken(String username) {
        String email = null;
        JSONObject resetToken = new JSONObject();
        String sql = "SELECT email FROM users WHERE username = ?";

        PreparedStatement stmt = null;
        PreparedStatement stmt2 = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();
        try {
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, username);
            rs = stmt.executeQuery();

            if (rs.next()) {
                email = rs.getString("email");
            }

            if (email == null) {
                throw new BadRequestException("User not found", "email is null for user: " + username);
            }
            StringGenerator sg = new StringGenerator();
            String token = sg.generateString(20);
            // set for 24hrs in future
            Timestamp ts = new Timestamp(Calendar.getInstance().getTime().getTime() + (1000 * 60 * 60 * 24));

            String updateSql = "UPDATE users SET " +
                    "passwordResetToken = \"" + token + "\", " +
                    "passwordResetExpiration = \"" + ts + "\" " +
                    "WHERE username = ?";
            stmt2 = conn.prepareStatement(updateSql);

            stmt2.setString(1, username);

            stmt2.executeUpdate();

            resetToken.put("email", email);
            resetToken.put("resetToken", token);

            return resetToken;
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error while generating reset token.", "db error retrieving email for user "
                    + username, e);
        } finally {
            BcidDatabase.close(null, stmt, rs);
            BcidDatabase.close(conn, stmt2, null);
        }
    }

    /**
     * This will update a given users password. Better to use the web interface
     *
     * @param args username and password
     */
    public static void main(String args[]) {

        // Some classes to help us
        CommandLineParser clp = new GnuParser();
        CommandLine cl;

        Options options = new Options();
        options.addOption("U", "username", true, "Username you would like to set a password for");
        options.addOption("P", "password", true, "The temporary password you would like to set");




        try {
            cl = clp.parse(options, args);
        } catch (UnrecognizedOptionException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        } catch (ParseException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        if (!cl.hasOption("U") || (!cl.hasOption("P") && cl.hasOption("ldap"))) {
            System.out.println("You must enter a username and a password");
            return;
        }

        String username = cl.getOptionValue("U");
        String password = cl.getOptionValue("P");

        Authenticator authenticator = new Authenticator();

        Boolean success = authenticator.setHashedPass(username, password);

        if (!success) {
            System.out.println("Error updating password for " + username);
            return;
        }

        // change hasSetPassword field to 0 so user has to create new password next time they login
        Connection conn = BcidDatabase.getConnection();
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            Integer result = stmt.executeUpdate("UPDATE users SET hasSetPassword=\"0\" WHERE username=\"" + username + "\"");

            if (result == 0) {
                System.out.println("Error updating hasSetPassword value to 0 for " + username);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            BcidDatabase.close(conn, stmt, null);
        }

        System.out.println("Successfully set new password for " + username);

    }
}


