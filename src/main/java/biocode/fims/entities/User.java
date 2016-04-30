package biocode.fims.entities;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

/**
 * User entity object
 */
@Entity
@Table(name = "users")
public class User {

    private int userId;
    private String username;
    private String password;
    private boolean enabled;
    private boolean hasSetPassword;
    private String email;
    private boolean admin;
    private String institution;
    private String firstName;
    private String lastName;
    private String passwordResetToken;
    private Date passwordResetExpiration;
    private Set<Bcid> bcids;
    private Set<Expedition> expeditions;
    private Set<Project> projects;
    private Set<Project> projectsMemberOf;
    private Set<TemplateConfig> templateConfigs;

    public static class UserBuilder {
        // Required
        private String username;
        private String password;
        private String email;
        private String institution;
        private String firstName;
        private String lastName;

        // Optional
        private boolean hasSetPassword = false;
        private boolean enabled = true;
        private boolean admin = false;
        private String passwordResetToken;
        private Date passwordResetExpiration;

        public UserBuilder(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder institution(String institution) {
            this.institution = institution;
            return this;
        }

        public UserBuilder name(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
            return this;
        }

        public UserBuilder hasSetPassword(boolean hasSetPassword) {
            this.hasSetPassword = hasSetPassword;
            return this;
        }

        public UserBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public UserBuilder admin(boolean admin) {
            this.admin = admin;
            return this;
        }

        public UserBuilder passwordResetToken(String passwordResetToken) {
            this.passwordResetToken = passwordResetToken;
            return this;
        }

        public UserBuilder passwordResetExpiration(Date passwordResetExpiration) {
            this.passwordResetExpiration = passwordResetExpiration;
            return this;
        }

        private boolean validUser() {
            if (email == null || institution == null || firstName == null || lastName == null)
                return false;

            return true;
        }

        public User build() {
            if (!validUser())
                throw new FimsRuntimeException("", "Trying to create an invalid User. " +
                        "username, password, email, institution, firstName, and lastName are must not be null", 500);

            return new User(this);
        }
    }

    private User(UserBuilder builder) {
        username = builder.username;
        password = builder.password;
        email = builder.email;
        institution = builder.institution;
        firstName = builder.firstName;
        lastName = builder.lastName;
        hasSetPassword = builder.hasSetPassword;
        enabled = builder.enabled;
        admin = builder.admin;
        passwordResetToken = builder.passwordResetToken;
        passwordResetExpiration = builder.passwordResetExpiration;
    }

    // needed for hibernate
    private User() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getUserId() {
        return userId;
    }

    private void setUserId(int id) {
        this.userId = id;
    }

    @Column(nullable = false)
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @JsonIgnore
    @Column(nullable = false)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Column(nullable = false)
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Column(nullable = false)
    public boolean isHasSetPassword() {
        return hasSetPassword;
    }

    public void setHasSetPassword(boolean hasSetPassword) {
        this.hasSetPassword = hasSetPassword;
    }

    @Column(nullable = false)
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Column(nullable = false)
    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    @Column(nullable = false)
    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    @Column(nullable = false)
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Column(nullable = false)
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Transient
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    public void setPasswordResetToken(String passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getPasswordResetExpiration() {
        return passwordResetExpiration;
    }

    public void setPasswordResetExpiration(Date passwordResetExpiration) {
        this.passwordResetExpiration = passwordResetExpiration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (userId != user.userId) return false;
        if (enabled != user.enabled) return false;
        if (hasSetPassword != user.hasSetPassword) return false;
        if (admin != user.admin) return false;
        if (username != null ? !username.equals(user.username) : user.username != null) return false;
        if (password != null ? !password.equals(user.password) : user.password != null) return false;
        if (email != null ? !email.equals(user.email) : user.email != null) return false;
        if (institution != null ? !institution.equals(user.institution) : user.institution != null) return false;
        if (firstName != null ? !firstName.equals(user.firstName) : user.firstName != null) return false;
        if (lastName != null ? !lastName.equals(user.lastName) : user.lastName != null) return false;
        if (passwordResetToken != null ? !passwordResetToken.equals(user.passwordResetToken) : user.passwordResetToken != null)
            return false;
        if (passwordResetExpiration != null ? !passwordResetExpiration.equals(user.passwordResetExpiration) : user.passwordResetExpiration != null)
            return false;

        return true;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", enabled=" + enabled +
                ", hasSetPassword=" + hasSetPassword +
                ", email='" + email + '\'' +
                ", admin=" + admin +
                ", institution='" + institution + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", passwordResetToken='" + passwordResetToken + '\'' +
                ", passwordResetExpiration=" + passwordResetExpiration +
                '}';
    }

    @Override
    public int hashCode() {
        int result = userId;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (enabled ? 1 : 0);
        result = 31 * result + (hasSetPassword ? 1 : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (admin ? 1 : 0);
        result = 31 * result + (institution != null ? institution.hashCode() : 0);
        result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
        result = 31 * result + (passwordResetToken != null ? passwordResetToken.hashCode() : 0);
        result = 31 * result + (passwordResetExpiration != null ? passwordResetExpiration.hashCode() : 0);
        return result;
    }

    @JsonIgnore
    @OneToMany(mappedBy = "user")
    public Set<Bcid> getBcids() {
        return bcids;
    }

    private void setBcids(Set<Bcid> bcids) {
        this.bcids = bcids;
    }

    @JsonIgnore
    @OneToMany(mappedBy = "user")
    public Set<Expedition> getExpeditions() {
        return expeditions;
    }

    private void setExpeditions(Set<Expedition> expeditions) {
        this.expeditions = expeditions;
    }

    @JsonIgnore
    @OneToMany(mappedBy = "user")
    public Set<Project> getProjects() {
        return projects;
    }

    private void setProjects(Set<Project> projects) {
        this.projects = projects;
    }

    @JsonIgnore
    @ManyToMany
    @JoinTable(name = "userProjects",
            joinColumns = @JoinColumn(name = "userId", referencedColumnName = "userId"),
            inverseJoinColumns = @JoinColumn(name = "projectId", referencedColumnName = "projectId"),
            foreignKey = @ForeignKey(name = "FK_userProjects_userId"),
            inverseForeignKey = @ForeignKey(name = "FK_userProjects_projectId")
    )
    public Set<Project> getProjectsMemberOf() {
        return projectsMemberOf;
    }

    private void setProjectsMemberOf(Set<Project> projectsMemberOf) {
        this.projectsMemberOf = projectsMemberOf;
    }

    @JsonIgnore
    @OneToMany(mappedBy = "user")
    public Set<TemplateConfig> getTemplateConfigs() {
        return templateConfigs;
    }

    private void setTemplateConfigs(Set<TemplateConfig> templateConfigs) {
        this.templateConfigs = templateConfigs;
    }
}
