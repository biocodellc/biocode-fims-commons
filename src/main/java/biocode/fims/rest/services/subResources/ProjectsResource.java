package biocode.fims.rest.services.subResources;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.fimsExceptions.errorCodes.ConfigCode;
import biocode.fims.models.Network;
import biocode.fims.models.Project;
import biocode.fims.models.ProjectConfiguration;
import biocode.fims.models.User;
import biocode.fims.rest.Compress;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.NetworkId;
import biocode.fims.rest.UserEntityGraph;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.rest.responses.ConfirmationResponse;
import biocode.fims.rest.responses.InvalidConfigurationResponse;
import biocode.fims.serializers.Views;
import biocode.fims.service.NetworkService;
import biocode.fims.service.ProjectConfigurationService;
import biocode.fims.service.ProjectService;
import biocode.fims.utils.Flag;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author RJ Ewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class ProjectsResource extends FimsController {
    private final ProjectService projectService;
    private final ProjectConfigurationService projectConfigurationService;
    private final NetworkService networkService;

    @Context
    private NetworkId networkId;

    @Autowired
    public ProjectsResource(ProjectService projectService, ProjectConfigurationService projectConfigurationService,
                            NetworkService networkService, FimsProperties props) {
        super(props);
        this.projectService = projectService;
        this.projectConfigurationService = projectConfigurationService;
        this.networkService = networkService;
    }

    @GET
    @Path("exists/{projectTitle}")
    public boolean projectExists(@PathParam("projectTitle") String projectTitle) {
        return projectService.findByProjectTitle(projectTitle) != null;
    }

    /**
     * Fetch all projects available to the current user
     *
     * @param includePublic If we should include public projects
     * @param projectTitle  A filter on the projectTitle field.
     * @param admin         Flag used to request projects the authenticated user is an admin for. Note: this flag
     *                      takes precedence over all other query params
     */
    @JsonView(Views.Detailed.class)
    @UserEntityGraph("User.withProjectsAndProjectsMemberOf")
    @GET
    public List<Project> getProjects(@QueryParam("includePublic") @DefaultValue("true") Boolean includePublic,
                                     @QueryParam("projectTitle") String projectTitle,
                                     @QueryParam("admin") @DefaultValue("false") Flag admin) {
        if (admin.isPresent()) {
            return new ArrayList<>(userContext.getUser().getProjects());
        }

        List<Project> projects = projectService.getProjects(userContext.getUser(), includePublic);

        return projectTitle == null
                ? projects
                : projects.stream().filter(p -> projectTitle.equals(p.getProjectTitle())).collect(Collectors.toList());
    }

    /**
     * Create a new project
     *
     * @param project
     * @return
     */
    @Compress
    @JsonView(Views.DetailedConfig.class)
    @Authenticated
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(NewProject project) {
        User user = userContext.getUser();
        project.setUser(user);

        if (project.getProjectConfiguration() == null && project.projectConfig == null) {
            throw new BadRequestException("projectConfig or projectConfiguration must be present");
        }

        Network network = networkService.getNetwork(networkId.get());
        if (network == null) {
            throw new BadRequestException("Invalid network");
        }
        project.setNetwork(network);

        boolean createdConfig = false;
        ProjectConfiguration configuration;
        if (project.projectConfig != null) {

            if (!user.isSubscribed() && !network.getUser().equals(user)) {
                throw new BadRequestException("only subscribed users can create a new project configuration");
            }

            configuration = new ProjectConfiguration(project.getProjectTitle(), project.projectConfig, network);
            configuration.setUser(user);
            try {
                configuration = projectConfigurationService.create(configuration);
                createdConfig = true;
            } catch (FimsRuntimeException e) {
                if (e.getErrorCode().equals(ConfigCode.INVALID)) {
                    return Response.status(Response.Status.BAD_REQUEST).entity(new InvalidConfigurationResponse(configuration.getProjectConfig().errors())).build();
                } else {
                    throw e;
                }

            }
        } else {
            configuration = projectConfigurationService.getProjectConfiguration(project.getProjectConfiguration().getId());
            if (configuration == null) {
                throw new BadRequestException("invalid projectConfiguration id");
            }
        }

        project.setProjectConfiguration(configuration);

        try {
            return Response.ok(projectService.create(project.toProject())).build();
        } catch (Exception e) {
            if (createdConfig) {
                projectConfigurationService.deleteIfNoProjects(configuration.getId());
            }
            throw e;
        }
    }


    /**
     * Update a {@link Project}
     *
     * @param project   The updated project object
     * @param projectId The id of the project to update
     * @responseType biocode.fims.models.Project
     * @responseMessage 403 not the project's admin `biocode.fims.utils.ErrorInfo
     */
    @JsonView(Views.Detailed.class)
    @PUT
    @Authenticated
    @Path("/{projectId}/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateProject(@PathParam("projectId") Integer projectId,
                                  Project project) {
        Project existingProject = projectService.getProject(projectId);

        if (existingProject == null) {
            throw new FimsRuntimeException("project not found", 404);
        }

        if (!existingProject.getUser().equals(userContext.getUser())) {
            throw new ForbiddenRequestException("You must be this project's admin in order to update the metadata");
        }

        updateExistingProject(existingProject, project);
        projectService.update(existingProject);

        return Response.ok(existingProject).build();

    }

    /**
     * method to transfer the updated {@link Project} object to an existing {@link Project}. This
     * allows us to control which properties can be updated.
     * Currently allows updating of the following properties : description, projectTitle, isPublic, and isEnforceExpeditionAccess
     *
     * @param existingProject
     * @param updatedProject
     */
    private void updateExistingProject(Project existingProject, Project updatedProject) {
        existingProject.setProjectTitle(updatedProject.getProjectTitle());
        existingProject.setDescription(updatedProject.getDescription());
        existingProject.setPublic(updatedProject.isPublic());
        existingProject.setEnforceExpeditionAccess(updatedProject.isEnforceExpeditionAccess());
    }

    /**
     * delete a project
     *
     * @param projectId The id of the project to delete
     * @responseMessage 403 not the project's admin `biocode.fims.utils.ErrorInfo
     */
    @DELETE
    @Authenticated
    @Path("/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ConfirmationResponse delete(@PathParam("projectId") Integer projectId) {
        Project project = projectService.getProject(projectId);

        if (project == null) {
            throw new FimsRuntimeException("project not found", 404);
        }

        if (!project.getUser().equals(userContext.getUser())) {
            throw new ForbiddenRequestException("You must be this project's admin in order to update the metadata");
        }
        projectService.delete(project);

        return new ConfirmationResponse(true);
    }

    private static class NewProject extends Project {
        public ProjectConfig projectConfig;

        public NewProject() {
            super();
        }

        Project toProject() {
            Project p = new Project.ProjectBuilder(
                    this.getDescription(),
                    this.getProjectTitle(),
                    this.getProjectConfiguration())
                    .isPublic(this.isPublic())
                    .enforceExpeditionAccess(this.isEnforceExpeditionAccess())
                    .build();
            p.setUser(this.getUser());
            p.setProjectMembers(this.getProjectMembers());
            p.setNetwork(this.getNetwork());

            return p;
        }
    }
}
