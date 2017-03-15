package biocode.fims.query.dsl;

import org.parboiled.Action;
import org.parboiled.BaseParser;
import org.parboiled.Context;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;

/**
 * Custom fims query parser. This minimally parses an incoming query string so we can reconstruct the appropriate
 * elastic search query. Elastic search query_string queries do not support nested queries, se we need to parse
 * just enough to be able to reconstruct the appropriate Elastic Search QueryBuilders, with support for nested queries.
 * <p>
 * TODO prevent regexp and wildcard queries? Bad performance and resource intensive
 *
 * @author rjewing
 */
@SuppressWarnings("InfiniteRecursion")
@BuildParseTree
public class QueryParser extends BaseParser<Object> {

    private final QueryAction NewQuery = new QueryAction();

    public Rule Parse() {
        return Sequence(
                NewQuery,
                Optional(QueryExpression())
        );
    }

    public Rule QueryExpression() {
        return Optional(
                FirstOf(
                        AnyExpression(),
                        OneOrMore(WhiteSpaceChars())
                ),
                QueryExpression()
        );
    }

    public Rule QueryExpressionBreakOnWhiteSpace() {
        return Optional(
                AnyExpression(),
                QueryExpressionBreakOnWhiteSpace()
        );
    }

    public Rule AnyExpression() {
        return FirstOf(
                QueryGroup(),
                Exists(),
                Must(),
                MustNot(),
                Filter(),
                Range(),
                Phrase(),
                Term()
        );
    }

    public Rule SubQuery() {
        return Sequence(
                NewQuery,
                Optional(QueryExpression())
        );
    }

    public Rule SubQueryBreakOnWhiteSpace() {
        return Sequence(
                NewQuery,
                Optional(QueryExpressionBreakOnWhiteSpace())
        );
    }

    public Rule QueryGroup() {
        return Sequence(
                WhiteSpace(),
                OpenParen(),
                WhiteSpace(),
                SubQuery(),
                WhiteSpace(),
                CloseParen(),
                new Action() {
                    @Override
                    public boolean run(Context context) {
                        ((Query) peek(1)).addShould((Query) pop());
                        return true;
                    }
                }
        );
    }

    public Rule Exists() {
        return Sequence(
                ExistsString(),
                ExistsColumn()
        );
    }

    public Rule ExistsColumn() {
        return Sequence(
                Chars(),
                new Action() {
                    @Override
                    public boolean run(Context context) {
                        ((Query) peek()).addExists(match());
                        return true;
                    }
                }
        );
    }

    public Rule Must() {
        return Sequence(
                MustChar(),
                WhiteSpace(),
                SubQueryBreakOnWhiteSpace(),
                WhiteSpace(),
                new Action() {
                    @Override
                    public boolean run(Context context) {
                        ((Query) peek(1)).addMust((Query) pop());
                        return true;
                    }
                }
        );
    }

    public Rule MustNot() {
        return Sequence(
                MustNotChar(),
                new Action() {
                    @Override
                    public boolean run(Context context) {
                        return true;
                    }
                },
                WhiteSpace(),
                SubQueryBreakOnWhiteSpace(),
                new Action() {
                    @Override
                    public boolean run(Context context) {
                        return true;
                    }
                },
                WhiteSpace(),
                new Action() {
                    @Override
                    public boolean run(Context context) {
                        ((Query) peek(1)).addMustNot((Query) pop());
                        return true;
                    }
                }
        );
    }

    public Rule Filter() {
        return Sequence(
                Chars(),
                push(match()),
                ValueDelim(),
                SubQueryBreakOnWhiteSpace(),
                WhiteSpace(),
                new Action() {
                    @Override
                    public boolean run(Context context) {
                        Query filterQuery = (Query) pop();
                        QueryFilter filter = new QueryFilter((String) pop(), filterQuery);
                        ((Query) peek()).addFilter(filter);
                        return true;
                    }
                }
        );
    }

    public Rule Range() {
        return Sequence(
                OpenRangeChars(),
                push(match()),
                OneOrMore(
                        TestNot(CloseRangeChars()),
                        ANY
                ),
                push(match()),
                CloseRangeChars(),
                push(match()),
                new Action() {
                    @Override
                    public boolean run(Context context) {
                        Query query = (Query) peek(3);
                        swap3();
                        query.appendQueryString("" + pop() + pop() + pop());
                        return true;
                    }
                }
        );
    }

    public Rule Phrase() {
        return Sequence(
                QuoteChar(),
                OneOrMore(
                        TestNot(QuoteChar()),
                        ANY
                ),
                push(match()),
                QuoteChar(),
                new Action() {
                    @Override
                    public boolean run(Context context) {
                        ((Query) peek(1)).appendQueryString("\"" + pop() + "\"");
                        return true;
                    }
                }
        );
    }

    public Rule Term() {
        return Sequence(
                Chars(),
                new Action() {
                    @Override
                    public boolean run(Context context) {
                        ((Query) peek()).appendQueryString(match());
                        return true;
                    }
                }
        );
    }

    public Rule WhiteSpace() {
        return ZeroOrMore(WhiteSpaceChars());
    }

    public Rule Chars() {
        return OneOrMore(
                TestNot(OpenParen()),
                TestNot(CloseParen()),
                TestNot(ExistsString()),
                TestNot(MustChar()),
                TestNot(ValueDelim()),
                TestNot(MustNotChar()),
                TestNot(QuoteChar()),
                TestNot(OpenRangeChars()),
                TestNot(CloseRangeChars()),
                TestNot(WhiteSpaceChars()),
                ANY
        );
    }

    public Rule ExistsString() {
        return String("_exists_:");
    }

    public Rule ValueDelim() {
        return Ch(':');
    }

    public Rule MustChar() {
        return Ch('+');
    }

    public Rule MustNotChar() {
        return Ch('-');
    }

    public Rule QuoteChar() {
        return Ch('"');
    }

    public Rule OpenParen() {
        return Ch('(');
    }

    public Rule CloseParen() {
        return Ch(')');
    }

    public Rule OpenRangeChars() {
        return AnyOf("[{");
    }

    public Rule CloseRangeChars() {
        return AnyOf("]}");
    }

    public Rule WhiteSpaceChars() {
        return AnyOf(" \t\f");
    }

    private class QueryAction implements Action {

        @Override
        public boolean run(Context context) {
            push(new Query());
            return true;
        }
    }
}
