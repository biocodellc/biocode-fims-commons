package biocode.fims.service;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.ProjectTemplateCode;
import biocode.fims.models.WorksheetTemplate;
import biocode.fims.repositories.ProjectTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Service class for handling {@link WorksheetTemplate} persistence
 */
@Transactional
@Service
public class ProjectTemplateService {
    private final ProjectTemplateRepository projectTemplateRepository;

    @Autowired
    public ProjectTemplateService(ProjectTemplateRepository projectTemplateRepository) {
        this.projectTemplateRepository = projectTemplateRepository;
    }


    public WorksheetTemplate save(WorksheetTemplate template) {
        if (template.getName().equalsIgnoreCase("default")) {
            throw new FimsRuntimeException(ProjectTemplateCode.INVALID_NAME, 400);
        }

        return projectTemplateRepository.save(template);
    }

    public WorksheetTemplate get(String configName, Integer projectId) {
        return projectTemplateRepository.getByNameAndProjectProjectId(configName, projectId);
    }

    public void delete(String configName, Integer projectId) {
        projectTemplateRepository.deleteByNameAndProjectProjectId(configName, projectId);
    }
}

