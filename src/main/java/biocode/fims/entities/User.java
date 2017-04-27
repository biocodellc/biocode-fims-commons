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
                attributeNodes = @NamedAttributeNode("projects")),
        @NamedEntityGraph(name = "User.withProjectsAndProjectsMemberOf",
                attributeNodes = {@NamedAttributeNode("projects"), @NamedAttributeNode("projectsMemberOf")})
})
@Entity
@Table(name = "users")
public class User {

    private int id;
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
    }

    // needed for hibernate
    User() {
    }

    @JsonView(Views.Summary.class)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getId() {
        return id;
    }

    private void setId(int id) {
        this.id = id;
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
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "userProjects",
            joinColumns = @JoinColumn(name = "userId", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "projectId", referencedColumnName = "id"),
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
                "id=" + id +
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
