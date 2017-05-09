package biocode.fims.validation;

import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.Record;
import biocode.fims.projectConfig.ProjectConfig;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class RecordValidatorFactoryTest {

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_if_null_validator_map_given() {
        RecordValidatorFactory validatorFactory = new RecordValidatorFactory(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_if_null_config_given() {
        RecordValidatorFactory validatorFactory = new RecordValidatorFactory(new HashMap<>());
        validatorFactory.getValidator(GenericRecord.class, null);
    }

    @Test
    public void should_return_generic_validator_if_no_validator_found() {
        RecordValidatorFactory validatorFactory = new RecordValidatorFactory(new HashMap<>());
        assertEquals(RecordValidator.class, validatorFactory.getValidator(null, new ProjectConfig(null, null, null)).getClass());
    }

    @Test
    public void should_return_validator_for_class() {
        RecordValidator validator = new RecordValidator();
        Map<Class<? extends Record>, RecordValidator> validators = new HashMap<>();
        validators.put(GenericRecord.class, validator);

        ProjectConfig config = new ProjectConfig(null, null, null);
        RecordValidatorFactory validatorFactory = new RecordValidatorFactory(validators);

        assertEquals(validator, validatorFactory.getValidator(GenericRecord.class, config));
    }

}