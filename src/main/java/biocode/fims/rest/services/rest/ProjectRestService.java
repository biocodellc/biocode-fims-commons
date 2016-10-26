package biocode.fims.rest.services.rest;

import biocode.fims.bcid.ProjectMinter;
import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.Field;
import biocode.fims.digester.Mapping;
import biocode.fims.digester.Validation;
import biocode.fims.entities.Expedition;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.filters.Admin;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.run.TemplateProcessor;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.OAuthProviderService;
import biocode.fims.settings.SettingsManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.*;

/**
 * REST interface calls for working with projects.  This includes fetching details associated with projects.
 * Currently, there are no REST services for creating projects, which instead must be added to the Database
 * manually by an administrator
 */
@Path("projects")
public class ProjectRestService extends FimsService {
    private ExpeditionService expeditionService;

    @Autowired
    ProjectRestService(ExpeditionService expeditionService,
                       OAuthProviderService providerService, SettingsManager settingsManager) {
        super(providerService, settingsManager);
        this.expeditionService = expeditionService;
    }

    /**
     * Produce a list of all publically available projects and the private projects the logged in user is a memeber of
     *
     * @return  Generates a JSON listing containing project metadata as an array
     */
    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fetchList(@QueryParam("includePublic") @DefaultValue("false") boolean includePublic) {

        ProjectMinter project = new ProjectMinter();
        Integer userId = null;

        if (user == null && !includePublic) {
            throw new BadRequestException("You must be logged in if you don't want to include public projects");
        }
        if (user != null) {
            userId = user.getUserId();
        }

        JSONArray response = project.listProjects(userId, includePublic);

        return Response.ok(response.toJSONString()).header("Access-Control-Allow-Origin", "*").build();
    }

    /**
     * Given a project id, get the latest graphs by expedition
     *
     * @param projectId
     * @return
     */
    @GET
    @Path("/{projectId}/graphs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLatestGraphsByExpedition(@PathParam("projectId") Integer projectId) {
        ProjectMinter project= new ProjectMinter();
        String username = null;
        if (user != null) {
            username = user.getUsername();
        }

        JSONArray graphs = project.getLatestGraphs(projectId, username);

        return Response.ok(graphs.toJSONString()).header("Access-Control-Allow-Origin", "*").build();
    }

    @GET
    @Path("/{projectId}/abstract")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAbstract(@PathParam("projectId") int projectId) {
        JSONObject obj = new JSONObject();
        TemplateProcessor t = new TemplateProcessor(projectId, uploadPath(), true);

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
        ProjectMinter project= new ProjectMinter();

        String response = project.getMyLatestGraphs(user.getUsername());

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
        ProjectMinter project= new ProjectMinter();

        String response = project.getMyTemplatesAndDatasets(user.getUsername());

        return Response.ok(response).header("Access-Control-Allow-Origin", "*").build();
    }

    /**
     * Return a json representation to be used for select options of the projects that a user is an admin to
     * @return
     */
    @GET
    @Authenticated
    @Admin
    @Path("/admin/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserAdminProjects() {
        ProjectMinter project= new ProjectMinter();
        JSONArray projects = project.getAdminProjects(user.getUsername());

        return Response.ok(projects.toJSONString()).build();
    }


    /**
     * Service used for updating a project's configuration.
     * @param projectID
     * @param title
     * @param validationXML
     * @param publicProject
     * @return
     */
    @POST
    @Authenticated
    @Admin
    @Path("/{projectId}/metadata/update")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateConfig(@PathParam("projectId") Integer projectID,
                                 @FormParam("title") String title,
                                 @FormParam("validationXml") String validationXML,
                                 @FormParam("public") String publicProject) {
        ProjectMinter p = new ProjectMinter();

        if (!p.isProjectAdmin(user.getUserId(), projectID)) {
            throw new ForbiddenRequestException("You must be this project's admin in order to update the metadata");
        }

        JSONObject metadata = p.getMetadata(projectID, user.getUsername());
        Hashtable<String, String> update = new Hashtable<String, String>();

        if (title != null &&
                !metadata.get("title").equals(title)) {
            update.put("projectTitle", title);
        }
        if (!metadata.containsKey("validationXml") || !metadata.get("validationXml").equals(validationXML)) {
            update.put("validationXml", validationXML);
        }
        if ((publicProject != null && (publicProject.equals("on") || publicProject.equals("true")) && metadata.get("public").equals("false")) ||
                (publicProject == null && metadata.get("public").equals("true"))) {
            if (publicProject != null && (publicProject.equals("on") || publicProject.equals("true"))) {
                update.put("public", "true");
            } else {
                update.put("public", "false");
            }
        }

        if (!update.isEmpty()) {
            if (p.updateMetadata(update, projectID)) {
                return Response.ok("{\"success\": \"Successfully updated project metadata.\"}").build();
            } else {
                throw new BadRequestException("Project wasn't found");
            }
        } else {
            return Response.ok("{\"success\": \"nothing needed to be updated\"}").build();
        }
    }

