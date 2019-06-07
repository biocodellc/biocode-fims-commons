package biocode.fims.config.project.models;

import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.DefaultEntity;
import biocode.fims.config.models.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.ConfigCode;
import biocode.fims.models.dataTypes.JacksonUtil;
import biocode.fims.serializers.EntityTypeIdResolver;
import biocode.fims.validation.rules.Rule;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Object for storing @link{Entity} information configurable by the project
 *
 * @author rjewing
 */
public class ProjectEntity {

    // conceptAlias of the Entity to override
    private String conceptAlias;

    private LinkedList<ProjectAttribute> attributes;
    // rule extensions set by the project
    private LinkedHashSet<Rule> rules;

    private String worksheet;
    private String uniqueKey;
    private boolean uniqueAcrossProject = false;
    private boolean hashed = false;
    private Map<String, Object> additionalProps;

    // needed for Jackson
    ProjectEntity() {
    }

    public ProjectEntity(Entity e) {
        conceptAlias = e.getConceptAlias();
        worksheet = e.getWorksheet();
        uniqueKey = e.getUniqueKey();
        uniqueAcrossProject = e.getUniqueAcrossProject();
        hashed = e.isHashed();
        additionalProps = e.additionalProps() != null ? e.additionalProps() : Collections.emptyMap();

        rules = new LinkedHashSet<>();
        e.getRules().stream()
                .filter(rule -> !rule.isNetworkRule())
                .forEach(rules::add);

        attributes = new LinkedList<>();
        e.getAttributes().forEach(a -> addAttribute(
                new ProjectAttribute(a)
        ));
    }

    public String getConceptAlias() {
        return conceptAlias;
    }

    public LinkedList<ProjectAttribute> getAttributes() {
        return attributes;
    }

    public boolean addAttribute(ProjectAttribute projectAttribute) {
        return attributes.add(projectAttribute);
    }

    public LinkedHashSet<Rule> getRules() {
        return rules;
    }

    public boolean addRule(Rule rule) {
        return rules.add(rule);
    }

    public String getWorksheet() {
        return worksheet;
    }

    public void setWorksheet(String worksheet) {
        this.worksheet = worksheet;
    }

    public String getUniqueKey() {
        return uniqueKey;
    }

    public void setUniqueKey(String uniqueKey) {
        this.uniqueKey = uniqueKey;
    }

    public boolean isUniqueAcrossProject() {
        return uniqueAcrossProject;
    }

    public void setUniqueAcrossProject(boolean uniqueAcrossProject) {
        this.uniqueAcrossProject = uniqueAcrossProject;
    }

    public Map<String, Object> getAdditionalProps() {
        return additionalProps;
    }

    public void setAdditionalProps(Map<String, Object> additionalProps) {
        this.additionalProps = additionalProps;
    }

    public boolean isHashed() {
        return hashed;
    }

    public void setHashed(boolean hashed) {
        this.hashed = hashed;
    }

    Entity toEntity(Entity base) {
        if (base == null) {
            throw new FimsRuntimeException(ConfigCode.UNKNOWN_ENTITY, 500);
        }

        if (!base.getConceptAlias().equals(conceptAlias)) {
            throw new FimsRuntimeException(ConfigCode.INVALID, 500);
        }

        // get a copy of the base so we don't modify the base entity
        Entity e = base.clone();

        e.setHashed(hashed);
        e.setUniqueAcrossProject(uniqueAcrossProject);
        e.setUniqueKey(uniqueKey);
        e.setWorksheet(worksheet);
        e.setAdditionalProps(additionalProps);

        e.getAttributes().clear();
        attributes.forEach(a -> {
            Attribute baseAttribute = base.getAttributeByUri(a.getUri());
            e.addAttribute(a.toAttribute(baseAttribute));
        });

        base.getAttributes().stream()
                .filter(a -> a.isInternal() && !e.getAttributes().contains(a))
                .forEach(e::addAttribute);

        List<String> columns = e.getAttributes().stream()
                .map(Attribute::getColumn)
                .collect(Collectors.toList());

        e.getRules().clear();
        base.getRules().forEach(r -> {
            r = r.toProjectRule(columns);
            if (r != null) e.addRule(r);
        });
        e.addRules(this.rules);

        return e;
    }
}
