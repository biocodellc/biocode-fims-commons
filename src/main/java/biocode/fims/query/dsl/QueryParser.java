package biocode.fims.query.dsl;

import biocode.fims.query.QueryBuildingExpressionVisitor;
import org.parboiled.Action;
import org.parboiled.BaseParser;
import org.parboiled.Context;
import org.parboiled.Rule;
import org.parboiled.support.StringVar;

import static biocode.fims.query.dsl.LogicalOperator.AND;
import static biocode.fims.query.dsl.LogicalOperator.OR;

/**
 * @author rjewing
 */
@SuppressWarnings("InfiniteRecursion")
public class QueryParser extends BaseParser<Object> {
    private final QueryBuildingExpressionVisitor queryBuilder;

    public QueryParser(QueryBuildingExpressionVisitor queryBuilder) {
        this.queryBuilder = queryBuilder;
    }

    public Rule Parse() {
        return Sequence(
                Optional(TopExpression()),
                EOI,
                new Action() {
                    @Override
                    public boolean run(Context context) {
                        if (context.getValueStack().isEmpty()) push(new Query(queryBuilder, new EmptyExpression()));
                        else push(new Query(queryBuilder, popExp()));
                        return context.getValueStack().size() == 1;
                    }
                }
        );
    }

    Rule TopExpression() {
        return Sequence(
                WhiteSpace(),
                SubExpression(),
                ZeroOrMore(
                        WhiteSpace(),
                        OrChars(),
                        WhiteSpace(),
                        SubExpression(),
                        push(new LogicalExpression(OR, popExp(1), popExp()))
                )
        );
    }

    Rule SubExpression() {
        return Sequence(
                Expression(),
                ZeroOrMore(
                        WhiteSpace(),
                        AndChars(),
                        WhiteSpace(),
                        Expression(),
                        push(new LogicalExpression(AND, popExp(1), popExp()))
                )
        );
    }

    Rule Expression() {
        return FirstOf(
                FilterExpression(),
                ExistsExpression(),
                ExpeditionsExpression(),
                FTSExpression(),
                ExpressionGroup()
        );
    }

    Rule FilterExpression() {
        return Sequence(
                Chars(),
                WhiteSpace(),
                FirstOf(
                        PhraseComparisonExpression(),
                        ComparisonExpression(),
                        LikeExpression(),
                        PhraseExpression(),
                        RangeExpression()
                )
        );
    }

    Rule ExistsExpression() {
        return Sequence(
                SpecialPrefixExpression(ExistsChars()),
                push(new ExistsExpression(popStr())),
                WhiteSpace()
        );
    }

    Rule ExpeditionsExpression() {
        return Sequence(
                SpecialPrefixExpression(ExpeditionsChars()),
                push(new ExpeditionExpression(popStr())),
                WhiteSpace()
        );
    }

    Rule SpecialPrefixExpression(Rule prefix) {
        return Sequence(
                prefix,
                WhiteSpace(),
                SemiColon(),
                WhiteSpace(),
                FirstOf(
                        Sequence(
                                OpenBracket(),
                                WhiteSpace(),
                                PhraseChars(CloseBracket(), false),
                                WhiteSpace(),
                                CloseBracket()
                        ),
                        Chars()
                )
        );
    }

    Rule FTSExpression() {
        StringVar column = new StringVar();
        return Sequence(
                Optional(
                        Chars(),
                        WhiteSpace(),
                        SemiColon(),
                        column.set(popStr())

                ),
                WhiteSpace(),
                Chars(),
                push(new FTSExpression(column.get(), popStr())),
                WhiteSpace()
        );
    }

    Rule ExpressionGroup() {
        return Sequence(
                OpenParen(),
                WhiteSpace(),
                TopExpression(),
                WhiteSpace(),
                CloseParen(),
                push(new GroupExpression(popExp())),
                WhiteSpace()
        );
    }

    Rule PhraseComparisonExpression() {
        return Sequence(
                ComparisonOperator(),
                new Action() {
                    @Override
                    public boolean run(Context context) {
                        ComparisonOperator op = ComparisonOperator.fromOp((String) peek());
                        return op.equals(ComparisonOperator.EQUALS) || op.equals(ComparisonOperator.NOT_EQUALS);
                    }
                },
                WhiteSpace(),
                Phrase(),
                push(new ComparisonExpression(popStr(2), popStr(), ComparisonOperator.fromOp(popStr()))),
                WhiteSpace()
        );
    }

