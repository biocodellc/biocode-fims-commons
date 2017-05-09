package biocode.fims.validation;

import biocode.fims.projectConfig.ProjectConfig;

/**
 * @author rjewing
 */
public class TestRecordValidator extends RecordValidator {
    public TestRecordValidator(ProjectConfig config) {
        super(config);
    }

    public static class TestValidatorInstantiator implements ValidatorInstantiator {

        @Override
        public RecordValidator newInstance(ProjectConfig config) {
            return new TestRecordValidator(config);
        }
    }
}
