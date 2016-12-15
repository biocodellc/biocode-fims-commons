package biocode.fims.rest.services.rest.subResources;

import biocode.fims.entities.Expedition;
import biocode.fims.service.ExpeditionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author RJ Ewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
public class ExpeditionsResource {
    private final ExpeditionService expeditionService;

    @PathParam("projectId")
    private int projectId;

    @Autowired
    public ExpeditionsResource(ExpeditionService expeditionService) {
        this.expeditionService = expeditionService;
    }

    @GET
    public List<Expedition> listExpeditions() {
        // TODO check that the project is public or the user is a member of the project
        return expeditionService.getExpeditions(projectId);
    }

}
