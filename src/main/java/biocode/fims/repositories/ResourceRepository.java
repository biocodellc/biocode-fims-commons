package biocode.fims.repositories;

import biocode.fims.models.Resource;

import java.util.List;

/**
 * @author rjewing
 */
public interface ResourceRepository {
    void save(List<Resource> resources, int projectId);
}
