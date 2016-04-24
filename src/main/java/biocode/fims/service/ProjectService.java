package biocode.fims.service;

import biocode.fims.entities.Project;
import biocode.fims.repositories.ProjectRepository;
import biocode.fims.settings.SettingsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Service class for handling {@link Project} persistence
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final SettingsManager settingsManager;

    @Autowired
    public ProjectService(ProjectRepository projectRepository,
                          SettingsManager settingsManager) {
        this.projectRepository = projectRepository;
        this.settingsManager = settingsManager;
    }

    @Transactional
    public void create(Project project) {
        projectRepository.save(project);

    }

    public void update(Project project) {
        projectRepository.save(project);
    }

    public Project getProject(int projectId) {
        return projectRepository.findByProjectId(projectId);
    }
}
