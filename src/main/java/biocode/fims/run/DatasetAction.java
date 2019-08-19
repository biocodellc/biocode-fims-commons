package biocode.fims.run;

import biocode.fims.models.Project;

/**
 * @author rjewing
 */
public interface DatasetAction {
    void onSave(Project project, Dataset dataset);
}
