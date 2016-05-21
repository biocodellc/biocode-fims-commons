package biocode.fims.service;

import biocode.fims.auth.PasswordHash;
import biocode.fims.entities.User;
import biocode.fims.repositories.UserRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;

/**
 * Tests for {@link UserService}
 */
public class UserServiceTest {
    private String INVALID_PASSWORD = "password";
    private String PASSWORD = "correct";
    private User user;

    @InjectMocks
    private UserService userService;
    @Mock
    private UserRepository userRepository;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        user = new User.UserBuilder("demo", PasswordHash.createHash(PASSWORD))
                .name("test", "account")
                .email("test@example.com")
                .institution("biocode")
                .build();
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
    public void getUser_with_username_and_password_should_return_user_with_valid_password() {
        Mockito.when(userService.getUser("demo")).thenReturn(user);
        assertEquals(user, userService.getUser("demo", PASSWORD));
    }


}