package biocode.fims.rest;

import biocode.fims.models.User;


/**
 * Bean for holding the User for each REST request
 * @author RJ Ewing
 */
public class UserContext {
    private User user = null;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
