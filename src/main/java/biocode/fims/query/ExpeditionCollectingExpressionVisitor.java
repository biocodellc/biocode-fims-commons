package biocode.fims.query;

import biocode.fims.query.dsl.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Visitor to collect all unique expeditionCodes from Expressions
 * @author rjewing
 */
public class ExpeditionCollectingExpressionVisitor implements ExpressionVisitor {
    private Set<String> expeditions;

    public ExpeditionCollectingExpressionVisitor() {
        this.expeditions = new HashSet<>();
    }

    public Set<String> expeditions() {
        return expeditions;
    }

    @Override
    public void visit(ComparisonExpression expression) {
    }

    @Override
    public void visit(ExistsExpression existsExpression) {
    }

    @Override
    public void visit(ExpeditionExpression expeditionExpression) {
        expeditions.addAll(expeditionExpression.expeditions());
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
