package biocode.fims.dataset;

import biocode.fims.run.ProcessController;

/**
 * Created by rjewing on 6/10/16.
 */
public abstract class AbstractValidator implements Validator {
    private ProcessController processController;

    private boolean validConfiguration() {
        return false;
    }
}
