package biocode.fims.repositories.customOperations;

import biocode.fims.models.UserInvite;

import java.util.UUID;

/**
 * @author RJ Ewing
 */
public interface UserInviteCustomOperations {

    UserInvite getInvite(UUID id, long expirationInterval);
}
