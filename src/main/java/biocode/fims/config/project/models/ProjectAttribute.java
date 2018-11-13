package biocode.fims.config.project.models;

import biocode.fims.config.models.Attribute;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.ConfigCode;

/**
 * Object for storing @link{Attribute} information configurable by the project
 *
 * @author rjewing
 */
public class ProjectAttribute {
    // uri of the Attribute to override
    private String uri;
    private String group;
    private String definition;
    private Boolean allowUnknown;
    private Boolean allowTBD;

    // needed for Jackson
    ProjectAttribute() {
    }

    public ProjectAttribute(Attribute a) {
        uri = a.getUri();
        group = a.getGroup();
        definition = a.getDefinition();
        allowUnknown = a.getAllowUnknown();
        allowTBD = a.getAllowTBD();
    }

    public String getUri() {
        return uri;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public Boolean getAllowUnknown() {
        return allowUnknown;
    }

    public void setAllowUnknown(Boolean allowUnknown) {
        this.allowUnknown = allowUnknown;
    }

    public Boolean getAllowTBD() {
        return allowTBD;
    }

    public void setAllowTBD(Boolean allowTBD) {
        this.allowTBD = allowTBD;
    }

    Attribute toAttribute(Attribute base) {
        if (base == null) {
            throw new FimsRuntimeException(ConfigCode.MISSING_ATTRIBUTE, 500);
        }
        if (!base.getUri().equals(uri)) {
            throw new FimsRuntimeException(ConfigCode.INVALID, 500);
        }

        Attribute a = base.clone();

        a.setGroup(group != null ? group : base.getGroup());
        a.setDefinition(definition != null ? definition : base.getDefinition());
        a.setAllowUnknown(allowUnknown != null ? allowUnknown : base.getAllowUnknown());
        a.setAllowTBD(allowTBD != null ? allowTBD: base.getAllowTBD());

        return a;
    }
}
