package biocode.fims.query.dsl;

import biocode.fims.elasticSearch.FieldColumnTransformer;
import org.parboiled.Action;
import org.parboiled.BaseParser;
import org.parboiled.Context;
import org.parboiled.Rule;

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
public class QueryParser extends BaseParser<Object> {

    private FieldColumnTransformer transformer;

    public QueryParser(FieldColumnTransformer transformer) {
        this.transformer = transformer;
    }

    public Rule Parse() {
        return Query();
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
                Expedition(),
                Must(),
                MustNot(),
                Filter(),
                Range(),
                Phrase(),
                Term()
        );
    }

    public Rule Query() {
        return Sequence(
                push(new Query()),
                Optional(QueryExpression())
        );
    }

    public Rule QueryClause() {
        return Sequence(
                push(new QueryClause()),
                Optional(QueryExpressionBreakOnWhiteSpace())
        );
    }

    public Rule FilterQuery() {
        return Sequence(
                push(new FilterQuery()),
                Optional(QueryExpressionBreakOnWhiteSpace())
        );
    }

    public Rule QueryGroup() {
        return Sequence(
                OpenParen(),
                WhiteSpace(),
                Query(),
                WhiteSpace(),
                CloseParen(),
                new Action() {
                    @Override
                    public boolean run(Context context) {
                        ((QueryContainer) peek(1)).add((QueryExpression) pop());
                        return true;
                    }
                }
        );
    }

    public Rule Exists() {
        return Sequence(
                ExistsString(),
                Chars(),
                new Action() {
                    @Override
                    public boolean run(Context context) {
                        ((QueryContainer) peek()).add(
                                new ExistsQuery(transformer, match())
                        );
                        return true;
                    }
                }
        );
    }

    public Rule Expedition() {
        return Sequence(
                ExpeditionString(),
                Chars(),
                new Action() {
                    @Override
                    public boolean run(Context context) {
                        ((ExpeditionQueryContainer) peek()).addExpedition(match());
                        return true;
                    }
                }
        );
    }

    public Rule Must() {
        return Sequence(
                MustChar(),
                WhiteSpace(),
                QueryClause(),
                WhiteSpace(),
                new Action() {
                    @Override
                    public boolean run(Context context) {
                        ((Query) peek(1)).addMust((QueryClause) pop());
                        return true;
                    }
                }
        );
    }

    public Rule MustNot() {
        return Sequence(
                MustNotChar(),
                WhiteSpace(),
                QueryClause(),
                WhiteSpace(),
                new Action() {
                    @Override
                    public boolean run(Context context) {
                        ((Query) peek(1)).addMustNot((QueryClause) pop());
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
                FilterQuery(),
                new FilterAction()
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
                new RangeAction()
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
                        QueryStringQuery q = new QueryStringQuery("\"" + pop() + "\"");
                        ((QueryContainer) peek()).add(q);
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
                        QueryStringQuery q = new QueryStringQuery(match());
                        ((QueryContainer) peek()).add(q);
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
                FirstOf(
                        EscapedReservedChars(),
                        TestNot(ReservedChars())
                ),
                ANY
        );
    }

    public Rule EscapedReservedChars() {
        return Sequence(
                EscapeChar(),
                ReservedChars()
        );
    }

    public Rule ReservedChars() {
        return FirstOf(
                OpenParen(),
                CloseParen(),
                ExistsString(),
                ExpeditionString(),
                MustChar(),
                MustNotChar(),
                ValueDelim(),
                QuoteChar(),
                OpenRangeChars(),
                CloseRangeChars(),
                WhiteSpaceChars()
        );
    }

    public Rule EscapeChar() {
        return Ch('\\');
    }

    public Rule ExistsString() {
        return String("_exists_:");
    }

    public Rule ExpeditionString() {
        return String("expedition:");
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


    class FilterAction implements Action {
        @Override
        public boolean run(Context context) {
            FilterQuery fq = (FilterQuery) pop();
            String col = (String) pop();
            QueryContainer qc = (QueryContainer) peek();

            for (QueryExpression e : fq.getExpressions()) {
                e.setColumn(transformer, col);
                qc.add(e);
            }

            return true;
        }
    }

    class RangeAction implements Action {
        @Override
        public boolean run(Context context) {
            QueryContainer qc = (QueryContainer) peek(3);
            swap3();
            QueryStringQuery q = new QueryStringQuery("" + pop() + pop() + pop());
            qc.add(q);
            return true;
        }
    }
}