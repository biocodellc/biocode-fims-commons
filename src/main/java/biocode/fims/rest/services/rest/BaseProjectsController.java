package biocode.fims.rest.services.rest;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.services.rest.subResources.*;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import org.glassfish.jersey.server.model.Resource;

import javax.ws.rs.*;

/**
 * API endpoints for working with projects. This includes fetching details associated with projects.
 * Currently, there are no REST services for creating projects, which instead must be added to the Database
 * manually by an administrator
 * @exclude
 */
public abstract class BaseProjectsController extends FimsController {
    protected ExpeditionService expeditionService;
    protected ProjectService projectService;

    BaseProjectsController(ExpeditionService expeditionService, FimsProperties props,
                           ProjectService projectService) {
        super(props);
        this.expeditionService = expeditionService;
        this.projectService = projectService;
    }


    /**
     *
     * @responseType biocode.fims.rest.services.rest.subResources.ProjectsResource
     */
    @Path("/")
    public Resource getProjectsResource() {
        return Resource.from(ProjectsResource.class);
    }

    /**
     *
     * @responseType biocode.fims.rest.services.rest.subResources.ProjectTemplatesResource
     * @resourceTag Templates
     */
    @Path("{projectId}/templates")
    public Resource getTemplatesResource() {
        return Resource.from(ProjectTemplatesResource.class);
    }

    /**
     *
     * @responseType biocode.fims.rest.services.rest.subResources.ExpeditionsResource
     * @resourceTag Expeditions
     */
    @Path("{projectId}/expeditions")
    public Resource getExpeditionsResource() {
        return Resource.from(ExpeditionsResource.class);
    }

    /**
     *
     * @responseType biocode.fims.rest.services.rest.subResources.ProjectMembersResource
     * @resourceTag Members
     */
    @Path("{projectId}/members")
    public Resource getProjectMembersResource() {
        return Resource.from(ProjectMembersResource.class);
    }

    /**
     *
     * @responseType biocode.fims.rest.services.rest.subResources.ProjectConfigurationResource
     * @resourceTag Config
     */
    @Path("/{projectId}/config")
    public Resource getProjectConfigurationResource() {
        return Resource.from(ProjectConfigurationResource.class);
    }
}
