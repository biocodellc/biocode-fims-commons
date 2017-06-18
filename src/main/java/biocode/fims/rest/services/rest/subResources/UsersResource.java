package biocode.fims.rest.services.rest.subResources;

import biocode.fims.auth.PasswordHash;
import biocode.fims.models.User;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.UserEntityGraph;
import biocode.fims.rest.filters.Admin;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.serializers.Views;
import biocode.fims.service.UserService;
import biocode.fims.settings.SettingsManager;
import biocode.fims.utils.EmailUtils;
import com.fasterxml.jackson.annotation.JsonView;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;

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
    public UsersResource(UserService userService, MessageSource messageSource, SettingsManager settingsManager) {
        super(settingsManager);
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
                !userService.isAProjectAdmin(userContext.getUser(), appRoot)) {
            throw new ForbiddenRequestException("You must be a project admin to access another user's profile");
        }

        return userService.getUser(username);
    }

    /**
     * create a new user, and send an email to the new user with login information
     *
     * @param user
     * @responseMessage 400 invalid user object `biocode.fims.utils.ErrorInfo
     */
    @JsonView(Views.Detailed.class)
    @UserEntityGraph("User.withProjects")
    @POST
    @Admin
    @Authenticated
    public User createUser(User user) {

        if (!user.isValid(true)) {
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
                new String[]{userContext.getUser().getEmail()},
                messageSource.getMessage(
                        "NewUserEmail__SUBJECT",
                        null,
                        Locale.US),
                messageSource.getMessage(
                        "NewUserEmail__BODY",
                        new Object[]{user.getFirstName(), user.getLastName(), appRoot, user.getUsername(), password},
                        Locale.US)
        );

        return user;
    }

    /**
     * update a users profile
     *
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
                !userService.isAProjectAdmin(userContext.getUser(), appRoot)) {
            throw new ForbiddenRequestException("You must be a project admin to access another user's profile");
        }

        User existingUser = userService.getUser(username);
        if (existingUser == null) {
            throw new FimsRuntimeException("user not found", 404);
        }

        if (!user.isValid(false)) {
            throw new BadRequestException("firstName, lastName, email, and institution are required");
        }

        existingUser.update(user);
        return userService.update(existingUser);
    }

    /**
     * update a users password
     *
     * @param username
     * @param currentPassword
     * @param newPassword
     * @responseMessage 403 You cannot update another users password. `biocode.fims.utils.ErrorInfo
     * @responseMessage 400 Invalid current password. `biocode.fims.utils.ErrorInfo
     */
    @JsonView(Views.Detailed.class)
    @Path("/{username}/password")
    @PUT
    @Authenticated
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public User updateUserPassword(@PathParam("username") String username,
                                   @FormParam("currentPassword") String currentPassword,
                                   @FormParam("newPassword") String newPassword) {
        User user = userContext.getUser();
        if (StringUtils.isBlank(currentPassword) ||
                StringUtils.isBlank(newPassword)) {
            throw new BadRequestException("currentPassword and newPassword must not be blank");
        }
        if (!user.getUsername().equals(username)) {
            throw new ForbiddenRequestException("You cannot update another users password.");
        }

        if (!user.equals(userService.getUser(username, currentPassword))) {
            throw new BadRequestException("Invalid current password.");
        }

        // TODO impose minimum newPassword strength

        // hash the users password
        user.setPassword(PasswordHash.createHash(newPassword));

        return userService.update(user);
    }
}
