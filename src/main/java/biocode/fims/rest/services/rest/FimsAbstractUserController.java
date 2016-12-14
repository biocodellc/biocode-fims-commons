package biocode.fims.rest.services.rest;

import biocode.fims.auth.PasswordHash;
import biocode.fims.bcid.ProjectMinter;
import biocode.fims.entities.User;
import biocode.fims.fimsExceptions.*;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.filters.Admin;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.rest.services.rest.resources.UserProjectResource;
import biocode.fims.service.UserService;
import biocode.fims.settings.SettingsManager;
import org.glassfish.jersey.server.model.Resource;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The REST Interface for dealing with users. Includes user creation and profile updating.
 */
public abstract class FimsAbstractUserController extends FimsService {

    protected final UserService userService;
    @Context
    ResourceContext resourceContext;

    FimsAbstractUserController(UserService userService, SettingsManager settingsManager) {
        super(settingsManager);
        this.userService = userService;
    }

    @Path("{userId}/projects")
    public UserProjectResource getUserProjectResource(@PathParam("userId") Integer userId) {
//        return Resource.from(UserProjectResource.class);
        return resourceContext.getResource(UserProjectResource.class);
    }

    /**
     * gets the UserProjectResource, passing in the userId from the current logged in user
     * this is to maintain backwards compatibility with api v1
     * @return
     */
    @Deprecated
    @Path("projects")
    public Resource getUserProjectResourceDeprecated() {
        return Resource.from(UserProjectResource.class);
    }

    /**
     * Service to create a new user.
     *
     * @param createUser
     * @param password
     * @param firstName
     * @param lastName
     * @param email
     * @param institution
     * @param projectId
     * @return
     */
    @POST
    @Authenticated
    @Admin
    @Path("/admin/create")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createUser(@FormParam("username") String createUser,
                               @FormParam("password") String password,
                               @FormParam("firstName") String firstName,
                               @FormParam("lastName") String lastName,
                               @FormParam("email") String email,
                               @FormParam("institution") String institution,
                               @FormParam("projectId") Integer projectId) {

        if (StringUtils.isEmpty(createUser) ||
                StringUtils.isEmpty(password) ||
                StringUtils.isEmpty(firstName) ||
                StringUtils.isEmpty(lastName) ||
                StringUtils.isEmpty(email) ||
                StringUtils.isEmpty(institution))
            throw new BadRequestException("all fields are required");

        // check that a valid email is given
        if (!email.toUpperCase().matches("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}")) {
            throw new BadRequestException("please enter a valid email");
        }

        if (userService.getUser(createUser) != null)
            throw new BadRequestException("username already exists");

        ProjectMinter p = new ProjectMinter();
        // check if the user is this project's admin
        if (!p.isProjectAdmin(userContext.getUser().getUsername(), projectId)) {
            throw new ForbiddenRequestException("You can't add a user to a project that you're not an admin.");
        }

        User newUser = new User.UserBuilder(createUser, PasswordHash.createHash(password))
                .name(firstName, lastName)
                .email(email)
                .institution(institution)
                .build();

        userService.create(newUser, projectId);

        return Response.ok("{\"success\": \"successfully created new user\"}").build();
    }

    /**
     * Service for a user to update their profile.
     *
     * @param firstName
     * @param lastName
     * @param email
     * @param institution
     * @param oldPassword
     * @param newPassword
     * @param returnTo
     * @returns either error message or the url to redirect to upon success
     */
    @POST
    @Authenticated
    @Path("/profile/update")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProfile(@FormParam("firstName") String firstName,
                                  @FormParam("lastName") String lastName,
                                  @FormParam("email") String email,
                                  @FormParam("institution") String institution,
                                  @FormParam("oldPassword") String oldPassword,
                                  @FormParam("newPassword") String newPassword,
                                  @FormParam("username") String updateUsername,
                                  @QueryParam("return_to") String returnTo) {
        Boolean adminAccess = false;
        User userToUpdate = this.userContext.getUser();

        if (!userContext.getUser().getUsername().equals(updateUsername.trim())) {
            if (!userService.isAProjectAdmin(userContext.getUser())) {
                throw new ForbiddenRequestException("You must be a project admin to update someone else's profile.");
            } else {
                adminAccess = true;
                userToUpdate = userService.getUser(updateUsername);
            }
        }
        if (userToUpdate == null)
            throw new BadRequestException("user: " + updateUsername + "not found");

        // Only update user's password if both old_password and new_password fields contain values and the user is updating
        // their own profile
        if (!newPassword.isEmpty()) {
            if (adminAccess) {
                userToUpdate.setPassword(PasswordHash.createHash(newPassword));
                // Make the user change their password next time they login
                userContext.getUser().setHasSetPassword(false);
            } else if (!oldPassword.isEmpty()) {
                // If user's old_password matches stored pass, then update the user's password to the new value
                if (userService.getUser(updateUsername, oldPassword) == null)
                    throw new BadRequestException("Wrong Password");

                userToUpdate.setPassword(PasswordHash.createHash(newPassword));
                // Make sure that the getHasSetPassword field is 1 (true) so they aren't asked to change their password after login
                userToUpdate.setHasSetPassword(true);
            }
        }

        userToUpdate.setFirstName(firstName);
        userToUpdate.setLastName(lastName);

        // check that a valid email is given
        if (email.toUpperCase().matches("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}")) {
            userToUpdate.setEmail(email);
        } else {
            throw new BadRequestException("please enter a valid email");
        }

        userToUpdate.setInstitution(institution);
        userService.update(userToUpdate);

        JSONObject response = new JSONObject();
        response.put("adminAccess", adminAccess);
        if (returnTo != null) {
            response.put("returnTo", returnTo);
        }
        return Response.ok(response.toJSONString()).build();
    }

    /**
     * Service for oAuth client apps to retrieve a user's profile information.
     *
     * @return
     */
    @GET
    @Path("/profile")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserData() {
        if (userContext.getUser() != null) {
            userContext.getUser().setAdmin(userService.isAProjectAdmin(userContext.getUser()));
            return Response.ok(userContext.getUser()).build();
        }
        throw new BadRequestException("invalid_grant", "access_token was null");
    }

    /**
     * Service for a user to exchange their reset resetToken in order to update their password
     *
     * @param password
     * @param resetToken
     */
    @POST
    @Path("/resetPassword")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetPassword(@FormParam("password") String password,
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

        return Response.ok("{\"success\":\"Successfully updated your password\"}").build();
    }

}
