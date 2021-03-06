package biocode.fims.models;

import biocode.fims.serializers.JsonViewOverride;
import biocode.fims.serializers.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.*;

/**
 * TemplateConfig Entity object
 */
@Entity
@Table(name = "worksheet_templates")
public class WorksheetTemplate {

    private Integer id;
    private String name;
    private String worksheet;
    private List<String> columns;
    private Project project;
    private User user;

    WorksheetTemplate() {
    }

    public WorksheetTemplate(String name, List<String> columns, String worksheet, Project project, User user) {
        this.name = name;
        this.columns = columns;
        this.worksheet = worksheet;
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
    @Column(nullable = false, name = "worksheet")
    public String getWorksheet() {
        return worksheet;
    }

    public void setWorksheet(String worksheet) {
        this.worksheet = worksheet;
    }

    @JsonView(Views.Detailed.class)
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb", name = "columns")
    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        Set<String> cols = new HashSet<>(columns);
        this.columns = new ArrayList<>(cols);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorksheetTemplate)) return false;

        WorksheetTemplate that = (WorksheetTemplate) o;

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
        return "WorksheetTemplate{" +
                "name='" + name + '\'' +
                ", worksheet='" + worksheet + '\'' +
                ", columns=" + columns +
                ", project=" + project +
                ", user=" + user +
                '}';
    }

    //    @MapsId()
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
