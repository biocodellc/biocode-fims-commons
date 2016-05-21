package biocode.fims.service;

import biocode.fims.auth.PasswordHash;
import biocode.fims.entities.Project;
import biocode.fims.entities.User;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.repositories.UserRepository;
import biocode.fims.settings.SettingsManager;
import biocode.fims.utils.StringGenerator;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Timestamp;
import java.util.Calendar;


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
    private final ProjectService projectService;
    private final SettingsManager settingsManager;

    @Autowired
    public UserService(UserRepository userRepository, ProjectService projectService,
                       SettingsManager settingsManager) {
        this.userRepository = userRepository;
        this.projectService = projectService;
        this.settingsManager = settingsManager;
    }

    public void create(User user, int projectId) {
        userRepository.save(user);
        entityManager.refresh(user);

        addUserToProject(user, projectId);
    }

    public void update(User user) {
        userRepository.save(user);
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
        User user = getUser(username);

        if (user != null && !user.getPassword().isEmpty()) {
            if (PasswordHash.validatePassword(password, user.getPassword())) {
                return user;
            }
        }
        return null;
    }

    /**
     * add a {@link} to a {@link Project}
     * @param user
     * @param projectId
     */
    public void addUserToProject(User user, int projectId) {
        Project project = entityManager.getReference(Project.class, projectId);
        user.getProjectsMemberOf().add(project);

        userRepository.save(user);
    }

    /**
     * Find a user given a resetToken. A {@link User} will be returned if the resetToken exists and has not expired
     * @param resetToken
     * @return
     */
    public User getUserByResetToken(String resetToken) {
        return userRepository.findOneByResetToken(resetToken);
    }

    public User generateResetToken(String username) {
        User user = getUser(username);

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
}
