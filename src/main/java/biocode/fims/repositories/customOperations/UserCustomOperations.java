package biocode.fims.repositories.customOperations;

import biocode.fims.entities.User;

/**
 * @author RJ Ewing
 */
public interface UserCustomOperations {

    User getUser(int userId, String entityGraph);
}
