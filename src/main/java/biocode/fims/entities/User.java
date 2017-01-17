package biocode.fims.entities;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.serializers.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.util.StringUtils;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * User entity object
 */
@NamedEntityGraphs({
        @NamedEntityGraph(name = "User.withProjectsMemberOf",
                attributeNodes = @NamedAttributeNode("projectsMemberOf")),
        @NamedEntityGraph(name = "User.withProjects",
                attributeNodes = @NamedAttributeNode("projects"))
})
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
    private List<Project> projectsMemberOf;
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

        private boolean validUser() {
            if (StringUtils.isEmpty(email) || StringUtils.isEmpty(institution) || StringUtils.isEmpty(firstName) || StringUtils.isEmpty(lastName))
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
    }

    // needed for hibernate
    User() {
    }

    @JsonView(Views.Summary.class)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getUserId() {
        return userId;
    }

    private void setUserId(int id) {
        this.userId = id;
    }

    @JsonView(Views.Summary.class)
    @Column(nullable = false, unique = true)
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

    @JsonView(Views.Detailed.class)
    @Column(nullable = false)
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @JsonView(Views.Detailed.class)
    @Column(nullable = false)
    public boolean getHasSetPassword() {
        return hasSetPassword;
    }

    public void setHasSetPassword(boolean hasSetPassword) {
        this.hasSetPassword = hasSetPassword;
    }

    @JsonView(Views.Detailed.class)
    @Column(nullable = false)
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @JsonIgnore // we aren't using this property maybe in the future?
    @JsonProperty("projectAdmin")
    @Column(nullable = false)
    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    @JsonView(Views.Detailed.class)
    @Column(nullable = false)
    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    @JsonView(Views.Detailed.class)
    @Column(nullable = false)
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @JsonView(Views.Detailed.class)
    @Column(nullable = false)
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @JsonIgnore
    @Transient
    public String getFullName() {
        return firstName + " " + lastName;
    }

    @JsonIgnore
    @Column(columnDefinition = "char(20) null")
    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    public void setPasswordResetToken(String passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }

    @JsonIgnore
    @Temporal(TemporalType.TIMESTAMP)
    public Date getPasswordResetExpiration() {
        return passwordResetExpiration;
    }

    public void setPasswordResetExpiration(Date passwordResetExpiration) {
        this.passwordResetExpiration = passwordResetExpiration;
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
    public List<Project> getProjectsMemberOf() {
        return projectsMemberOf;
    }

    private void setProjectsMemberOf(List<Project> projectsMemberOf) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;

        User user = (User) o;

        if (this.getUserId() != 0 && user.getUserId() != 0)
            return this.getUserId() == user.getUserId();

        if (isEnabled() != user.isEnabled()) return false;
        if (getHasSetPassword() != user.getHasSetPassword()) return false;
        if (isAdmin() != user.isAdmin()) return false;
        if (!getUsername().equals(user.getUsername())) return false;
        if (!getPassword().equals(user.getPassword())) return false;
        if (getEmail() != null ? !getEmail().equals(user.getEmail()) : user.getEmail() != null) return false;
        if (getInstitution() != null ? !getInstitution().equals(user.getInstitution()) : user.getInstitution() != null)
            return false;
        if (getFirstName() != null ? !getFirstName().equals(user.getFirstName()) : user.getFirstName() != null)
            return false;
        if (getLastName() != null ? !getLastName().equals(user.getLastName()) : user.getLastName() != null)
            return false;
        if (getPasswordResetToken() != null ? !getPasswordResetToken().equals(user.getPasswordResetToken()) : user.getPasswordResetToken() != null)
            return false;
        return getPasswordResetExpiration() != null ? getPasswordResetExpiration().equals(user.getPasswordResetExpiration()) : user.getPasswordResetExpiration() == null;

    }

    @Override
    public int hashCode() {
        int result = getUsername().hashCode();
        result = 31 * result + getPassword().hashCode();
        result = 31 * result + (isEnabled() ? 1 : 0);
        result = 31 * result + (getHasSetPassword() ? 1 : 0);
        result = 31 * result + (getEmail() != null ? getEmail().hashCode() : 0);
        result = 31 * result + (isAdmin() ? 1 : 0);
        result = 31 * result + (getInstitution() != null ? getInstitution().hashCode() : 0);
        result = 31 * result + (getFirstName() != null ? getFirstName().hashCode() : 0);
        result = 31 * result + (getLastName() != null ? getLastName().hashCode() : 0);
        result = 31 * result + (getPasswordResetToken() != null ? getPasswordResetToken().hashCode() : 0);
        result = 31 * result + (getPasswordResetExpiration() != null ? getPasswordResetExpiration().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", enabled=" + enabled +
                ", getHasSetPassword=" + hasSetPassword +
                ", email='" + email + '\'' +
                ", admin=" + admin +
                ", institution='" + institution + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", passwordResetToken='" + passwordResetToken + '\'' +
                ", passwordResetExpiration=" + passwordResetExpiration +
                '}';
    }
}
