package biocode.fims.service;

import biocode.fims.entities.Project;
import biocode.fims.entities.User;
import biocode.fims.repositories.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Service class for handling {@link Project} persistence
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Autowired
    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
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


    public boolean isUserMemberOfProject(User user, Project project) {
        boolean userIsProjectMember = false;
        for (Project userProject: user.getProjectsMemberOf()) {
            if (userProject.equals(project))
                userIsProjectMember = true;
        }
        return userIsProjectMember;
    }
}
