package biocode.fims.query;

import biocode.fims.query.dsl.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Visitor to collect all entities from SelectExpressions
 * @author rjewing
 */
public class EntityCollectingExpressionVisitor implements ExpressionVisitor {
    private Set<String> entities;

    public EntityCollectingExpressionVisitor() {
        this.entities= new HashSet<>();
    }

    public Set<String> entities() {
        return entities;
    }

    @Override
    public void visit(ComparisonExpression expression) {
    }

    @Override
    public void visit(ExistsExpression existsExpression) {
    }

    @Override
    public void visit(ExpeditionExpression expeditionExpression) {
    }

    @Override
    public void visit(SelectExpression selectExpression) {
        entities.addAll(selectExpression.entites());
    }

    @Override
    public void visit(FTSExpression ftsExpression) {
    }

    @Override
    public void visit(LikeExpression likeExpression) {
    }

    @Override
    public void visit(LogicalExpression logicalExpression) {
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
