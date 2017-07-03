package biocode.fims.rest.services.rest.subResources;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.auth.PasswordHash;
import biocode.fims.entities.User;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.UserEntityGraph;
import biocode.fims.rest.filters.Admin;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.serializers.Views;
import biocode.fims.service.UserService;
import biocode.fims.utils.EmailUtils;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Locale;

/**
 * @author RJ Ewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
public class UsersResource extends FimsService {
    private final UserService userService;
    private final MessageSource messageSource;

    @Autowired
    public UsersResource(UserService userService, MessageSource messageSource, FimsProperties props) {
        super(props);
        this.userService = userService;
        this.messageSource = messageSource;
    }

    /**
     * get all registered users
     *
     * @responseMessage 403 you are not a project admin `biocode.fims.utils.ErrorInfo
     */
    @UserEntityGraph("User.withProjects")
    @JsonView(Views.Detailed.class)
    @GET
    @Admin
    public List<User> getUsers() {
        return userService.getUsers();
    }

    /**
     * get user
     *
     * @responseMessage 403 you must be authenticated to access a users profile `biocode.fims.utils.ErrorInfo
     * @responseMessage 403 you must be a project admin to access another user's profile `biocode.fims.utils.ErrorInfo
     */
    @UserEntityGraph("User.withProjects")
    @JsonView(Views.Detailed.class)
    @Path("/{username}")
    @GET
    @Authenticated
    public User getUser(@PathParam("username") String username) {
        if (!userContext.getUser().getUsername().equals(username) &&
                !userService.isAProjectAdmin(userContext.getUser(), props.appRoot())) {
            throw new ForbiddenRequestException("You must be a project admin to access another user's profile");
        }

        return userService.getUser(username);
    }

    /**
     * create a new user, and send an email to the new user with login information
     * @param user
     * @responseMessage 400 invalid user object `biocode.fims.utils.ErrorInfo
     */
    @JsonView(Views.Detailed.class)
    @UserEntityGraph("User.withProjects")
    @POST
    @Admin
    @Authenticated
    public User createUser(User user) {

        if (!isValidUser(user, true)) {
            throw new BadRequestException("username, password, firstName, lastName, email, and institution are required");
        }

        // check that a valid email is given
        if (!user.getEmail().toUpperCase().matches("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}")) {
            throw new BadRequestException("please enter a valid email");
        }

        if (userService.getUser(user.getUsername()) != null) {
            throw new BadRequestException("username already exists");
        }

        String password = user.getPassword();

        // hash the users password
        user.setPassword(PasswordHash.createHash(password));
        user = userService.create(user);

        EmailUtils.sendEmail(
                user.getEmail(),
                new String[] {userContext.getUser().getEmail()},
                messageSource.getMessage(
                        "NewUserEmail__SUBJECT",
                        null,
                        Locale.US),
                messageSource.getMessage(
                        "NewUserEmail__BODY",
                        new Object[] {user.getFirstName(), user.getLastName(), props.appRoot(), user.getUsername(), password},
                        Locale.US)
        );

        return user;
    }

    /**
     * update a users profile
     * @param username
     * @param user
     * @responseMessage 403 you must be authenticated to update a users profile `biocode.fims.utils.ErrorInfo
     * @responseMessage 403 you must be a project admin to update another user's profile `biocode.fims.utils.ErrorInfo
     */
    @JsonView(Views.Detailed.class)
    @UserEntityGraph("User.withProjects")
    @Path("/{username}")
    @PUT
    @Authenticated
    public User updateUser(@PathParam("username") String username,
                           User user) {
        if (!userContext.getUser().getUsername().equals(username) &&
                !userService.isAProjectAdmin(userContext.getUser(), props.appRoot())) {
            throw new ForbiddenRequestException("You must be a project admin to access another user's profile");
        }

        User existingUser = userService.getUser(username);
        if (existingUser == null) {
            throw new FimsRuntimeException("user not found", 404);
        }

        if (!isValidUser(user, false)) {
            throw new BadRequestException("firstName, lastName, email, and institution are required");
        }

        updateExistingUser(existingUser, user);
        return userService.update(existingUser);
    }

    /**
     * method to transfer the updated {@link User} object to an existing {@link User}. This
     * allows us to control which properties can be updated.
     * Currently allows updating of the following properties : password, firstName, lastName, email, and institution
     * @param existingUser
     * @param newUser
     */
    private void updateExistingUser(User existingUser, User newUser) {
        if (!org.apache.commons.lang.StringUtils.isEmpty(newUser.getPassword())) {
            //user is updating their password, so we need to hash it
            existingUser.setPassword(PasswordHash.createHash(newUser.getPassword()));
        }

        existingUser.setFirstName(newUser.getFirstName());
        existingUser.setLastName(newUser.getLastName());
        existingUser.setEmail(newUser.getEmail());
        existingUser.setInstitution(newUser.getInstitution());
    }

    /**
     * check that all required fields are present on the User object
     * @param user
     * @return
     */
    private boolean isValidUser(User user, boolean requirePassword) {
        return !StringUtils.isEmpty(user.getUsername()) &&
                (!StringUtils.isEmpty(user.getPassword()) || !requirePassword) &&
                !StringUtils.isEmpty(user.getFirstName()) &&
                !StringUtils.isEmpty(user.getLastName()) &&
                !StringUtils.isEmpty(user.getEmail()) &&
                !StringUtils.isEmpty(user.getInstitution());
    }
}
