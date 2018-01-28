package biocode.fims.models;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.serializers.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import org.apache.commons.lang.StringUtils;

import javax.persistence.*;
import java.util.ArrayList;
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
                attributeNodes = @NamedAttributeNode("projects")),
        @NamedEntityGraph(name = "User.withProjectsAndProjectsMemberOf",
                attributeNodes = {@NamedAttributeNode("projects"), @NamedAttributeNode("projectsMemberOf")})
})
@Entity
@Table(name = "users")
public class User {

    private int userId;
    private String username;
    private String password;
    private boolean hasSetPassword;
    private String email;
    private boolean admin;
    private String institution;
    private String firstName;
    private String lastName;
    private String passwordResetToken;
    private Date passwordResetExpiration;
    private Set<Expedition> expeditions;
    private Set<Project> projects;
    private List<Project> projectsMemberOf;
    private Set<ProjectTemplate> projectTemplates;

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

        private boolean validUser() {
            if (StringUtils.isBlank(email) || StringUtils.isBlank(institution) || StringUtils.isBlank(firstName) || StringUtils.isBlank(lastName))
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
        projectsMemberOf = new ArrayList<>();
    }

    // needed for hibernate
    User() {
        projectsMemberOf = new ArrayList<>();
    }

    /**
     * method to transfer the updated {@link User} object to this existing {@link User}. This
     * allows us to control which properties can be updated.
     * Currently allows updating of the following properties : firstName, lastName, email, and institution
     *
     * @param user
     */
    public void update(User user) {
        this.setFirstName(user.getFirstName());
        this.setLastName(user.getLastName());
        this.setEmail(user.getEmail());
        this.setInstitution(user.getInstitution());
    }

    public boolean isValid(boolean requirePassword) {
        return !StringUtils.isEmpty(this.getUsername()) &&
                (!StringUtils.isEmpty(this.getPassword()) || !requirePassword) &&
                !StringUtils.isEmpty(this.getFirstName()) &&
                !StringUtils.isEmpty(this.getLastName()) &&
                !StringUtils.isEmpty(this.getEmail());
    }

    @Column(name = "id")
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
    @Column(nullable = false, unique = true, updatable = false)
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @JsonView(Views.Detailed.class)
    @Column(nullable = false, name = "has_set_password")
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

    @Transient
    @JsonProperty("projectAdmin")
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
    @Column(nullable = false, name = "first_name")
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @JsonView(Views.Detailed.class)
    @Column(nullable = false, name = "last_name")
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
    @Column(columnDefinition = "char(20) null", name = "password_reset_token")
    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    public void setPasswordResetToken(String passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }

    @JsonIgnore
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "password_reset_expiration")
    public Date getPasswordResetExpiration() {
        return passwordResetExpiration;
    }

    public void setPasswordResetExpiration(Date passwordResetExpiration) {
        this.passwordResetExpiration = passwordResetExpiration;
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
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "user_projects",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "project_id", referencedColumnName = "id"),
            foreignKey = @ForeignKey(name = "FK_user_projects_user_id"),
            inverseForeignKey = @ForeignKey(name = "FK_user_projects_project_id")
    )
    public List<Project> getProjectsMemberOf() {
        return projectsMemberOf;
    }

    private void setProjectsMemberOf(List<Project> projectsMemberOf) {
        this.projectsMemberOf = projectsMemberOf;
    }

    @JsonIgnore
    @OneToMany(mappedBy = "user")
    public Set<ProjectTemplate> getProjectTemplates() {
        return projectTemplates;
    }

    private void setProjectTemplates(Set<ProjectTemplate> projectTemplates) {
        this.projectTemplates = projectTemplates;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;

        User that = (User) o;
        return getUsername().equals(that.getUsername());
    }

    @Override
    public int hashCode() {
        return getUsername().hashCode();
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", getHasSetPassword=" + hasSetPassword +
                ", email='" + email + '\'' +
                ", institution='" + institution + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", passwordResetToken='" + passwordResetToken + '\'' +
                ", passwordResetExpiration=" + passwordResetExpiration +
                '}';
    }
}