    /**
     * Service used to remove a user as a member of a project.
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
        if (!p.isProjectAdmin(user.getUsername(), projectId)) {
            throw new ForbiddenRequestException("You are not this project's admin.");
        }

        p.removeUser(userId, projectId);

        return Response.ok("{\"success\": \"User has been successfully removed\"}").build();
    }

    /**
     * Service used to add a user as a member of a project.
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
        if (!p.isProjectAdmin(user.getUsername(), projectId)) {
            throw new ForbiddenRequestException("You are not this project's admin");
        }
        p.addUserToProject(userId, projectId);

        return Response.ok("{\"success\": \"User has been successfully added to this project\"}").build();
    }

    /**
     * Service used to retrieve a JSON representation of the project's a user is a member of.
     * @return
     */
    @GET
    @Authenticated
    @Path("/user/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserProjects() {
        ProjectMinter p = new ProjectMinter();
        JSONArray projects = p.listUsersProjects(user.getUsername());
        return Response.ok(projects.toJSONString()).build();
    }

    /**
     * Service used to save a fims template generator configuration
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
            if (projectMinter.usersTemplateConfig(configName, projectId, user.getUserId())) {
                projectMinter.updateTemplateConfig(configName, projectId, user.getUserId(), checkedOptions);
            } else {
                return Response.ok("{\"error\": \"A configuration with that name already exists, and you are not the owner.\"}").build();
            }
        } else {
            projectMinter.saveTemplateConfig(configName, projectId, user.getUserId(), checkedOptions);
        }

        return Response.ok("{\"success\": \"Successfully saved template configuration.\"}").build();
    }

    /**
     * Service used to get the fims template generator configurations for a given project
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
        if (p.templateConfigExists(configName, projectId) && p.usersTemplateConfig(configName, projectId, user.getUserId())) {
            p.removeTemplateConfig(configName, projectId);
        } else {
            return Response.ok("{\"error\": \"Only the owners of a configuration can remove the configuration.\"}").build();
        }

        return Response.ok("{\"success\": \"Successfully removed template configuration.\"}").build();
    }

    /**
     * Return a JSON representation of the expedition's that a user is a member of
     *
     * @param projectId
     *
     * @return
     */
    @GET
    @Authenticated
    @Path("/{projectId}/expeditions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Expedition> listExpeditions(@PathParam("projectId") Integer projectId) {
        return expeditionService.getExpeditions(projectId, user.getUserId(), false);
    }

    /**
     * Retrieve a list of valid values for a given column
     *
     * @param projectId
     *
     * @return
     */
    @GET
    @Path("/{projectId}/getListFields/{listName}/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getListFields(@PathParam("projectId") Integer projectId,
                                  @PathParam("listName") String listName) {

        File configFile = new ConfigurationFileFetcher(projectId, uploadPath(), true).getOutputFile();

        Mapping mapping = new Mapping();
        mapping.addMappingRules(configFile);

        Validation validation = new Validation();
        validation.addValidationRules(configFile, mapping);

        biocode.fims.digester.List results = validation.findList(listName);
        JSONArray list = new JSONArray();

        Iterator it = results.getFields().iterator();

        // Get field values
        while (it.hasNext()) {
            Field f = (Field)it.next();
            list.add(f.getValue());
        }

        return Response.ok(list.toJSONString()).build();
    }
}
