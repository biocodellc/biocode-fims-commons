package biocode.fims.rest.services.rest.subResources;

import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.Mapping;
import biocode.fims.models.Expedition;
import biocode.fims.models.Project;
import biocode.fims.fileManagers.fimsMetadata.FimsMetadataFileManager;
import biocode.fims.fimsExceptions.*;
import biocode.fims.rest.AcknowledgedResponse;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.UserEntityGraph;
import biocode.fims.rest.filters.Admin;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.run.ProcessController;
import biocode.fims.serializers.Views;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.settings.SettingsManager;
import biocode.fims.utils.Flag;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author RJ Ewing
 */
@Scope("prototype")
@Controller
@Produces(MediaType.APPLICATION_JSON)
public class ExpeditionsResource extends FimsService {
    private final ExpeditionService expeditionService;
    private final ProjectService projectService;
    private final FimsMetadataFileManager fimsMetadataFileManager;

    @Autowired
    public ExpeditionsResource(ExpeditionService expeditionService, ProjectService projectService,
                               SettingsManager settingsManager, FimsMetadataFileManager fimsMetadataFileManager) {
        super(settingsManager);
        this.expeditionService = expeditionService;
        this.projectService = projectService;
        this.fimsMetadataFileManager = fimsMetadataFileManager;
    }

    /**
     * Get a list of a Projects Expeditions
     *
     * @param projectId      The project to get expeditions for
     * @param admin          If present, all expeditions will be returned, regardless of the expedition owner. Note: this flag
     *                       takes precedence over all other query params
     * @param user           If present, only the expeditions for the authenticated user will be returned
     * @param includePrivate Include the authenticated users private expeditions in the results
     * @responseMessage 403 Invalid request. Either using admin flag and user is not the project admin, or requesting
     * expeditions for a private project that the user is not a member of. `biocode.fims.utils.ErrorInfo
     */
    @JsonView(Views.Detailed.class)
    @UserEntityGraph("User.withProjectsAndProjectsMemberOf")
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
    @UserEntityGraph("User.withProjects")
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

    @UserEntityGraph("User.withProjectsMemberOf")
    @JsonView(Views.Detailed.class)
    @GET
    @Path("/{expeditionCode}")
    @Produces(MediaType.APPLICATION_JSON)
    public Expedition getExpedition(@PathParam("projectId") Integer projectId,
                                    @PathParam("expeditionCode") String expeditionCode) {
        Expedition expedition = expeditionService.getExpedition(expeditionCode, projectId);

        if (expedition == null) {
            return expedition;
        }

        if (!expedition.getProject().isPublic() && !projectService.isUserMemberOfProject(userContext.getUser(), expedition.getProject().getProjectId())) {
            throw new ForbiddenRequestException("You are not a member of this private project");
        }

        return expedition;
    }

    /**
     * update Expedition
     *
     * @param projectId      The projectId the expedition belongs to
     * @param expeditionCode The expeditionCode of the expedition to update
     * @param expedition     The Expedition to be updated. Note: The expedition must already belong to the projectId
     * @responseMessage 403 not the expedition owner or the project's admin `biocode.fims.utils.ErrorInfo
     * @responseMessage 400 A given expedition does not belong to the project `biocode.fims.utils.ErrorInfo
     */
    @UserEntityGraph("User.withProjects")
    @PUT
    @Authenticated
    @Path("/{expeditionCode}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Expedition updateExpeditions(@PathParam("projectId") Integer projectId,
                                        @PathParam("expeditionCode") String expeditionCode,
                                        Expedition expedition) {
        Expedition existingExpedition = expeditionService.getExpedition(expeditionCode, projectId);

        if (existingExpedition == null || !existingExpedition.getProject().getProjectUrl().equals(appRoot)) {
            throw new FimsRuntimeException("project not found", 404);
        }

        if (!existingExpedition.getUser().equals(userContext.getUser()) && !projectService.isProjectAdmin(userContext.getUser(), projectId)) {
            throw new ForbiddenRequestException("You do not own this expedition, or you are not an admin for this project");
        }


        updateExistingExpedition(existingExpedition, expedition);
        expeditionService.update(existingExpedition);

        return existingExpedition;

    }

    /**
     * method to transfer the updated {@link Expedition} object to an existing {@link Expedition}. This
     * allows us to control which properties can be updated.
     * Currently allows updating of the following properties : expeditionTitle and isPublic
     *
     * @param existingExpedition
     * @param updatedExpedition
     */
    private void updateExistingExpedition(Expedition existingExpedition, Expedition updatedExpedition) {
        existingExpedition.setExpeditionTitle(updatedExpedition.getExpeditionTitle());
        existingExpedition.setPublic(updatedExpedition.isPublic());
    }

    /**
     * delete an expedition
     * <p>
     * Project Admin access only
     *
     * @param projectId      The projectId the expedition belongs to
     * @param expeditionCode The expeditionCode of the expedition to delete
     * @responseMessage 403 not the project's admin `biocode.fims.utils.ErrorInfo
     */
    @UserEntityGraph("User.withProjects")
    @DELETE
    @Authenticated
    @Admin
    @Path("/{expeditionCode}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public AcknowledgedResponse deleteExpedition(@PathParam("projectId") Integer projectId,
                                                 @PathParam("expeditionCode") String expeditionCode) {
        if (!projectService.isProjectAdmin(userContext.getUser(), projectId)) {
            throw new ForbiddenRequestException("You are not an admin for this project");
        }

        expeditionService.delete(expeditionCode, projectId);

        // delete any data for the expedition
        File configFile = new ConfigurationFileFetcher(projectId, defaultOutputDirectory(), true).getOutputFile();

        Mapping mapping = new Mapping();
        mapping.addMappingRules(configFile);

        ProcessController processController = new ProcessController(projectId, expeditionCode);
        processController.setOutputFolder(defaultOutputDirectory());
        processController.setMapping(mapping);
        fimsMetadataFileManager.setProcessController(processController);
        fimsMetadataFileManager.deleteDataset();

        return new AcknowledgedResponse(true);
    }

}
