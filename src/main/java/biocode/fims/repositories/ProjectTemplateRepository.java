package biocode.fims.repositories;

import biocode.fims.models.ProjectTemplate;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author rjewing
 */
@Transactional
public interface ProjectTemplateRepository extends Repository<ProjectTemplate, Integer> {

    @Modifying
    ProjectTemplate save(ProjectTemplate template);

    ProjectTemplate getByNameAndProjectProjectId(String configName, Integer projectId);

    void deleteByNameAndProjectProjectId(String configName, Integer projectId);
}
