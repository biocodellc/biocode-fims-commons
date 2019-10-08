package biocode.fims.rest.services;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.auth.PasswordHash;
import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.fimsExceptions.*;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.rest.FimsObjectMapper;
import biocode.fims.rest.responses.AcknowledgedResponse;
import biocode.fims.rest.responses.ConfirmationResponse;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.UserEntityGraph;
import biocode.fims.rest.filters.Admin;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.rest.responses.DynamicViewResponse;
import biocode.fims.serializers.Views;
import biocode.fims.service.ProjectService;
import biocode.fims.service.UserService;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;

/**
 * User API endpoints.
 *
 * @exclude
 */
@Produces(MediaType.APPLICATION_JSON)
public abstract class BaseUserController extends FimsController {

    protected final UserService userService;
    private final ProjectService projectService;

    BaseUserController(UserService userService, ProjectService projectService, FimsProperties props) {
        super(props);
        this.userService = userService;
        this.projectService = projectService;
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
     * @responseType biocode.fims.models.User
     */
    @UserEntityGraph("User.withProjects")
    @JsonView(Views.Detailed.class)
    @Path("/{username}")
    @GET
    public DynamicViewResponse<User> getUser(@PathParam("username") String username, @Context ObjectMapper objectMapper) {
        User user = userService.getUser(username);

        User authenticatedUser = userContext.getUser();
        if (authenticatedUser != null && (
                authenticatedUser.getUsername().equals(username) || userService.isAProjectAdmin(authenticatedUser))) {
            return new DynamicViewResponse<>(user, Views.Detailed.class);
        }
        return new DynamicViewResponse<>(user, Views.Public.class);
    }

    /**
     * get user
     *
     * @responseType biocode.fims.models.User
     */
    @UserEntityGraph("User.withProjects")
    @JsonView(Views.Detailed.class)
    @Path("/{userId: [0-9]+}")
    @GET
    public DynamicViewResponse<User> getUserById(@PathParam("userId") int userId, @Context ObjectMapper objectMapper) {
        User user = userService.getUser(userId);

        User authenticatedUser = userContext.getUser();
        if (authenticatedUser != null && (
                authenticatedUser.getUserId() == userId || userService.isAProjectAdmin(authenticatedUser))) {
            return new DynamicViewResponse<>(user, Views.Detailed.class);
        }
        return new DynamicViewResponse<>(user, Views.Public.class);
    }

    /**
     * create a new user
     *
     * @param user
     * @param inviteId (optional) A valid invite token
     * @responseMessage 400 invalid user object `biocode.fims.utils.ErrorInfo
     * @responseMessage 400 invalid invite code `biocode.fims.utils.ErrorInfo
     * @responseMessage 400 duplicate username `biocode.fims.utils.ErrorInfo
     */
    @JsonView(Views.Detailed.class)
    @POST
    public User createUser(@QueryParam("inviteId") UUID inviteId,
                           User user) {
        return userService.create(user, user.getPassword(), inviteId);
    }

    /**
     * update a users profile
     *
     * @param username
     * @param updatedUser
     * @responseMessage 403 You cannot update another user's profile. `biocode.fims.utils.ErrorInfo
     */
    @JsonView(Views.Detailed.class)
    @UserEntityGraph("User.withProjects")
    @Path("/{username}")
    @PUT
    @Authenticated
    public User updateUser(@PathParam("username") String username,
                           User updatedUser) {
        User user = userContext.getUser();
        if (!user.getUsername().equals(username)) {
            throw new ForbiddenRequestException("You cannot update another user's profile.");
        }

        if (!user.isValid(false)) {
            throw new BadRequestException("firstName, lastName, email are required");
        }

        user.update(updatedUser);
        return userService.update(user);
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
        if (org.apache.commons.lang3.StringUtils.isBlank(currentPassword) ||
                org.apache.commons.lang3.StringUtils.isBlank(newPassword)) {
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

    /**
     * Send a user invite via email notification
     *
     * @param projectId
     * @param email
     * @return
     */
    @Authenticated
    @POST
    @Path("/invite")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ConfirmationResponse inviteUser(@FormParam("projectId") int projectId,
                                           @FormParam("email") String email) {
        if (email == null) {
            throw new BadRequestException("Email must not be null");
        }

        Project project = projectService.getProject(projectId);

        if (project == null || !project.getUser().equals(userContext.getUser())) {
            throw new BadRequestException("Only admins can invite users to their project.");
        }

        // check that a valid email is given
        if (!email.toUpperCase().matches("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}")) {
            throw new BadRequestException("Please enter a valid email.");
        }

        if (userService.userExists(email)) {
            throw new BadRequestException("User with that email already exists.");
        }

        userService.inviteUser(email, project, userContext.getUser());

        return new ConfirmationResponse(true);
    }

    /**
     * Request a password reset token. Will send an email to the user's registered email
     * if the user is found
     *
     * @param username
     */
    @POST
    @Path("{username}/sendResetToken")
    @Produces(MediaType.APPLICATION_JSON)
    public AcknowledgedResponse sendResetToken(@PathParam("username") String username) {
        if (username.isEmpty()) {
            throw new BadRequestException("User not found.", "username is null");
        }

        userService.sendResetEmail(username);
        return new AcknowledgedResponse(true);
    }

    /**
     * Service for a user to exchange their reset resetToken in order to update their password
     *
     * @param password
     * @param resetToken
     */
    @POST
    @Path("/reset")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ConfirmationResponse resetPassword(@FormParam("password") String password,
                                              @FormParam("resetToken") String resetToken) {
        if (password.isEmpty()) {
            throw new BadRequestException("Password must not be empty");
        }

        User user = userService.getUserByResetToken(resetToken);

        if (user == null)
            throw new BadRequestException("Expired/Invalid Reset Token");

        user.setPassword(PasswordHash.createHash(password));
        user.setPasswordResetExpiration(null);
        user.setPasswordResetToken(null);
        userService.update(user);


        return new ConfirmationResponse(true);
    }

}
