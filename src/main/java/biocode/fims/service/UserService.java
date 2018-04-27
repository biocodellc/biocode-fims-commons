package biocode.fims.service;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.auth.PasswordHash;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.UserCode;
import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.models.UserInvite;
import biocode.fims.repositories.ProjectRepository;
import biocode.fims.repositories.UserInviteRepository;
import biocode.fims.repositories.UserRepository;
import biocode.fims.utils.EmailUtils;
import biocode.fims.utils.StringGenerator;
import org.apache.commons.lang.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.*;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.SECONDS;


/**
 * Service class for handling {@link User} persistence
 */
@Transactional
@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static long INVITE_EXPIRATION_INTEVAL = SECONDS.convert(7, DAYS); // also need to change postgresql cleanup trigger

    @PersistenceContext(unitName = "entityManagerFactory")
    private EntityManager entityManager;

    private final UserRepository userRepository;
    private final UserInviteRepository userInviteRepository;
    private final MessageSource messageSource;
    private final ProjectRepository projectRepository;
    private final FimsProperties props;

    @Autowired
    public UserService(UserRepository userRepository, UserInviteRepository userInviteRepository,
                       ProjectRepository projectRepository,
                       FimsProperties props, MessageSource messageSource) {
        this.userRepository = userRepository;
        this.userInviteRepository = userInviteRepository;
        this.projectRepository = projectRepository;
        this.messageSource = messageSource;
        this.props = props;
    }


    public User create(User user, String password, UUID inviteId) {
        UserInvite invite = userInviteRepository.getInvite(inviteId, INVITE_EXPIRATION_INTEVAL);

        if (invite == null) {
            throw new FimsRuntimeException(UserCode.INVALID_INVITE, 400);
        }

        if (!user.isValid(true)) {
            throw new FimsRuntimeException(UserCode.INVALID, 400);
        }

        if (getUser(user.getUsername()) != null) {
            throw new FimsRuntimeException(UserCode.DUPLICATE_USERNAME, 400);
        }

        // hash the users password
        user.setPassword(PasswordHash.createHash(password));

        // add user to project members
        Project project = projectRepository.getProjectByProjectId(invite.getProject().getProjectId(), "Project.withMembers");
        project.getProjectMembers().add(user);
        user.getProjectsMemberOf().add(project);

        user = userRepository.save(user);
        userInviteRepository.delete(invite);

        return user;
    }

    @Deprecated
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

        PersistenceUnitUtil unitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        if (!unitUtil.isLoaded(user, "projectsMemberOf")) {
            user = getUserWithMemberProjects(user.getUsername());
        }

        for (Project project : user.getProjectsMemberOf()) {
            if (project.getProjectUrl().equals(props.appRoot()))
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

    public boolean sendResetEmail(String username) {
        User user = getUserWithMemberProjects(username);

        if (user != null) {
            user.setPasswordResetToken(StringGenerator.generateString(20));
            // set for 24hrs in future
            user.setPasswordResetExpiration(new Timestamp(
                    Calendar.getInstance().getTime().getTime() + (1000 * 60 * 60 * 24))
            );
            update(user);


            String resetTokenUrl = props.appRoot() + props.resetPasswordPath();

            Map<String, String> paramMap = new HashMap<>();
            paramMap.put("token", user.getPasswordResetToken());

            EmailUtils.sendEmail(
                    user.getEmail(),
                    messageSource.getMessage(
                            "UserResetPasswordEmail__SUBJECT",
                            null,
                            Locale.US),
                    messageSource.getMessage(
                            "UserResetPasswordEmail__BODY",
                            new Object[]{StrSubstitutor.replace(resetTokenUrl, paramMap)},
                            Locale.US)
            );

            return true;
        }

        return false;
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

        for (Project project : user.getProjects()) {
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

    public boolean userExists(String email) {
        return userRepository.userExists(email);
    }

    public UserInvite inviteUser(String email, Project project, User inviter) {
        try {
            UserInvite invite = userInviteRepository.save(new UserInvite(email, project, inviter));

            String accountCreateUrl = props.appRoot() + props.accountCreatePath();

            Map<String, String> paramMap = new HashMap<>();
            paramMap.put("id", invite.getId().toString());
            paramMap.put("email", invite.getEmail());


            EmailUtils.sendEmail(
                    email,
                    messageSource.getMessage(
                            "UserInviteEmail__SUBJECT",
                            null,
                            Locale.US),
                    messageSource.getMessage(
                            "UserInviteEmail__BODY",
                            new Object[]{StrSubstitutor.replace(accountCreateUrl, paramMap), project.getProjectTitle()},
                            Locale.US)
            );

            return invite;

        } catch (DataIntegrityViolationException e) {
            throw new FimsRuntimeException(UserCode.DUPLICATE_INVITE, 400);
        }
    }
}
