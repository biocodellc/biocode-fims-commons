package biocode.fims.bcid;

import biocode.fims.fimsExceptions.ServerErrorException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Mint new expeditions.  Includes the automatic creation of a core set of entity types
 */
@Deprecated
public class ProjectMinter {

    /**
     * The constructor defines the class-level variables used when minting Expeditions.
     * It defines a generic set of entities (process, information content, objects, agents)
     * that can be used for any expedition.
     */
    public ProjectMinter() {
    }

    /**
     * Find the BCID that denotes the validation file location for a particular expedition
     *
     * @param projectId defines the projectId to lookup
     *
     * @return returns the BCID for this expedition and conceptURI combination
     */
    public String getValidationXML(Integer projectId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();

        try {
            String query = "select \n" +
                    "validation_xml\n" +
                    "from \n" +
                    " projects\n" +
                    "where \n" +
                    "id=?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, projectId);

            rs = stmt.executeQuery();
            rs.next();
            return rs.getString("validation_xml");
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "Trouble getting Configuration File", e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
    }

}

