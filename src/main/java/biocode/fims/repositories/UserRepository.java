package biocode.fims.repositories;

import biocode.fims.entities.User;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * This repositories provides CRUD operations for {@link User} objects
 */
@Transactional
public interface UserRepository extends Repository<User, Integer>, JpaSpecificationExecutor {

    @Modifying
    void delete(User user);

    void save(User user);

    User findByUserId(int userId);

    User findByUsername(String username);

    @Query("select u from User u where u.passwordResetToken = :resetToken and u.passwordResetExpiration > current_timestamp")
    User findOneByResetToken(@Param("resetToken") String resetToken);

    Set<User> findAll();
}
