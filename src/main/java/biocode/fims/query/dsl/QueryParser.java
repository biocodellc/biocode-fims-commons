package biocode.fims.query.dsl;

import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.query.QueryBuildingExpressionVisitor;
import org.parboiled.*;
import org.parboiled.support.StringVar;

import static biocode.fims.query.dsl.LogicalOperator.AND;
import static biocode.fims.query.dsl.LogicalOperator.OR;

/**
 * @author rjewing
 */
@SuppressWarnings({"InfiniteRecursion", "WeakerAccess"})
public class QueryParser extends BaseParser<Object> {
    private final QueryBuildingExpressionVisitor queryBuilder;
    private final ProjectConfig projectConfig;

    public QueryParser(QueryBuildingExpressionVisitor queryBuilder, ProjectConfig config) {
        this.queryBuilder = queryBuilder;
        this.projectConfig = config;
    }

    public Rule Parse() {
        return Sequence(
                Optional(TopExpression()),
                EOI,
                new Action() {
                    @Override
                    public boolean run(Context context) {
                        if (context.getValueStack().isEmpty())
                            push(new Query(queryBuilder, projectConfig, new EmptyExpression()));
                        else push(new Query(queryBuilder, projectConfig, popExp()));
                        return context.getValueStack().size() == 1;
                    }
                }
        );
    }

    Rule TopExpression() {
        return FirstOf(
                Sequence(
                        WhiteSpace(),
                        Ch('*'),
                        push(new AllExpression()),
                        WhiteSpace()
                ),
                Sequence(
                        WhiteSpace(),
                        SubExpression(),
                        ZeroOrMore(
                                WhiteSpaceChars(),
                                OrChars(),
                                WhiteSpaceChars(),
                                SubExpression(),
                                push(new LogicalExpression(OR, popExp(1), popExp()))
                        ),
                        WhiteSpace()
                )
        );
    }

    Rule SubExpression() {
        return Sequence(
                Expression(),
                ZeroOrMore(
                        WhiteSpaceChars(),
                        AndChars(),
                        WhiteSpaceChars(),
                        Expression(),
                        push(new LogicalExpression(AND, popExp(1), popExp()))
                )
        );
    }

    Rule Expression() {
        return FirstOf(
                NotExpression(),
                FilterExpression(),
                ExistsExpression(),
                ExpeditionsExpression(),
                SelectExpression(),
                FTSExpression(),
                ExpressionGroup()
        );
    }

    Rule NotExpression() {
        return Sequence(
                WhiteSpace(),
                NotChars(),
                WhiteSpaceChars(),
                TestNot(NotExpression()),
                Expression(),
                push(new NotExpression(popExp()))
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
                WhiteSpace(),
                SpecialPrefixExpression(ExistsChars()),
                push(new ExistsExpression(popStr()))
        );
    }

    Rule ExpeditionsExpression() {
        return Sequence(
                WhiteSpace(),
                SpecialPrefixExpression(ExpeditionsChars()),
                push(new ExpeditionExpression(popStr()))
        );
    }

    Rule SelectExpression() {
        return Sequence(
                WhiteSpace(),
                SpecialPrefixExpression(SelectChars()),
                push(new SelectExpression(popStr()))
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
                WhiteSpace(),
                Optional(
                        Chars(),
                        WhiteSpace(),
                        SemiColon(),
                        column.set(popStr())

                ),
                WhiteSpace(),
                Chars(),
                push(new FTSExpression(column.get(), popStr()))
        );
    }

    Rule ExpressionGroup() {
        return Sequence(
                WhiteSpace(),
                OpenParen(),
                WhiteSpace(),
                TopExpression(),
                WhiteSpace(),
                CloseParen(),
                push(new GroupExpression(popExp()))
        );
    }

    Rule PhraseComparisonExpression() {
        return Sequence(
                WhiteSpace(),
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
                push(new ComparisonExpression(popStr(2), popStr(), ComparisonOperator.fromOp(popStr())))
        );
    }

    Rule ComparisonExpression() {
        return Sequence(
                WhiteSpace(),
                ComparisonOperator(),
                WhiteSpace(),
                Chars(),
                push(new ComparisonExpression(popStr(2), popStr(), ComparisonOperator.fromOp(popStr())))
        );
    }

    Rule LikeExpression() {
        return Sequence(
                WhiteSpace(),
                LikeChar(),
                WhiteSpace(),
                Phrase(),
                push(new LikeExpression(popStr(1), popStr()))
        );
    }

    Rule PhraseExpression() {
        return Sequence(
                WhiteSpace(),
                SemiColon(),
                WhiteSpace(),
                Phrase(),
                push(new LikeExpression(popStr(1), "%" + popStr() + "%"))
        );
    }

    Rule RangeExpression() {
        return Sequence(
                WhiteSpace(),
                SemiColon(),
                WhiteSpace(),
                Range(),
                push(new RangeExpression(popStr(1), popStr()))
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
                SelectChars(),
                QuoteChar(),
                OpenParen(),
                CloseParen(),
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

    Rule SelectChars() {
        return String("_select_");
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

    Rule NotChars() {
        return IgnoreCase("not");
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
