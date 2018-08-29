package biocode.fims.config.project.models;

import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.DefaultEntity;
import biocode.fims.config.models.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.ConfigCode;
import biocode.fims.models.dataTypes.JacksonUtil;
import biocode.fims.validation.rules.Rule;

import java.util.LinkedHashSet;
import java.util.LinkedList;

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

    // needed for Jackson
    ProjectEntity() {}

    public ProjectEntity(Entity e) {
        conceptAlias = e.getConceptAlias();
        worksheet = e.getWorksheet();
        uniqueKey = e.getUniqueKey();
        uniqueAcrossProject = e.getUniqueAcrossProject();
        hashed = e.isHashed();

        rules = new LinkedHashSet<>();
        e.getRules().stream()
                .filter(entity -> !entity.isNetworkRule())
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

        // TODO should not always be DefaultEntity, need to resolve entity type
        Entity e = new DefaultEntity(base.getConceptAlias(), base.getConceptURI());

        e.setHashed(hashed);
        e.setUniqueAcrossProject(uniqueAcrossProject);
        e.setUniqueKey(uniqueKey);
        e.setWorksheet(worksheet);

        e.setParentEntity(base.getParentEntity());
        e.setRecordType(base.getRecordType());

        base.getRules().forEach(r -> {
            // hacky way to make a copy of the rule so we don't modify the network rule
            Rule newR = JacksonUtil.fromString(
                    JacksonUtil.toString(r),
                    r.getClass()
            );
            newR.setNetworkRule(true);
            e.addRule(newR);
        });
        e.addRules(rules);

        attributes.forEach(a -> {
            Attribute baseAttribute = base.getAttributeByUri(a.getUri());
            e.addAttribute(a.toAttribute(baseAttribute));
        });

        return e;
    }
}
