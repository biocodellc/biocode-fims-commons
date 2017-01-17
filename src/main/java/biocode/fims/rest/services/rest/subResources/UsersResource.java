package biocode.fims.rest.services.rest.subResources;

import biocode.fims.auth.PasswordHash;
import biocode.fims.bcid.ProjectMinter;
import biocode.fims.entities.User;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.filters.Admin;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.serializers.Views;
import biocode.fims.service.ProjectService;
import biocode.fims.service.UserService;
import biocode.fims.settings.SettingsManager;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author RJ Ewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
public class UsersResource extends FimsService {
    private final UserService userService;

    @Autowired
    public UsersResource(UserService userService, SettingsManager settingsManager) {
        super(settingsManager);
        this.userService = userService;
    }

    /**
     * get all registered users
     *
     * @responseMessage 403 you are not a project admin `biocode.fims.utils.ErrorInfo
     */
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
    @JsonView(Views.Detailed.class)
    @Path("/{username}")
    @GET
    @Authenticated
    public User getUser(@PathParam("username") String username) {
        if (!userContext.getUser().getUsername().equals(username) &&
                !userService.isAProjectAdmin(userContext.getUser())) {
            throw new ForbiddenRequestException("You must be a project admin to access another user's profile");
        }

        return userService.getUser(username);
    }

    @JsonView(Views.Detailed.class)
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

        // hash the users password
        user.setPassword(PasswordHash.createHash(user.getPassword()));

        return userService.create(user);
    }

    /**
     * update a users profile
     * @param username
     * @param user
     * @responseMessage 403 you must be authenticated to update a users profile `biocode.fims.utils.ErrorInfo
     * @responseMessage 403 you must be a project admin to update another user's profile `biocode.fims.utils.ErrorInfo
     */
    @JsonView(Views.Detailed.class)
    @Path("/{username}")
    @PUT
    @Authenticated
    public User updateUser(@PathParam("username") String username,
                           User user) {
        if (!userContext.getUser().getUsername().equals(username) &&
                !userService.isAProjectAdmin(userContext.getUser())) {
            throw new ForbiddenRequestException("You must be a project admin to access another user's profile");
        }

        user.setUsername(username);

        if (!isValidUser(user, false)) {
            throw new BadRequestException("firstName, lastName, email, and institution are required");
        }

        return userService.update(user);
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
