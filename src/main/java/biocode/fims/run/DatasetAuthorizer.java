package biocode.fims.run;

import biocode.fims.models.EntityIdentifier;
import biocode.fims.models.Project;
import biocode.fims.models.User;

/**
 * @author rjewing
 */
public interface DatasetAuthorizer {
    boolean authorize(Dataset dataset, Project project, User user);
    boolean authorize(EntityIdentifier entityIdentifier, User user);
}
