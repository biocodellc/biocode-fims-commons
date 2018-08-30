package biocode.fims.query;

import biocode.fims.query.dsl.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Visitor to collect all unique expeditionCodes from Expressions
 *
 * @author rjewing
 */
public class ProjectCollectingExpressionVisitor implements ExpressionVisitor {
    private Set<Integer> projects;

    public ProjectCollectingExpressionVisitor() {
        this.projects = new HashSet<>();
    }

    public List<Integer> projects() {
        return new ArrayList<>(projects);
    }

    @Override
    public void visit(ComparisonExpression expression) {
    }

    @Override
    public void visit(ExistsExpression existsExpression) {
    }

    @Override
    public void visit(ProjectExpression projectExpression) {
        projects.addAll(projectExpression.projects());
    }

    @Override
    public void visit(ExpeditionExpression expeditionExpression) {
    }

    @Override
    public void visit(SelectExpression selectExpression) {
        if (selectExpression.expression() != null) selectExpression.expression().accept(this);
    }

    @Override
    public void visit(FTSExpression ftsExpression) {
    }

    @Override
    public void visit(LikeExpression likeExpression) {
    }

    @Override
    public void visit(LogicalExpression logicalExpression) {
        logicalExpression.left().accept(this);
        logicalExpression.right().accept(this);
    }

    @Override
    public void visit(RangeExpression rangeExpression) {
    }

    @Override
    public void visit(EmptyExpression emptyExpression) {
    }

    @Override
    public void visit(GroupExpression groupExpression) {
    }

    @Override
    public void visit(NotExpression notExpression) {
    }

    @Override
    public void visit(AllExpression allExpression) {
    }
}
