package biocode.fims.rest.services.rest.resources;

import biocode.fims.entities.Expedition;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.UserContext;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.rest.filters.AuthenticatedUserResource;
import biocode.fims.service.ExpeditionService;
import biocode.fims.settings.SettingsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author RJ Ewing
 */
@Controller
@AuthenticatedUserResource
@Produces(MediaType.APPLICATION_JSON)
public class UserProjectExpeditionsResource extends FimsService {
    private final ExpeditionService expeditionService;

    @Autowired
    UserContext userContext;

    @PathParam("projectId")
    private int projectId;

    @PathParam("userId")
    private int userId;

    @Autowired
    public UserProjectExpeditionsResource(ExpeditionService expeditionService, SettingsManager settingsManager) {
        super(settingsManager);
        this.expeditionService = expeditionService;
    }

    @GET
    public List<Expedition> listExpeditions(@QueryParam("includePrivate") @DefaultValue("true") Boolean includePrivate) {

        // provide backwards compatibility for the deprecated v1 api
        if (userId == 0 && userContext.getUser() != null) {
            userId = userContext.getUser().getUserId();
        }
        return expeditionService.getExpeditionsForUser(projectId, userId, includePrivate);
    }


}
