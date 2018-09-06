package biocode.fims.repositories;

import biocode.fims.models.WorksheetTemplate;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author rjewing
 */
@Transactional
public interface ProjectTemplateRepository extends Repository<WorksheetTemplate, Integer> {

    @Modifying
    WorksheetTemplate save(WorksheetTemplate template);

    WorksheetTemplate getByNameAndProjectProjectId(String configName, Integer projectId);

    void deleteByNameAndProjectProjectId(String configName, Integer projectId);
}
