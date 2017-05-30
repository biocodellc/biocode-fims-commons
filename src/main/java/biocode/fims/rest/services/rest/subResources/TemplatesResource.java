package biocode.fims.rest.services.rest.subResources;

import biocode.fims.authorizers.ProjectAuthorizer;
import biocode.fims.bcid.ProjectMinter;
import biocode.fims.models.Project;
import biocode.fims.models.ProjectTemplate;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.run.TemplateProcessor;
import biocode.fims.serializers.Views;
import biocode.fims.service.ProjectService;
import biocode.fims.settings.SettingsManager;
import biocode.fims.tools.CachedFile;
import biocode.fims.tools.FileCache;
import biocode.fims.utils.StringGenerator;
import com.fasterxml.jackson.annotation.JsonView;
import org.glassfish.jersey.server.model.Resource;
import org.json.simple.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * @author RJ Ewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
public class TemplatesResource extends FimsService {

    private final ProjectService projectService;
    private final ProjectAuthorizer projectAuthorizer;
    private final FileCache fileCache;

    @Autowired
    public TemplatesResource(ProjectService projectService, FileCache fileCache, SettingsManager settingsManager) {
        super(settingsManager);
        this.projectService = projectService;
        this.projectAuthorizer = new ProjectAuthorizer(projectService, appRoot);
        this.fileCache = fileCache;
    }

    @JsonView(Views.Detailed.class)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<ProjectTemplate> getTemplates(@PathParam("projectId") Integer projectId) {

        Project project = projectService.getProjectWithTemplates(projectId, appRoot);

        if (!projectAuthorizer.userHasAccess(userContext.getUser(), project)) {
            throw new BadRequestException("you do not have the necessary permissions to access this project");
        }

        return project.getTemplates();
    }

    @POST
    @Path("/generate/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createExcel(
            @FormParam("columns") List<String> columns,
            @FormParam("sheetName") String sheetName,
            @PathParam("projectId") Integer projectId) {

        Project project = projectService.getProjectWithTemplates(projectId, appRoot);

        if (!projectAuthorizer.userHasAccess(userContext.getUser(), project)) {
            throw new BadRequestException("you do not have the necessary permissions to access this project");
        }

        // Create the template processor which handles all functions related to the template, reading, generation
        TemplateProcessor t = new TemplateProcessor(projectId, defaultOutputDirectory());

        File file = t.createExcelFile(sheetName, defaultOutputDirectory(), columns);

        // Catch a null file and return 204
        if (file == null) {
            return Response.noContent().build();
        }

        int userId = userContext.getUser() != null ? userContext.getUser().getUserId() : 0;
        String fileId = StringGenerator.generateString(20);
        CachedFile cf = new CachedFile(fileId, file.getAbsolutePath(), userId, file.getName());
        fileCache.addFile(cf);

        URI fileURI = uriInfo.getBaseUriBuilder().path("utils/file").queryParam("id", fileId).build();

        return Response.ok("{\"url\": \"" + fileURI + "\"}").build();
    }

//    /**
//     * Service used to save a fims template generator configuration
//     *
//     * @param checkedOptions
//     * @param configName
//     * @param projectId
//     * @return
//     */
//    @PUT
//    @Authenticated
//    @Produces(MediaType.APPLICATION_JSON)
//    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//    public ProjectTemplate save(@FormParam("columns") List<String> checkedOptions,
//                                @FormParam("sheetName") String sheetName,
//                                @FormParam("configName") String configName,
//                                @PathParam("projectId") Integer projectId) {
//
//        if (configName.equalsIgnoreCase("default")) {
//            throw new BadRequestException("You can not change the default template configuration.");
//        }
//
//        Project project = projectService.getProjectWithTemplates(projectId, appRoot);
//
//        if (!projectAuthorizer.userHasAccess(userContext.getUser(), project)) {
//            throw new BadRequestException("you do not have the necessary permissions to access this project");
//        }
//
////        ProjectTemplate projectTemplate = new ProjectTemplate(configName)
//
//        ProjectMinter projectMinter = new ProjectMinter();
//
//        if (projectMinter.templateConfigExists(configName, projectId)) {
//            if (projectMinter.usersTemplateConfig(configName, projectId, userContext.getUser().getUserId())) {
//                projectMinter.updateTemplateConfig(configName, projectId, userContext.getUser().getUserId(), checkedOptions);
//            } else {
//                return Response.ok("{\"error\": \"A configuration with that name already exists, and you are not the owner.\"}").build();
//            }
//        } else {
//            projectMinter.saveTemplateConfig(configName, projectId, userContext.getUser().getUserId(), checkedOptions);
//        }
//
//        return Response.ok("{\"success\": \"Successfully saved template configuration.\"}").build();
//    }
}
