package biocode.fims.validation;

import biocode.fims.config.project.ProjectConfig;

/**
 * @author rjewing
 */
public interface ValidatorInstantiator {

    RecordValidator newInstance(ProjectConfig config);
}
