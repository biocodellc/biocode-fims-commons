package biocode.fims.service;

import biocode.fims.auth.PasswordHash;
import biocode.fims.entities.Project;
import biocode.fims.entities.User;
import biocode.fims.repositories.UserRepository;
import biocode.fims.settings.SettingsManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import javax.persistence.Inheritance;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link UserService}
 */
public class UserServiceTest {
    private String INVALID_PASSWORD = "password";
    private String PASSWORD = "correct";
    private String APP_ROOT = "http://localhost/";
    private User user;

    @InjectMocks
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SettingsManager settingsManager;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        user = spy(new User.UserBuilder("demo", PasswordHash.createHash(PASSWORD))
                .name("test", "account")
                .email("test@example.com")
                .institution("biocode")
                .build());

        Mockito.when(settingsManager.retrieveValue("appRoot")).thenReturn(APP_ROOT);
    }

    @Test
    public void getUser_with_username_and_password_should_return_null_with_wrong_password() {
        Mockito.when(userService.getUser("demo")).thenReturn(user);
        assertNull(userService.getUser("demo", INVALID_PASSWORD));
    }

    @Test
    public void getUser_with_username_and_password_should_return_null_with_empty_password() {
        Mockito.when(userService.getUser("demo")).thenReturn(user);
        assertNull(userService.getUser("demo", ""));
    }

    @Test
    public void getUser_with_username_and_password_should_return_null_with_valid_password_and_not_member_of_project_at_domain() {
        Project project = new Project.ProjectBuilder("PROJ", "Test project", "n/a", "http://localhost/test/")
                .build();
        Set<Project> projects = new HashSet<>();
        projects.add(project);
        Mockito.when(user.getProjectsMemberOf()).thenReturn(projects);

        Mockito.when(userService.getUser("demo")).thenReturn(user);
        assertNull(userService.getUser("demo", PASSWORD));
    }

    @Test
    public void getUser_with_username_and_password_should_return_user_with_valid_password_and_member_of_project_at_domain() {
        Project project = new Project.ProjectBuilder("PROJ", "Test project", "n/a", "http://localhost/")
                .build();
        Set<Project> projects = new HashSet<>();
        projects.add(project);
        Mockito.when(user.getProjectsMemberOf()).thenReturn(projects);

        Mockito.when(userService.getUser("demo")).thenReturn(user);
        assertEquals(user, userService.getUser("demo", PASSWORD));
    }


}