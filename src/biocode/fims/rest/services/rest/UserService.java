package biocode.fims.rest.services.rest;

import biocode.fims.auth.Authenticator;
import biocode.fims.auth.Authorizer;
import biocode.fims.bcid.ProjectMinter;
import biocode.fims.bcid.UserMinter;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.filters.Admin;
import biocode.fims.rest.filters.Authenticated;
import org.json.simple.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Hashtable;

/**
 * The REST Interface for dealing with users. Includes user creation and profile updating.
 */
@Path("users")
public class UserService extends FimsService {

    /**
     * Service to create a new user.
     * @param createUser
     * @param password
     * @param firstName
     * @param lastName
     * @param email
     * @param institution
     * @param projectId
     * @return
     */
    @POST
    @Authenticated
    @Admin
    @Path("/admin/create")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createUser(@FormParam("username") String createUser,
                               @FormParam("password") String password,
                               @FormParam("firstName") String firstName,
                               @FormParam("lastName") String lastName,
                               @FormParam("email") String email,
                               @FormParam("institution") String institution,
                               @FormParam("projectId") Integer projectId) {

        if ((createUser == null || createUser.isEmpty()) ||
                (password == null || password.isEmpty()) ||
                (firstName == null || firstName.isEmpty()) ||
                (lastName == null || lastName.isEmpty()) ||
                (email == null || email.isEmpty()) ||
                (institution == null) || institution.isEmpty()) {
            throw new BadRequestException("all fields are required");
        }

        // check that a valid email is given
        if (!email.toUpperCase().matches("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}")) {
            throw new BadRequestException("please enter a valid email");
        }

        Hashtable<String, String> userInfo = new Hashtable<String, String>();
        userInfo.put("username", createUser);
        userInfo.put("firstName", firstName);
        userInfo.put("lastName", lastName);
        userInfo.put("email", email);
        userInfo.put("institution", institution);
        userInfo.put("password", password);

        UserMinter u = new UserMinter();
        ProjectMinter p = new ProjectMinter();

        if (u.checkUsernameExists(createUser)) {
            throw new BadRequestException("username already exists");
        }
        // check if the user is this project's admin
        if (!p.isProjectAdmin(username, projectId)) {
            throw new ForbiddenRequestException("You can't add a user to a project that you're not an admin.");
        }
        u.createUser(userInfo, projectId);
        return Response.ok("{\"success\": \"successfully created new user\"}").build();
    }

    /**
     * Service for a user to update their profile.
     * @param firstName
     * @param lastName
     * @param email
     * @param institution
     * @param oldPassword
     * @param newPassword
     * @param returnTo
     * @returns either error message or the url to redirect to upon success
     */
    @POST
    @Authenticated
    @Path("/profile/update")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProfile(@FormParam("firstName") String firstName,
                                  @FormParam("lastName") String lastName,
                                  @FormParam("email") String email,
                                  @FormParam("institution") String institution,
                                  @FormParam("oldPassword") String oldPassword,
                                  @FormParam("newPassword") String newPassword,
                                  @FormParam("username") String updateUser,
                                  @QueryParam("return_to") String returnTo) {
        Authorizer authorizer = new Authorizer();

        if (!username.equals(updateUser.trim()) && !authorizer.userProjectAdmin(username)) {
            throw new ForbiddenRequestException("You must be a project admin to update someone else's profile.");
        }

        Boolean adminAccess = false;
        if (!username.equals(updateUser.trim()) && authorizer.userProjectAdmin(username))
            adminAccess = true;

        Hashtable<String, String> update = new Hashtable<String, String>();

        // Only update user's password if both old_password and new_password fields contain values and the user is updating
        // their own profile
        if (!adminAccess) {
            if (!oldPassword.isEmpty() && !newPassword.isEmpty()) {
                Authenticator myAuth = new Authenticator();
                // Call the login function to verify the user's old_password
                Boolean valid_pass = myAuth.login(updateUser, oldPassword);

                // If user's old_password matches stored pass, then update the user's password to the new value
                if (valid_pass) {
                    Boolean success = myAuth.setHashedPass(updateUser, newPassword);
                    if (!success) {
                        throw new ServerErrorException("Server Error", "User not found");
                    }
                    // Make sure that the hasSetPassword field is 1 (true) so they aren't asked to change their password after login
                    else {
                        update.put("hasSetPassword", "1");
                    }
                } else {
                    throw new BadRequestException("Wrong Password");
                }

            }
        } else {
            // set new password if given
            if (!newPassword.isEmpty()) {
                Authenticator authenticator = new Authenticator();
                Boolean success = authenticator.setHashedPass(updateUser, newPassword);
                if (!success) {
                    throw new BadRequestException("user: " + updateUser + "not found");
                } else {
                    // Make the user change their password next time they login
                    update.put("hasSetPassword", "0");
                }
            }
        }

        // Check if any other fields should be updated
        UserMinter u = new UserMinter();

        if (!firstName.equals(u.getFirstName(updateUser))) {
            update.put("firstName", firstName);
        }
        if (!lastName.equals(u.getLastName(updateUser))) {
            update.put("lastName", lastName);
        }
        if (!email.equals(u.getEmail(updateUser))) {
            // check that a valid email is given
            if (email.toUpperCase().matches("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}")) {
                update.put("email", email);
            } else {
                throw new BadRequestException("please enter a valid email");
            }
        }
        if (!institution.equals(u.getInstitution(updateUser))) {
            update.put("institution", institution);
        }

        if (!update.isEmpty()) {
            Boolean success = u.updateProfile(update, updateUser);
            if (!success) {
                throw new ServerErrorException("Server Error", "User not found");
            }
        }

        JSONObject response = new JSONObject();
        response.put("adminAccess", adminAccess);
        if (returnTo != null) {
            response.put("returnTo", returnTo);
        }
        return Response.ok(response.toJSONString()).build();
    }

    /**
     * Service for oAuth client apps to retrieve a user's profile information.
     * @return
     */
    @GET
    @Path("/profile")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserData() {
        if (accessToken != null) {
            UserMinter u = new UserMinter();
            JSONObject response = u.getOauthProfile(accessToken);
            return Response.ok(response.toJSONString()).build();
        }
        throw new BadRequestException("invalid_grant", "access_token was null");
    }

    /**
     * Service for a user to exchange their reset token in order to update their password
     *
     * @param password
     * @param token
     */
    @POST
    @Path("/resetPassword")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetPassword(@FormParam("password") String password,
                                  @FormParam("token") String token) {
        if (token == null || token.isEmpty()) {
            throw new BadRequestException("Invalid Reset Token");
        }

        if (password.isEmpty()) {
            throw new BadRequestException("Password must not be empty");
        }

        Authorizer authorizer = new Authorizer();
        Authenticator authenticator = new Authenticator();

        if (!authorizer.validResetToken(token)) {
            throw new BadRequestException("Expired Reset Token");
        }

        Boolean resetPass = authenticator.resetPass(token, password);

        if (!resetPass) {
            throw new ServerErrorException("Server Error", "Error while updating user's password");
        }
        return Response.ok("{\"success\":\"Successfully updated your password\"}").build();
    }

}
