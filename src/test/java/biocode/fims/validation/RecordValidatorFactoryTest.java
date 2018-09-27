package biocode.fims.validation;

import biocode.fims.config.project.ProjectConfig;
import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;
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
        new RecordValidatorFactory(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_if_null_config_given() {
        RecordValidatorFactory validatorFactory = new RecordValidatorFactory(new HashMap<>());
        validatorFactory.getValidator(GenericRecord.class, null);
    }

    @Test
    public void should_return_generic_validator_if_no_validator_found() {
        RecordValidatorFactory validatorFactory = new RecordValidatorFactory(new HashMap<>());
        assertEquals(RecordValidator.class, validatorFactory.getValidator(null, new ProjectConfig()).getClass());
    }

    @Test
    public void should_return_validator_for_class() {
        Map<Class<? extends Record>, ValidatorInstantiator> validators = new HashMap<>();
        validators.put(GenericRecord.class, new TestRecordValidator.TestValidatorInstantiator());

        ProjectConfig config = new ProjectConfig();
        RecordValidatorFactory validatorFactory = new RecordValidatorFactory(validators);

        assertEquals(TestRecordValidator.class, validatorFactory.getValidator(GenericRecord.class, config).getClass());
    }

}