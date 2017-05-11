package biocode.fims.validation;

import biocode.fims.projectConfig.ProjectConfig;

/**
 * @author rjewing
 */
public interface ValidatorInstantiator {

    RecordValidator newInstance(ProjectConfig config);
}
