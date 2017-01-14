package biocode.fims.rest.services.rest;

import biocode.fims.bcid.ProjectMinter;
import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.entities.Project;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.filters.Admin;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.rest.services.rest.subResources.ExpeditionsResource;
import biocode.fims.rest.services.rest.subResources.ProjectConfigurationResource;
import biocode.fims.rest.services.rest.subResources.ProjectResource;
import biocode.fims.run.TemplateProcessor;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.settings.SettingsManager;
import org.glassfish.jersey.server.model.Resource;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 * REST interface calls for working with projects.  This includes fetching details associated with projects.
 * Currently, there are no REST services for creating projects, which instead must be added to the Database
 * manually by an administrator
 * @exclude
 */
public abstract class FimsAbstractProjectsController extends FimsService {
    private static final Logger logger = LoggerFactory.getLogger(FimsAbstractProjectsController.class);
    protected ExpeditionService expeditionService;
    protected ProjectService projectService;

    FimsAbstractProjectsController(ExpeditionService expeditionService, SettingsManager settingsManager,
                                   ProjectService projectService) {
        super(settingsManager);
        this.expeditionService = expeditionService;
        this.projectService = projectService;
    }


    /**
     *
     * @responseType biocode.fims.rest.services.rest.subResources.ProjectResource
     */
    @Path("/")
    public Resource getProjectResource() {
        return Resource.from(ProjectResource.class);
    }

    /**
     * Given a project id, get the latest graphs by expedition
     *
     * This service is no longer supported and will be removed in the future. Use {@link FimsAbstractProjectsController#listExpeditions(Integer)}
     *
     * @param projectId
     * @return
     */
    @Deprecated
    @GET
    @Path("/{projectId}/graphs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLatestGraphsByExpedition(@PathParam("projectId") Integer projectId) {
        ProjectMinter project = new ProjectMinter();
        String username = null;
        if (userContext.getUser() != null) {
            username = userContext.getUser().getUsername();
        }

        JSONArray graphs = project.getLatestGraphs(projectId, username);

        return Response.ok(graphs.toJSONString()).header("Access-Control-Allow-Origin", "*").build();
    }

    @GET
    @Path("/{projectId}/abstract")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAbstract(@PathParam("projectId") int projectId) {
        JSONObject obj = new JSONObject();
        TemplateProcessor t = new TemplateProcessor(projectId, uploadPath());

        // Write the all of the checkbox definitions to a String Variable
        //obj.put("abstract", JSONValue.escape(t.printAbstract()));
        obj.put("abstract", t.printAbstract());

        return Response.ok(obj.toJSONString()).build();
    }

    /**
     * Given an project Bcid, get the users latest datasets by expedition
     *
     * @return
     */
    @GET
    @Authenticated
    @Path("/myGraphs/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMyLatestGraphs() {
        ProjectMinter project = new ProjectMinter();

        String response = project.getMyLatestGraphs(userContext.getUser().getUsername());

        return Response.ok(response).header("Access-Control-Allow-Origin", "*").build();
    }

    /**
     * Get the users datasets
     *
     * @return
     */
    @GET
    @Authenticated
    @Path("/myDatasets/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDatasets() {
        ProjectMinter project = new ProjectMinter();

        String response = project.getMyTemplatesAndDatasets(userContext.getUser().getUsername());

        return Response.ok(response).header("Access-Control-Allow-Origin", "*").build();
    }

    /**
     * Service used to remove a user as a member of a project.
     *
     * @param projectId
     * @param userId
     * @return
     */
    @GET
    @Authenticated
    @Admin
    @Path("/{projectId}/admin/removeUser/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeUser(@PathParam("projectId") Integer projectId,
                               @PathParam("userId") Integer userId) {
        ProjectMinter p = new ProjectMinter();
        if (!p.isProjectAdmin(userContext.getUser().getUsername(), projectId)) {
            throw new ForbiddenRequestException("You are not this project's admin.");
        }

        p.removeUser(userId, projectId);

        return Response.ok("{\"success\": \"User has been successfully removed\"}").build();
    }

    /**
     * Service used to add a user as a member of a project.
     *
     * @param projectId
     * @param userId
     * @return
     */
    @POST
    @Authenticated
    @Admin
    @Path("/{projectId}/admin/addUser")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addUser(@PathParam("projectId") Integer projectId,
                            @FormParam("userId") Integer userId) {

        // userId of 0 means create new user, using ajax to create user, shouldn't ever receive userId of 0
        if (userId == 0) {
            throw new BadRequestException("invalid userId");
        }

        ProjectMinter p = new ProjectMinter();
        if (!p.isProjectAdmin(userContext.getUser().getUsername(), projectId)) {
            throw new ForbiddenRequestException("You are not this project's admin");
        }
        p.addUserToProject(userId, projectId);

        return Response.ok("{\"success\": \"User has been successfully added to this project\"}").build();
    }

