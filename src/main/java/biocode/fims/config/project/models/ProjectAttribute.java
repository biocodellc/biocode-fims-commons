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

    // needed for Jackson
    ProjectAttribute() {
    }

    public ProjectAttribute(Attribute a) {
        uri = a.getUri();
        group = a.getGroup();
        definition = a.getDefinition();
        allowUnknown = a.getAllowUnknown();
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

    Attribute toAttribute(Attribute base) {
        if (base == null) {
            throw new FimsRuntimeException(ConfigCode.MISSING_ATTRIBUTE, 500);
        }
        if (!base.getUri().equals(uri)) {
            throw new FimsRuntimeException(ConfigCode.INVALID, 500);
        }

        Attribute a = new Attribute(base.getColumn(), base.getUri());

        a.setDataType(base.getDataType());
        a.setDataFormat(base.getDataFormat());
        a.setDefinedBy(base.getDefinedBy());
        a.setDelimitedBy(base.getDelimitedBy());
        a.setInternal(base.isInternal());

        a.setGroup(group != null ? group : base.getGroup());
        a.setDefinition(definition != null ? definition : base.getDefinition());
        a.setAllowUnknown(allowUnknown != null ? allowUnknown : base.getAllowUnknown());

        return a;
    }
}
