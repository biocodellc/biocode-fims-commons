package biocode.fims.rest.services.rest.subResources;

import biocode.fims.entities.Expedition;
import biocode.fims.entities.Project;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.UserEntityGraph;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.settings.SettingsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * @author RJ Ewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
public class ExpeditionsResource extends FimsService {
    private final ExpeditionService expeditionService;
    private final ProjectService projectService;

    @PathParam("projectId")
    private int projectId;

    @Autowired
    public ExpeditionsResource(ExpeditionService expeditionService, ProjectService projectService,
                               SettingsManager settingsManager) {
        super(settingsManager);
        this.expeditionService = expeditionService;
        this.projectService = projectService;
    }

    @UserEntityGraph("User.withProjectsMemberOf")
    @GET
    public List<Expedition> listExpeditions() {
        Project project = projectService.getProjectWithExpeditions(projectId);

        if (!project.isPublic() && !projectService.isUserMemberOfProject(userContext.getUser(), project)) {
            throw new ForbiddenRequestException("You are not a member of this private project");
        }

        return project.getExpeditions()
                .stream()
                .filter(Expedition::isPublic)
                .collect(toList());
    }

}
