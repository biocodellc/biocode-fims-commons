package biocode.fims.rest.services.rest;

import biocode.fims.rest.FimsService;
import biocode.fims.service.OAuthProviderService;
import biocode.fims.settings.SettingsManager;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Biocode-Fims utility services
 * @exclude
 */
public abstract class FimsAbstractUtilsController extends FimsService {

    @Autowired
    FimsAbstractUtilsController(SettingsManager settingsManager) {
        super(settingsManager);
    }

    @GET
    @Path("/getNAAN")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNAAN() {
        String naan = settingsManager.retrieveValue("naan");

        return Response.ok("{\"naan\": \"" + naan + "\"}").build();
    }
}
