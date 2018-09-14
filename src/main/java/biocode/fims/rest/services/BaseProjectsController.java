package biocode.fims.rest.services;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.services.subResources.*;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import org.glassfish.jersey.server.model.Resource;

import javax.ws.rs.*;

/**
 * API endpoints for working with projects. This includes fetching details associated with projects.
 * Currently, there are no REST services for creating projects, which instead must be added to the Database
 * manually by an administrator
 *
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
     * @responseType biocode.fims.rest.services.subResources.ProjectsResource
     */
    @Path("/")
    public Resource getProjectsResource() {
        return Resource.from(ProjectsResource.class);
    }

    /**
     * @responseType biocode.fims.rest.services.subResources.ProjectTemplatesResource
     * @resourceTag Templates
     */
    @Path("{projectId}/templates")
    public Resource getTemplatesResource() {
        return Resource.from(ProjectTemplatesResource.class);
    }

    /**
     * @responseType biocode.fims.rest.services.subResources.ExpeditionsResource
     * @resourceTag Expeditions
     */
    @Path("{projectId}/expeditions")
    public Resource getExpeditionsResource() {
        return Resource.from(ExpeditionsResource.class);
    }

    /**
     * @responseType biocode.fims.rest.services.subResources.ProjectMembersResource
     * @resourceTag Members
     */
    @Path("{projectId}/members")
    public Resource getProjectMembersResource() {
        return Resource.from(ProjectMembersResource.class);
    }

    /**
     * @responseType biocode.fims.rest.services.subResources.ProjectConfigResource
     */
    @Path("/{projectId}/config")
    public Resource getProjectConfigResource() {
        return Resource.from(ProjectConfigResource.class);
    }

    /**
     * @responseType biocode.fims.rest.services.subResources.ProjectConfigurationResource
     * @resourceTag Project Configurations
     */
    @Path("/configs")
    public Resource getProjectConfigurationResource() {
        return Resource.from(ProjectConfigurationResource.class);
    }
}
