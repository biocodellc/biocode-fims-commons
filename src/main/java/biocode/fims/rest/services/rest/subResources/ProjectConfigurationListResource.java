package biocode.fims.rest.services.rest.subResources;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.digester.Field;
import biocode.fims.models.Project;
import biocode.fims.rest.FimsService;
import biocode.fims.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author RJ Ewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
public class ProjectConfigurationListResource extends FimsService {

    private final ProjectService projectService;

    @Autowired
    public ProjectConfigurationListResource(FimsProperties props, ProjectService projectService) {
        super(props);
        this.projectService = projectService;
    }

    /**
     * Retrieve a list of valid values for a given column
     *
     * @return
     */
    @GET
    @Path("/{listName}/fields")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Field> getListFields(@PathParam("projectId") Integer projectId,
                                     @PathParam("listName") String listName) {

        Project project = projectService.getProject(projectId, props.appRoot());

        if (project == null) {
            throw new BadRequestException("invalid projectId");
        }

        biocode.fims.digester.List list = project.getProjectConfig().findList(listName);
        if (list != null) {
            return list.getFields();
        } else {
            throw new BadRequestException("No list \"" + listName + "\" found");
        }
    }
}
