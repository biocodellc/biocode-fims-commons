package biocode.fims.query;

import biocode.fims.query.dsl.*;

/**
 * Visitor Pattern interface for {@link Expression} implementations
 *
 * https://en.wikipedia.org/wiki/Visitor_pattern
 *
 * @author rjewing
 */
public interface ExpressionVisitor {

    void visit(ComparisonExpression expression);

    void visit(ExistsExpression existsExpression);

    void visit(ExpeditionExpression expeditionExpression);

    void visit(FTSExpression ftsExpression);

    void visit(LikeExpression likeExpression);

    void visit(LogicalExpression logicalExpression);

    void visit(PhraseExpression phraseExpression);

    void visit(RangeExpression rangeExpression);

    void visit(EmptyExpression emptyExpression);
}
