package biocode.fims.validation;

import biocode.fims.models.records.Record;
import biocode.fims.projectConfig.ProjectConfig;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * @author rjewing
 */
public class RecordValidatorFactory {
    private final Map<Class<? extends Record>, ValidatorInstantiator> validators;

    public RecordValidatorFactory(Map<Class<? extends Record>, ValidatorInstantiator> validators) {
        Assert.notNull(validators);
        this.validators = validators;
    }

    public RecordValidator getValidator(Class<? extends Record> recordType, ProjectConfig config) {
        Assert.notNull(config);

        return validators.getOrDefault(recordType, new RecordValidator.DefaultValidatorInstantiator()).newInstance(config);
    }
}
