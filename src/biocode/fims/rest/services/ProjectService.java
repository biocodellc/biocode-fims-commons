package biocode.fims.rest.services;

import biocode.fims.bcid.ExpeditionMinter;
import biocode.fims.bcid.ProjectMinter;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.filters.Admin;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.run.TemplateProcessor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Hashtable;
import java.util.List;

/**
 * REST interface calls for working with projects.  This includes fetching details associated with projects.
 * Currently, there are no REST services for creating projects, which instead must be added to the Database
 * manually by an administrator
 */
@Path("projects")
public class ProjectService extends FimsService {

    /**
     * Produce a list of all publically available projects and the private projects the logged in user is a memeber of
     *
     * @return  Generates a JSON listing containing project metadata as an array
     */
    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fetchList() {

        ProjectMinter project = new ProjectMinter();
        JSONArray response = project.listProjects(userId);
        project.close();

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

        JSONArray graphs = project.getLatestGraphs(projectId, username);
        project.close();

        return Response.ok(graphs.toJSONString()).header("Access-Control-Allow-Origin", "*").build();
    }

    @GET
    @Path("/{projectId}/abstract")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAbstract(@PathParam("projectId") int projectId) {
        JSONObject obj = new JSONObject();
        TemplateProcessor t = new TemplateProcessor(projectId, uploadPath(), true);

        // Write the all of the checkbox definitions to a String Variable
        obj.put("abstract", JSONValue.escape(t.printAbstract()));

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

        String response = project.getMyLatestGraphs(username);
        project.close();

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

        String response = project.getMyTemplatesAndDatasets(username);
        project.close();

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
        JSONArray projects = project.getAdminProjects(username);
        project.close();

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

        try {
            if (!p.isProjectAdmin(userId, projectID)) {
                throw new ForbiddenRequestException("You must be this project's admin in order to update the metadata");
            }

            JSONObject metadata = p.getMetadata(projectID, username);
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
        } finally {
            p.close();
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
        if (!p.isProjectAdmin(username, projectId)) {
            throw new ForbiddenRequestException("You are not this project's admin.");
        }

        p.removeUser(userId, projectId);
        p.close();

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
        if (!p.isProjectAdmin(username, projectId)) {
            p.close();
            throw new ForbiddenRequestException("You are not this project's admin");
        }
        p.addUserToProject(userId, projectId);
        p.close();

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
        JSONArray projects = p.listUsersProjects(username);
        p.close();
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
    @Path("/{projectId}/saveConfig")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response saveTemplateConfig(@FormParam("checkedOptions") List<String> checkedOptions,
                                       @FormParam("configName") String configName,
                                       @PathParam("projectId") Integer projectId) {

        if (configName.equalsIgnoreCase("default")) {
            return Response.ok("{\"error\": \"To change the default config, talk to the project admin.\"}").build();
        }

        ProjectMinter projectMinter = new ProjectMinter();

        if (projectMinter.configExists(configName, projectId)) {
            if (projectMinter.usersConfig(configName, projectId, userId)) {
                projectMinter.updateTemplateConfig(configName, projectId, userId, checkedOptions);
            } else {
                return Response.ok("{\"error\": \"A configuration with that name already exists, and you are not the owner.\"}").build();
            }
        } else {
            projectMinter.saveTemplateConfig(configName, projectId, userId, checkedOptions);
        }
        projectMinter.close();

        return Response.ok("{\"success\": \"Successfully saved template configuration.\"}").build();
    }

    /**
     * Service used to get the fims template generator configurations for a given project
     * @param projectId
     * @return
     */
    @GET
    @Path("/{projectId}/getConfigs/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTemplateConfigs(@PathParam("projectId") Integer projectId) {
        ProjectMinter p = new ProjectMinter();
        JSONArray configs = p.getTemplateConfigs(projectId);
        p.close();

        return Response.ok(configs.toJSONString()).build();
    }

    /**
     * Service used to get a specific fims template generator configuration
     * @param configName
     * @param projectId
     * @return
     */
    @GET
    @Path("/{projectId}/getConfig/{configName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfig(@PathParam("configName") String configName,
                              @PathParam("projectId") Integer projectId) {
        ProjectMinter p = new ProjectMinter();
        JSONObject response = p.getTemplateConfig(configName, projectId);
        p.close();

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
    @Path("/{projectId}/removeConfig/{configName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeConfig(@PathParam("configName") String configName,
                                 @PathParam("projectId") Integer projectId) {
        if (configName.equalsIgnoreCase("default")) {
            return Response.ok("{\"error\": \"To remove the default config, talk to the project admin.\"}").build();
        }

        ProjectMinter p = new ProjectMinter();
        if (p.configExists(configName, projectId) && p.usersConfig(configName, projectId, userId)) {
            p.removeTemplateConfig(configName, projectId);
        } else {
            return Response.ok("{\"error\": \"Only the owners of a configuration can remove the configuration.\"}").build();
        }
        p.close();

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
    public Response listExpeditions(@PathParam("projectId") Integer projectId) {
        ExpeditionMinter e = new ExpeditionMinter();
        JSONArray expeditions = e.listExpeditions(projectId, username);
        e.close();

        return Response.ok(expeditions.toJSONString()).build();
    }
}
