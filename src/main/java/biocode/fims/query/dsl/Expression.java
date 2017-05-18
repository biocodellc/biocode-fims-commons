package biocode.fims.query.dsl;

import biocode.fims.query.ExpressionVisitor;

/**
 * @author rjewing
 */
public interface Expression {
    void accept(ExpressionVisitor visitor);
}
