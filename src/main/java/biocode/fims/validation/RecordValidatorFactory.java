package biocode.fims.validation;

import biocode.fims.models.records.Record;
import biocode.fims.projectConfig.ProjectConfig;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * @author rjewing
 */
public class RecordValidatorFactory {
    private final Map<Class<? extends Record>, RecordValidator> validators;

    public RecordValidatorFactory(Map<Class<? extends Record>, RecordValidator> validators) {
        Assert.notNull(validators);
        this.validators = validators;
    }

    public RecordValidator getValidator(Class<? extends Record> recordType, ProjectConfig config) {
        Assert.notNull(config);

        RecordValidator validator = validators.getOrDefault(
                recordType,
                new GenericRecordValidator()
        );

        validator.setProjectConfig(config);

        return validator;
    }
}
