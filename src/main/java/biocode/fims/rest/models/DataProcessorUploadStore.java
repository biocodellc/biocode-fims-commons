package biocode.fims.rest.models;

import biocode.fims.run.DatasetProcessor;

/**
 * A simple store used for holding DatasetProcessor objects between REST requests.
 * <p>
 * This is used between the validation and upload steps.
 *
 * @author rjewing
 */
public class DataProcessorUploadStore extends UploadStore<DatasetProcessor> {
}
