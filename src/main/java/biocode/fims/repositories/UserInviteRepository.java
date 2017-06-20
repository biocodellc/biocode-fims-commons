package biocode.fims.repositories;

import biocode.fims.models.User;
import biocode.fims.models.UserInvite;
import biocode.fims.repositories.customOperations.UserCustomOperations;
import biocode.fims.repositories.customOperations.UserInviteCustomOperations;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * This repositories provides CRUD operations for {@link User} objects
 */
@Transactional
public interface UserInviteRepository extends Repository<UserInvite, UUID>, UserInviteCustomOperations {

    @Modifying
    void delete(UserInvite invite);

    UserInvite save(UserInvite invite);
}
