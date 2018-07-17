package biocode.fims.authorizers;

import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.repositories.ProjectRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;


/**
 * @author rjewing
 */
public class ProjectAuthorizerTest {

    @Mock
    private ProjectRepository projectRepository;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void userHasAccess_unAuthenticatedUserPublicProject_true() {
        Project project = new Project.ProjectBuilder("TEST", "TEST Project", null)
                .isPublic(true)
                .build();

        ProjectAuthorizer projectAuthorizer = new ProjectAuthorizer(projectRepository);
        Assert.assertEquals(projectAuthorizer.userHasAccess(null, project), true);
    }

    @Test
    public void userHasAccess_unAuthenticatedUserPrivateProject_false() {
        Project project = new Project.ProjectBuilder("TEST", "TEST Project", null)
                .isPublic(false)
                .build();

        ProjectAuthorizer projectAuthorizer = new ProjectAuthorizer(projectRepository);
        Assert.assertEquals(projectAuthorizer.userHasAccess(null, project), false);
    }

    @Test
    public void userHasAccess_AuthenticatedMemberUserPrivateProject_true() {
        Project project = new Project.ProjectBuilder("TEST", "TEST Project", null)
                .isPublic(false)
                .build();

        User user = getUser();

        Mockito.when(projectRepository.userIsMember(project.getProjectId(), user.getUserId())).thenReturn(true);

        ProjectAuthorizer projectAuthorizer = new ProjectAuthorizer(projectRepository);
        Assert.assertEquals(projectAuthorizer.userHasAccess(user, project), true);
    }

    @Test
    public void userHasAccess_AuthenticatedNonMemberUserPrivateProject_false() {
        Project project = new Project.ProjectBuilder("TEST", "TEST Project", null)
                .isPublic(false)
                .build();

        User user = getUser();

        Mockito.when(projectRepository.userIsMember(project.getProjectId(), user.getUserId())).thenReturn(false);

        ProjectAuthorizer projectAuthorizer = new ProjectAuthorizer(projectRepository);
        Assert.assertEquals(projectAuthorizer.userHasAccess(user, project), false);
    }

    private User getUser() {
        return new User.UserBuilder("test", "test")
                .email("test@example.com")
                .institution("university")
                .name("test", "user")
                .build();
    }

}