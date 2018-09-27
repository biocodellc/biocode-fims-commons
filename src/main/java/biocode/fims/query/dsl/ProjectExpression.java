package biocode.fims.query.dsl;

import biocode.fims.query.ExpressionVisitor;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Project Search Expression
 * <p>
 * _projects_:1         ->  all results in project 1
 * _projects:[1, 2]     ->  all results in either project 1 or 2
 *
 * @author rjewing
 */
public class ProjectExpression implements Expression {
    private List<Integer> projects;

    public ProjectExpression(List<Integer> projects) {
        Assert.notEmpty(projects, "projects must contain at least 1 value");
        this.projects = projects;
    }

    public List<Integer> projects() {
        return projects;
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectExpression)) return false;

        ProjectExpression that = (ProjectExpression) o;

        return projects.equals(that.projects);
    }

    @Override
    public int hashCode() {
        return projects.hashCode();
    }

    @Override
    public String toString() {
        return "ProjectExpression{" +
                "projects=" + projects +
                '}';
    }
}

