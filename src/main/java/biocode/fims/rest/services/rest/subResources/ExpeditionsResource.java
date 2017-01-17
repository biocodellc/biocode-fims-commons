package biocode.fims.rest.services.rest.subResources;

import biocode.fims.entities.Expedition;
import biocode.fims.entities.Project;
import biocode.fims.fimsExceptions.*;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.UserEntityGraph;
import biocode.fims.rest.filters.Admin;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.serializers.Views;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.settings.SettingsManager;
import biocode.fims.utils.Flag;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author RJ Ewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
public class ExpeditionsResource extends FimsService {
    private final ExpeditionService expeditionService;
    private final ProjectService projectService;

    @Autowired
    public ExpeditionsResource(ExpeditionService expeditionService, ProjectService projectService,
                               SettingsManager settingsManager) {
        super(settingsManager);
        this.expeditionService = expeditionService;
        this.projectService = projectService;
    }

    /**
     * Get a list of a Projects Expeditions
     *
     * @param projectId      The project to get expeditions for
     * @param admin          If present, all expeditions will be returned, regardless of the expedition owner. Note: this flag
     *                       takes precedence over all other query params
     * @param user           If present, only the projects for the authenticated user will be returned
     * @param includePrivate Include the authenticated users private expeditions in the results
     * @responseMessage 403 Invalid request. Either using admin flag and user is not the project admin, or requesting
     * expeditions for a private project that the user is not a member of. `biocode.fims.utils.ErrorInfo
     */
    @JsonView(Views.Detailed.class)
    @UserEntityGraph("User.withProjectsMemberOf")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Expedition> listExpeditions(@PathParam("projectId") Integer projectId,
                                            @QueryParam("admin") @DefaultValue("false") Flag admin,
                                            @QueryParam("user") @DefaultValue("false") Flag user,
                                            @QueryParam("includePrivate") @DefaultValue("false") boolean includePrivate) {
        Project project = projectService.getProjectWithExpeditions(projectId);

        if (admin.isPresent()) {

            if (!projectService.isProjectAdmin(userContext.getUser(), projectId)) {
                throw new ForbiddenRequestException("You are not an admin for this project");
            }

            return project.getExpeditions();
        }

        if (!project.isPublic() && !projectService.isUserMemberOfProject(userContext.getUser(), project.getProjectId())) {
            throw new ForbiddenRequestException("You are not a member of this private project");
        }

        if (user.isPresent()) {
            return getUsersExpeditions(project.getExpeditions(), includePrivate);
        } else {
            return project.getExpeditions()
                    .stream()
                    .filter(e -> e.isPublic() || (includePrivate && e.getUser().equals(userContext.getUser())))
                    .collect(Collectors.toList());
        }
    }

    private List<Expedition> getUsersExpeditions(List<Expedition> expeditions, boolean includePrivate) {
        return expeditions
                .stream()
                .filter(e -> e.getUser().equals(userContext.getUser()) && (e.isPublic() || includePrivate))
                .collect(Collectors.toList());
    }

    /**
     * Bulk update expeditions
     * <p>
     * Project Admin access only
     *
     * @param projectId   The projectId of the expeditions to be updated
     * @param expeditions The Expeditions to be updated. Note: The expeditions must already belong to the projectId
     * @responseMessage 403 not the project's admin `biocode.fims.utils.ErrorInfo
     * @responseMessage 400 A given expedition does not belong to the project `biocode.fims.utils.ErrorInfo
     */
    @PUT
    @Authenticated
    @Admin
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Expedition> updateExpeditions(@PathParam("projectId") Integer projectId,
                                              List<Expedition> expeditions) {
        if (!projectService.isProjectAdmin(userContext.getUser(), projectId)) {
            throw new ForbiddenRequestException("You are not an admin for this project");
        }

        expeditionService.update(expeditions, projectId);

        return expeditions;
    }

}
