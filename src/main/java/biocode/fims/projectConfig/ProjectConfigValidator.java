package biocode.fims.projectConfig;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.DataType;
import biocode.fims.digester.Entity;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * class to validate @link(ProjectConfig) instances
 *
 * @author rjewing
 */
public class ProjectConfigValidator {
    private final ProjectConfig config;
    private List<String> errorMessages;

    public ProjectConfigValidator(ProjectConfig config) {
        this.config = config;
        this.errorMessages = new ArrayList<>();
    }

    public boolean isValid() {
        if (config.getMapping() == null) {
            return false;
        }

        validateMapping();

        return errorMessages.isEmpty();
    }

    private void validateMapping() {
        allEntitiesHaveUniqueConceptAlias();
        entityWithWorksheetHasUniqueKey();
        dateTimeAttributesHaveDataFormat();
    }

    private void dateTimeAttributesHaveDataFormat() {
        List<DataType> dataTimeDataTypes = Arrays.asList(DataType.DATE, DataType.DATETIME, DataType.TIME);

        for (Entity e : config.getMapping().getEntities()) {
            for (Attribute a : e.getAttributes()) {
                if (dataTimeDataTypes.contains(a.getDatatype())
                        && StringUtils.isEmpty(a.getDataformat())) {
                    errorMessages.add("Entity \"" + e.getConceptAlias() + "\" specifies an attribute \""
                            + a.getUri() + "\" with dataType \"" + a.getDatatype() + "\" but is missing a dataFormat");
                }
            }
        }
    }

    private void entityWithWorksheetHasUniqueKey() {
        for (Entity e : config.getMapping().getEntities()) {
            if (!StringUtils.isEmpty(e.getWorksheet()) && StringUtils.isEmpty(e.getUniqueKey())) {
                errorMessages.add("Entity \"" + e.getConceptAlias() + "\" specifies a worksheet but is missing a uniqueKey");
            }
        }
    }

    private void allEntitiesHaveUniqueConceptAlias() {
        Set<String> uniqueConceptAlias = new HashSet<>();

        for (Entity e : config.getMapping().getEntities()) {
            if (StringUtils.isEmpty(e.getConceptAlias())) {
                errorMessages.add("Entity is missing a conceptAlias");
            } else if (!uniqueConceptAlias.add(e.getConceptAlias())) {
                errorMessages.add("Duplicate entity conceptAlias detected \"" + e.getConceptAlias() + "\"");
            }
        }
    }

    public List<String> errors() {
        return errorMessages;
    }
}
