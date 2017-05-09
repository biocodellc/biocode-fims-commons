package biocode.fims.validation;

import biocode.fims.projectConfig.ProjectConfig;

/**
 * @author rjewing
 */
interface ValidatorInstantiator {

    RecordValidator newInstance(ProjectConfig config);
}
