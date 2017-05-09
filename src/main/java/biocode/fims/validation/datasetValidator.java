package biocode.fims.validation;

import biocode.fims.models.records.RecordSet;

import java.util.List;

/**
 * @author rjewing
 */
public class datasetValidator {
    private final RecordValidatorFactory validatorFactory;
    private final List<RecordSet> recordSets;

    public datasetValidator(RecordValidatorFactory validatorFactory, List<RecordSet> recordSets) {
        this.validatorFactory = validatorFactory;
        this.recordSets = recordSets;
    }
}
