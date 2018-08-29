package biocode.fims.query.dsl;


import biocode.fims.config.Config;
import biocode.fims.config.models.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.models.Project;
import biocode.fims.query.*;
import org.parboiled.Parboiled;
import org.parboiled.errors.ParserRuntimeException;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
public class Query {

    private final QueryBuildingExpressionVisitor queryBuilder;
    private final Expression expression;
    private final Config config;
    private Set<String> expeditions;
    private Set<Entity> entities;

    public Query(QueryBuildingExpressionVisitor queryBuilder, Config config, Expression expression) {
        this.config = config;
        Assert.notNull(queryBuilder);
        Assert.notNull(expression);
        this.queryBuilder = queryBuilder;
        this.expression = expression;
    }

    public Integer page() {
        return queryBuilder.page();
    }

    public Integer limit() {
        return queryBuilder.limit();
    }

    public ParametrizedQuery parameterizedQuery(boolean onlyPublicExpeditions) {
        expression.accept(queryBuilder);
        return queryBuilder.parameterizedQuery(onlyPublicExpeditions);
    }

    public Set<String> expeditions() {
        if (expeditions == null) {
            ExpeditionCollectingExpressionVisitor visitor = new ExpeditionCollectingExpressionVisitor();
            expression.accept(visitor);
            expeditions = visitor.expeditions();
        }

        return expeditions;
    }

    public Entity queryEntity() {
        return queryBuilder.entity();
    }

    public List<Entity> configEntities() {
        return this.config.entities();
    }

    public Set<Entity> entities() {
        if (entities == null) {
            EntityCollectingExpressionVisitor visitor = new EntityCollectingExpressionVisitor();
            expression.accept(visitor);
            entities = visitor.entities()
                    .stream()
                    .map(config::entity)
                    .collect(Collectors.toSet());
            entities.add(queryEntity());

        }

        return entities;
    }

    public static Query factory(Project project, String conceptAlias, String queryString) {
        return factory(project, conceptAlias, queryString, null, null);
    }

    public static Query factory(Project project, String conceptAlias, String queryString, Integer page, Integer limit) {
        QueryBuilder queryBuilder = new QueryBuilder(project.getProjectConfig(), project.getNetwork().getId(), conceptAlias, page, limit);

        QueryParser parser = Parboiled.createParser(QueryParser.class, queryBuilder, project.getProjectConfig());
        try {
            ParsingResult<Query> result = new ReportingParseRunner<Query>(parser.Parse()).run(queryString);

            if (result.hasErrors() || result.resultValue == null) {
                throw new FimsRuntimeException(QueryCode.INVALID_QUERY, 400, result.parseErrors.toString());
            }

            return result.resultValue;
        } catch (ParserRuntimeException e) {
            String parsedMsg = e.getMessage().replaceFirst(" action '(.*)'", "");
            throw new FimsRuntimeException(QueryCode.INVALID_QUERY, 400, parsedMsg.substring(0, (parsedMsg.indexOf("^"))));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Query)) return false;

        Query query = (Query) o;

        if (!queryBuilder.equals(query.queryBuilder)) return false;
        return expression.equals(query.expression);
    }

    @Override
    public int hashCode() {
        int result = queryBuilder.hashCode();
        result = 31 * result + expression.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return expression.toString();
    }
}