    /**
     * Service used to retrieve a JSON representation of the project's a user is a member of.
     *
     * @return
     */
    @GET
    @Authenticated
    @Path("/user/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserProjects() {
        ProjectMinter p = new ProjectMinter();
        JSONArray projects = p.listUsersProjects(userContext.getUser().getUsername());
        return Response.ok(projects.toJSONString()).build();
    }

    /**
     * Service used to save a fims template generator configuration
     *
     * @param checkedOptions
     * @param configName
     * @param projectId
     * @return
     */
    @POST
    @Authenticated
    @Path("/{projectId}/saveTemplateConfig")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response saveTemplateConfig(@FormParam("checkedOptions") List<String> checkedOptions,
                                       @FormParam("configName") String configName,
                                       @PathParam("projectId") Integer projectId) {

        if (configName.equalsIgnoreCase("default")) {
            return Response.ok("{\"error\": \"To change the default config, talk to the project admin.\"}").build();
        }

        ProjectMinter projectMinter = new ProjectMinter();

        if (projectMinter.templateConfigExists(configName, projectId)) {
            if (projectMinter.usersTemplateConfig(configName, projectId, userContext.getUser().getUserId())) {
                projectMinter.updateTemplateConfig(configName, projectId, userContext.getUser().getUserId(), checkedOptions);
            } else {
                return Response.ok("{\"error\": \"A configuration with that name already exists, and you are not the owner.\"}").build();
            }
        } else {
            projectMinter.saveTemplateConfig(configName, projectId, userContext.getUser().getUserId(), checkedOptions);
        }

        return Response.ok("{\"success\": \"Successfully saved template configuration.\"}").build();
    }

    /**
     * Service used to get the fims template generator configurations for a given project
     *
     * @param projectId
     * @return
     */
    @GET
    @Path("/{projectId}/getTemplateConfigs/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTemplateConfigs(@PathParam("projectId") Integer projectId) {
        ProjectMinter p = new ProjectMinter();
        JSONArray configs = p.getTemplateConfigs(projectId);

        return Response.ok(configs.toJSONString()).build();
    }

    /**
     * Service used to get a specific fims template generator configuration
     *
     * @param configName
     * @param projectId
     * @return
     */
    @GET
    @Path("/{projectId}/getTemplateConfig/{configName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfig(@PathParam("configName") String configName,
                              @PathParam("projectId") Integer projectId) {
        ProjectMinter p = new ProjectMinter();
        JSONObject response = p.getTemplateConfig(configName, projectId);

        return Response.ok(response.toJSONString()).build();
    }

    /**
     * Service used to delete a specific fims template generator configuration
     *
     * @param configName
     * @param projectId
     * @return
     */
    @GET
    @Authenticated
    @Path("/{projectId}/removeTemplateConfig/{configName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeConfig(@PathParam("configName") String configName,
                                 @PathParam("projectId") Integer projectId) {
        if (configName.equalsIgnoreCase("default")) {
            return Response.ok("{\"error\": \"To remove the default config, talk to the project admin.\"}").build();
        }

        ProjectMinter p = new ProjectMinter();
        if (p.templateConfigExists(configName, projectId) && p.usersTemplateConfig(configName, projectId, userContext.getUser().getUserId())) {
            p.removeTemplateConfig(configName, projectId);
        } else {
            return Response.ok("{\"error\": \"Only the owners of a configuration can remove the configuration.\"}").build();
        }

        return Response.ok("{\"success\": \"Successfully removed template configuration.\"}").build();
    }

    /**
     *
     * @responseType biocode.fims.rest.services.rest.subResources.ExpeditionsResource
     * @resourceTag Expeditions
     */
    @Path("{projectId}/expeditions")
    public Resource getUserProjectResource() {
        return Resource.from(ExpeditionsResource.class);
    }

    /**
     *
     * @responseType biocode.fims.rest.services.rest.subResources.ProjectConfigurationResource
     */
    @Path("/{projectId}/config")
    public Resource getProjectConfigurationResource() {
        return Resource.from(ProjectConfigurationResource.class);
    }

    @GET
    @Path("/{projectId}/config/refreshCache")
    public Response refreshCache(@PathParam("projectId") Integer projectId) {
        new ConfigurationFileFetcher(projectId, uploadPath(), false).getOutputFile();

        return Response.noContent().build();
    }

    @Scheduled(cron = "0 0 2 * * *")
    @GET
    @Path("/config/refreshCache")
    public Response refreshAllCache() {
        logger.info("refreshing project config caches");
        List<Project> projects = projectService.getProjects(settingsManager.retrieveValue("appRoot"));

        for (Project p: projects) {
            refreshCache(p.getProjectId());
        }

        return Response.noContent().build();
    }
}
