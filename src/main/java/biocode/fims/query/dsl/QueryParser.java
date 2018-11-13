package biocode.fims.query.dsl;

import biocode.fims.config.Config;
import biocode.fims.query.QueryBuildingExpressionVisitor;
import org.parboiled.*;
import org.parboiled.support.StringVar;
import org.parboiled.support.ValueStack;
import org.parboiled.support.Var;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static biocode.fims.query.dsl.LogicalOperator.AND;
import static biocode.fims.query.dsl.LogicalOperator.OR;

/**
 * @author rjewing
 */
@SuppressWarnings({"InfiniteRecursion", "WeakerAccess"})
public class QueryParser extends BaseParser<Object> {
    private final QueryBuildingExpressionVisitor queryBuilder;
    private final Config config;

    public QueryParser(QueryBuildingExpressionVisitor queryBuilder, Config config) {
        this.queryBuilder = queryBuilder;
        this.config = config;
    }

    public Rule Parse() {
        return Sequence(
                Optional(TopExpression()),
                EOI,
                new Action() {
                    @Override
                    public boolean run(Context context) {
                        // Generate a Query object from the parser results
                        ValueStack stack = context.getValueStack();

                        if (stack.isEmpty()) {
                            push(new Query(queryBuilder, config, new EmptyExpression()));
                        } else if (stack.size() == 1) {
                            Expression expression = popExp();

                            if (expression instanceof SelectExpression) {
                                ((SelectExpression) expression).setExpression(new AllExpression());
                            }

                            push(new Query(queryBuilder, config, expression));
                        } else {
                            List<String> selectEntities = new ArrayList<>();
                            Expression expression = null;

                            for (Object o : stack) {
                                if (o instanceof SelectExpression) {
                                    selectEntities.addAll(((SelectExpression) o).entites());
                                } else {
                                    // We should only have 0 or 1 objects other the SelectExpressions in the stack.
                                    // If we have more, then there was an issue parsing
                                    if (expression != null) {
                                        return false;
                                    }
                                    expression = (Expression) o;
                                }
                            }

                            // clear the stack so we can rebuild
                            stack.clear();

                            if (selectEntities.size() > 0) {
                                Expression e;
                                if (expression != null) {
                                    // If we have 1 object other then SelectExpressions. This is the filter expression
                                    e = new SelectExpression(String.join(",", selectEntities), expression);
                                } else {
                                    // If we have no other objects, then we have an AllExpression
                                    e = new SelectExpression(String.join(",", selectEntities), new AllExpression());
                                }
                                push(new Query(queryBuilder, config, e));
                            }
                        }

                        return stack.size() == 1;
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
                                FirstOf(
                                        Sequence(
                                                new Action() {
                                                    @Override
                                                    public boolean run(Context context) {
                                                        return peek() instanceof SelectExpression;
                                                    }
                                                },
                                                SubExpression()
                                        ),
                                        Sequence(
                                                OrChars(),
                                                WhiteSpaceChars(),
                                                SubExpression(),
                                                logicalAction(OR)
                                        )
                                )
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
                        logicalAction(AND)
                )
        );
    }

    Rule Expression() {
        return Sequence(
                // SelectExpression doesn't need to be preceded by "and" or "or"
//                Optional(SelectExpression()),
                FirstOf(
                        NotExpression(),
                        FilterExpression(),
                        ExistsExpression(),
                        ProjectsExpression(),
                        ExpeditionsExpression(),
                        SelectExpression(),
                        FTSExpression(),
                        ExpressionGroup()
                ),
                // SelectExpression doesn't need to be preceded by "and" or "or"
                Optional(SelectExpression())
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
                WhiteSpace(),
                Column(),
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

    Rule ProjectsExpression() {
        return Sequence(
                WhiteSpace(),
                SpecialPrefixExpression(ProjectChars()),
                new Action() {
                    @Override
                    public boolean run(Context context) {
                        String val = popStr();

                        List<Integer> projects;
                        if (val.contains(",")) {
                            projects = Arrays.stream(val.replaceAll(" ", "").split(","))
                                    .map(Integer::parseInt)
                                    .collect(Collectors.toList());
                        } else {
                            projects = Collections.singletonList(Integer.parseInt(val.trim()));
                        }
                        return push(new ProjectExpression(projects));
                    }
                }
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
                new Action() {
                    @Override
                    public boolean run(Context context) {
                        String val = popStr();
                        // all SelectExpressions should be added to bottom of stack
                        return push(context.getValueStack().size(), new SelectExpression(val, null));
                    }
                },
                Optional(Expression())
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
        StringVar val = new StringVar();
        return Sequence(
                WhiteSpace(),
                Optional(
                        Column(),
                        WhiteSpace(),
                        SemiColon(),
                        column.set(popStr())
                ),
                WhiteSpace(),
                Chars(),
                val.set(popStr()),
                ZeroOrMore(
                        Test(column.isEmpty()), // don't parse multi-term fts if a column is specified
                        WhiteSpace(),
                        TestNot(AndChars()),
                        TestNot(OrChars()),
                        TestNot(NotChars()),
                        Chars(),
                        val.append(" "),
                        val.append(popStr())
                ),
                push(new FTSExpression(column.get(), val.get()))
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

    Rule Column() {
        StringVar text = new StringVar("");

        return Sequence(
                OneOrMore(
                        FirstOf(
                                AppendEscapedReservedChars(text),
                                Sequence(
                                        TestNot(DotChar()),
                                        TestNot(WhiteSpaceChars()),
                                        TestNot(ReservedChars())
                                )
                        ),
                        ANY,
                        text.append(match())
                ),
                Optional(
                        PathedColumn(),
                        text.append((String) pop())
                ),
                push(text.get())
        );
    }

    Rule PathedColumn() {
        Var<Boolean> dotMatch = new Var<>(false);
        return FirstOf(
                Sequence(
                        FirstOf(
                                Sequence(DotChar(), dotMatch.set(true)),
                                Sequence(WhiteSpace(), OpenBracket())
                        ),
                        AnyQuote(),
                        PhraseChars(AnyQuote(), false),
                        AnyQuote(),
                        FirstOf(
                                // if dotMatch == true, then we return, otherwise we require a CloseBracket()
                                ACTION(dotMatch.get()),
                                Sequence(WhiteSpace(), CloseBracket())
                        ),
                        push("." + pop())

                ),
                Sequence(
                        // no delims found, consume chars to whitespace
                        DotChar(),
                        Chars(),
                        // Chars() pushes on to stack, so we throw that away
                        // b/c we want the entire match() from PathedColumn()
                        popAction(),
                        push("." + match())
                )
        );
    }

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
                ProjectChars(),
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

    Rule AnyQuote() {
        return FirstOf(QuoteChar(), SingleQuoteChar());
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

    Rule ProjectChars() {
        return String("_projects_");
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

    Rule DotChar() {
        return Ch('.');
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

    Rule SingleQuoteChar() {
        return Ch('\'');
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

    Action popAction() {
        return new Action() {
            @Override
            public boolean run(Context context) {
                pop();
                return true;
            }
        };
    }

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

    Action logicalAction(LogicalOperator op) {
        return new Action() {
            @Override
            public boolean run(Context context) {
                Expression left = popExp(1);
                Expression right = popExp();

                if (left instanceof SelectExpression || right instanceof SelectExpression)
                    return false;

                push(new LogicalExpression(op, left, right));
                return true;
            }
        };
    }
}
