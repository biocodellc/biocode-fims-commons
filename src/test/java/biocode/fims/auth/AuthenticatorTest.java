package biocode.fims.auth;

import org.junit.Test;

import static org.junit.Assert.*;

public class AuthenticatorTest {

    private Authenticator authenticator = new TestableAuthenticator();
    private String PASSWORD_TO_HASH = "password";

    @Test
    public void should_return_false_with_wrong_password() {
        assertFalse(authenticator.login("demo", "wrongPassword"));
    }

    @Test
    public void should_return_false_with_empty_password() {
        assertFalse(authenticator.login("demo", ""));
    }

    @Test
    public void should_return_true_with_valid_password() {
        assertTrue(authenticator.login("demo", "password"));
    }

    private class TestableAuthenticator extends Authenticator {
        @Override
        protected void getSettingsManager() {}

        @Override
        protected String getHashedPass(String username) {
            try {
                return PasswordHash.createHash(PASSWORD_TO_HASH);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}