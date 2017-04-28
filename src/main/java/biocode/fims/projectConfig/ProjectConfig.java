package biocode.fims.projectConfig;

import biocode.fims.digester.Mapping;
import biocode.fims.digester.Metadata;
import biocode.fims.digester.Validation;
import biocode.fims.rest.SpringObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.io.File;

/**
 * TODO remove xml annotations after converting xml files to json
 *
 * @author rjewing
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@XmlRootElement(name = "fims")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProjectConfig {

    @XmlElement
    private Mapping mapping;
    @XmlElement
    private Validation validation;
    @XmlElement
    private Metadata metadata;

    ProjectConfig() {};

    public ProjectConfig(Mapping mapping, Validation validation, Metadata metadata) {
        this.mapping = mapping;
        this.validation = validation;
        this.metadata = metadata;
    }

    public Mapping getMapping() {
        return mapping;
    }

    public void setMapping(Mapping mapping) {
        this.mapping = mapping;
    }

    public Validation getValidation() {
        return validation;
    }

    public void setValidation(Validation validation) {
        this.validation = validation;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public static void main(String[] args) throws Exception {
        File configFile = new File("/Users/rjewing/code/biocode/config-files/dipnet.xml");

        Mapping mapping = new Mapping();
        mapping.addMappingRules(configFile);

        Validation validation = new Validation();
        validation.addValidationRules(configFile, mapping);

        ProjectConfig projectConfig = new ProjectConfig(mapping, validation, mapping.getMetadata());

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        String config = mapper.writeValueAsString(projectConfig);
        JsonNode jsonNode = mapper.valueToTree(projectConfig);

        ProjectConfig pc = mapper.readValue(config, ProjectConfig.class);
        config.toLowerCase();
    }
}