    Rule ComparisonExpression() {
        return Sequence(
                ComparisonOperator(),
                WhiteSpace(),
                Chars(),
                push(new ComparisonExpression(popStr(2), popStr(), ComparisonOperator.fromOp(popStr()))),
                WhiteSpace()
        );
    }

    Rule LikeExpression() {
        return Sequence(
                LikeChar(),
                WhiteSpace(),
                Phrase(),
                push(new LikeExpression(popStr(1), popStr())),
                WhiteSpace()
        );
    }

    Rule PhraseExpression() {
        return Sequence(
                SemiColon(),
                WhiteSpace(),
                Phrase(),
                push(new LikeExpression(popStr(1), "%" + popStr() + "%")),
                WhiteSpace()
        );
    }

    Rule RangeExpression() {
        return Sequence(
                SemiColon(),
                WhiteSpace(),
                Range(),
                push(new RangeExpression(popStr(1), popStr())),
                WhiteSpace()
        );
    }

    //***** Helper Rules for Consuming Text *****//

    Rule Phrase() {
        return Sequence(
                QuoteChar(),
                PhraseChars(QuoteChar(), true),
                QuoteChar()
        );
    }

    Rule Range() {
        StringVar range = new StringVar("");
        return Sequence(
                OpenRange(),
                range.append(match()),
                PhraseChars(CloseRange(), false),
                range.append((String) pop()),
                CloseRange(),
                range.append(match()),
                push(range.get())
        );
    }

    Rule ComparisonOperator() {
        return Sequence(
                ComparisonOperatorChars(),
                push(match())
        );
    }

    Rule PhraseChars(Rule delimChar, boolean unEscape) {
        StringVar text = new StringVar("");
        return Sequence(
                OneOrMore(
                        FirstOf(
                                (unEscape) ? AppendEscapedReservedChars(text) : NOTHING,
                                TestNot(delimChar)
                        ),
                        ANY,
                        text.append(match())
                ),
                push(text.get())
        );
    }

    Rule Chars() {
        StringVar text = new StringVar("");

        return Sequence(
                OneOrMore(
                        FirstOf(
                                AppendEscapedReservedChars(text),
                                Sequence(
                                        TestNot(WhiteSpaceChars()),
                                        TestNot(ReservedChars())
                                )
                        ),
                        ANY,
                        text.append(match())
                ),
                push(text.get())
        );
    }

    Rule AppendEscapedReservedChars(StringVar text) {
        return Sequence(
                EscapeChar(),
                ReservedChars(),
                text.append(match().replaceFirst("\\\\", ""))
        );
    }

    Rule ReservedChars() {
        return FirstOf(
                ComparisonOperatorChars(),
                SemiColon(),
                ExpeditionsChars(),
                QuoteChar(),
                OpenParen(),
                CloseParen(),
                AndChars(),
                OrChars(),
                ExistsChars(),
                OpenBracket(),
                CloseBracket(),
                LikeChar(),
                OpenRange(),
                CloseRange()
        );
    }

    Rule WhiteSpace() {
        return ZeroOrMore(WhiteSpaceChars());
    }

    //***** Special Character(s) *****//

    Rule ComparisonOperatorChars() {
        return FirstOf(
                "<=",
                ">=",
                "<>",
                Ch('<'),
                Ch('>'),
                Ch('=')
        );
    }

    Rule EscapeChar() {
        return Ch('\\');
    }

    Rule ExpeditionsChars() {
        return String("_expeditions_");
    }

    Rule ExistsChars() {
        return String("_exists_");
    }

    Rule OpenParen() {
        return Ch('(');
    }

    Rule CloseParen() {
        return Ch(')');
    }

    Rule OpenBracket() {
        return Ch('[');
    }

    Rule CloseBracket() {
        return Ch(']');
    }

    Rule LikeChar() {
        return String("::");
    }

    Rule OpenRange() {
        return AnyOf("[{");
    }

    Rule CloseRange() {
        return AnyOf("]}");
    }

    Rule SemiColon() {
        return Ch(':');
    }

    Rule QuoteChar() {
        return Ch('"');
    }

    Rule OrChars() {
        return IgnoreCase("or");
    }

    Rule AndChars() {
        return IgnoreCase("AND");
    }

    Rule WhiteSpaceChars() {
        return AnyOf(" \t\f");
    }


    //***** Helper functions *****//

    Expression popExp() {
        return (Expression) pop();
    }

    Expression popExp(int down) {
        return (Expression) pop(down);
    }

    String popStr() {
        return (String) pop();
    }

    String popStr(int down) {
        return (String) pop(down);
    }
}
