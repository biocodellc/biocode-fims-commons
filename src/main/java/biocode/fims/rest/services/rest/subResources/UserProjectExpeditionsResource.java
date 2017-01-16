package biocode.fims.rest.services.rest.subResources;

import biocode.fims.entities.Expedition;
import biocode.fims.rest.FimsService;
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
 * @resourceTag Expeditions
 */
@Controller
@AuthenticatedUserResource
@Produces(MediaType.APPLICATION_JSON)
public class UserProjectExpeditionsResource extends FimsService {
    private final ExpeditionService expeditionService;

    @Autowired
    public UserProjectExpeditionsResource(ExpeditionService expeditionService, SettingsManager settingsManager) {
        super(settingsManager);
        this.expeditionService = expeditionService;
    }

    @GET
    public List<Expedition> listExpeditions(@PathParam("projectId") Integer projectId,
                                            @PathParam("userId") Integer userId,
                                            @QueryParam("includePrivate") @DefaultValue("true") Boolean includePrivate) {

        // provide backwards compatibility for the deprecated v1 api
        if (userId == 0 && userContext.getUser() != null) {
            userId = userContext.getUser().getUserId();
        }
        return expeditionService.getExpeditionsForUser(projectId, userId, includePrivate);
    }


}
