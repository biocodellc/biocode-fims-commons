package biocode.fims.models;

import biocode.fims.serializers.JsonViewOverride;
import biocode.fims.serializers.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.List;

/**
 * TemplateConfig Entity object
 */
@Entity
@Table(name = "project_templates")
public class ProjectTemplate {

    private Integer id;
    private String name;
    private String sheetName;
    private List<String> attributeUris;
    private Project project;
    private User user;

    ProjectTemplate() {
    }

    public ProjectTemplate(String name, List<String> attributeUris, String sheetName, Project project, User user) {
        this.name = name;
        this.attributeUris = attributeUris;
        this.sheetName = sheetName;
        this.project = project;
        this.user = user;
    }

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @JsonView(Views.Summary.class)
    @Column(nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonView(Views.Summary.class)
    @Column(nullable = false, name = "sheet_name")
    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    @JsonView(Views.Detailed.class)
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb", name = "attribute_uris")
    public List<String> getAttributeUris() {
        return attributeUris;
    }

    public void setAttributeUris(List<String> attributeUris) {
        this.attributeUris = attributeUris;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectTemplate)) return false;

        ProjectTemplate that = (ProjectTemplate) o;

        if (!getName().equals(that.getName())) return false;
        return getProject().equals(that.getProject());

    }

    @Override
    public int hashCode() {
        int result = getName().hashCode();
        result = 31 * result + getProject().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TemplateConfig{" +
                "name='" + name + '\'' +
                ", config='" + attributeUris + '\'' +
                ", project=" + project +
                ", user=" + user +
                '}';
    }

    @MapsId("")
    @JsonView(Views.Detailed.class)
    @JsonViewOverride(Views.Summary.class)
    @ManyToOne
    @JoinColumn(name = "project_id",
            referencedColumnName = "id",
            nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_project_templates_project_id")
    )
    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    @JsonView(Views.Detailed.class)
    @JsonViewOverride(Views.Summary.class)
    @ManyToOne
    @JoinColumn(name = "user_id",
            referencedColumnName = "id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_project_templates_user_id")
    )
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
