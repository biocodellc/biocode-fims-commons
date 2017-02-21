package biocode.fims.service;

import biocode.fims.auth.PasswordHash;
import biocode.fims.entities.Project;
import biocode.fims.entities.User;
import biocode.fims.repositories.UserRepository;
import biocode.fims.settings.SettingsManager;
import biocode.fims.utils.StringGenerator;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;


/**
 * Service class for handling {@link User} persistence
 */
@Transactional
@Service
public class UserService {
    private static final Logger logger = Logger.getLogger(UserService.class);

    @PersistenceContext(unitName = "entityManagerFactory")
    private EntityManager entityManager;

    private final UserRepository userRepository;
    private final SettingsManager settingsManager;

    @Autowired
    public UserService(UserRepository userRepository, SettingsManager settingsManager) {
        this.userRepository = userRepository;
        this.settingsManager = settingsManager;
    }

    public User create(User user) {
        return userRepository.save(user);
    }

    public User update(User user) {
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getUser(int userId) {
        return userRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public User getUser(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public User getUser(String username, String password) {
        User user = getUserWithMemberProjects(username);

        if (user != null && !user.getPassword().isEmpty()) {
            if (PasswordHash.validatePassword(password, user.getPassword())
                    && userBelongsToInstanceProject(user)) {
                return user;
            }
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    /**
     * checks to see if the user is a member of a Project that exists for this biocode-fims instance
     *
     * @param user
     * @return
     */
    @Transactional(readOnly = true)
    public boolean userBelongsToInstanceProject(User user) {
        String appRoot = settingsManager.retrieveValue("appRoot");

        PersistenceUnitUtil unitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        if (!unitUtil.isLoaded(user, "projectsMemberOf")) {
            user = getUserWithMemberProjects(user.getUsername());
        }

        for (Project project : user.getProjectsMemberOf()) {
            if (project.getProjectUrl().equals(appRoot))
                return true;
        }

        logger.warn("user exists, but is not a member of a project at this biocode-fims instance");
        return false;
    }

    /**
     * Find a user given a resetToken. A {@link User} will be returned if the resetToken exists and has not expired
     *
     * @param resetToken
     * @return
     */
    public User getUserByResetToken(String resetToken) {
        return userRepository.findOneByResetToken(resetToken);
    }

    public User generateResetToken(String username) {
        User user = getUserWithMemberProjects(username);

        if (user != null) {
            StringGenerator sg = new StringGenerator();
            user.setPasswordResetToken(sg.generateString(20));
            // set for 24hrs in future
            user.setPasswordResetExpiration(new Timestamp(
                    Calendar.getInstance().getTime().getTime() + (1000 * 60 * 60 * 24))
            );
            update(user);
        }

        return user;
    }

    /**
     * checks to see if the user is an admin of any project
     *
     * @param user
     * @return
     */
    public boolean isAProjectAdmin(User user, String appRoot) {
        PersistenceUnitUtil unitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        if (!unitUtil.isLoaded(user, "projects")) {
            user = getUserWithProjects(user.getUsername());
        }

        for (Project project: user.getProjects()) {
            if (project.getProjectUrl().equals(appRoot)) {
                return true;
            }
        }
        return false;
    }

    public User getUserWithMemberProjects(String username) {
        return userRepository.getUserWithMemberProjects(username);
    }

    public User getUserWithProjects(String username) {
        return userRepository.getUserWithProjects(username);
    }

    /**
     * Returns the @param user with the loaded @param userEntityGraph
     *
     * @param user
     * @param userEntityGraph
     * @return
     */
    public User loadUserEntityGraph(User user, String userEntityGraph) {
        PersistenceUnitUtil unitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

        for (AttributeNode node : entityManager.getEntityGraph(userEntityGraph).getAttributeNodes()) {

            String attributeName = node.getAttributeName();
            if (attributeName != null && !unitUtil.isLoaded(user, attributeName)) {
                return userRepository.getUser(user.getUserId(), userEntityGraph);
            }

        }

        return user;
    }
}
