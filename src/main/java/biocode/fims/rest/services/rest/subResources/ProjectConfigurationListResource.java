package biocode.fims.rest.services.rest.subResources;

import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.Field;
import biocode.fims.digester.Mapping;
import biocode.fims.digester.Validation;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.SpringObjectMapper;
import biocode.fims.settings.SettingsManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.json.simple.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.List;

/**
 * @author RJ Ewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
public class ProjectConfigurationListResource extends FimsService {

    @PathParam("projectId")
    protected Integer projectId;


    @Autowired
    public ProjectConfigurationListResource(SettingsManager settingsManager) {
        super(settingsManager);
    }

    /**
     * Retrieve a list of valid values for a given column
     *
     * @return
     */
    @GET
    @Path("/{listName}/fields")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Field> getListFields(@PathParam("listName") String listName) {

        File configFile = new ConfigurationFileFetcher(projectId, uploadPath(), true).getOutputFile();

        Mapping mapping = new Mapping();
        mapping.addMappingRules(configFile);

        Validation validation = new Validation();
        validation.addValidationRules(configFile, mapping);

        biocode.fims.digester.List list = validation.findList(listName);
        if (list != null) {
            return list.getFields();
        } else {
            throw new biocode.fims.fimsExceptions.BadRequestException("No list \"" + listName + "\" found");
        }
    }
}
