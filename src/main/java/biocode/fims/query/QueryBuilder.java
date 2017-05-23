package biocode.fims.query;

import biocode.fims.digester.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.models.Project;
import biocode.fims.query.dsl.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link QueryBuildingExpressionVisitor} which builds valid PostgreSQL SELECT query.
 *
 * @author rjewing
 */
public class QueryBuilder implements QueryBuildingExpressionVisitor {

    private final Project project;
    private final StringBuilder whereBuilder;
    private final Entity queryEntity;
    private final JoinBuilder joinBuilder;
    private boolean allQuery;

    public QueryBuilder(Project project, String entityConceptAlias) {
        this.project = project;
        this.queryEntity = project.getProjectConfig().getEntity(entityConceptAlias);
        this.whereBuilder = new StringBuilder();

        if (queryEntity == null) {
            throw new FimsRuntimeException(QueryCode.UNKNOWN_ENTITY, 400, entityConceptAlias);
        }

        this.joinBuilder = new JoinBuilder(queryEntity, project.getProjectConfig(), project.getProjectId());
    }

    @Override
    public Entity entity() {
        return queryEntity;
    }

    @Override
    public void visit(ComparisonExpression expression) {
        ColumnUri columnUri = lookupColumnUri(expression.column());

        whereBuilder
                .append(columnUri.conceptAlias())
                .append(".data->>'")
                .append(columnUri.uri())
                .append("' ")
                .append(expression.operator())
                .append(" '")
                .append(expression.term())
                .append("' ");
    }

    @Override
    public void visit(ExistsExpression expression) {
        Map<String, List<String>> entityColumns = getEntityColumnUris(expression);

        if (entityColumns.size() > 1) {
            whereBuilder.append("(");
        }

        int c = 1;
        for (Map.Entry<String, List<String>> entry : entityColumns.entrySet()) {
            String conceptAlias = entry.getKey();
            List<String> columns = entry.getValue();

            whereBuilder
                    .append(conceptAlias)
                    .append(".data ?");

            if (columns.size() == 1) {

                whereBuilder
                        .append(" '")
                        .append(columns.get(0))
                        .append("' ");
            } else {
                whereBuilder
                        .append("& array['")
                        .append(String.join("', '", columns))
                        .append("'] ");
            }

            if (c < entityColumns.size()) {
                whereBuilder.append("AND ");
            } else {
                // remove trailing space
                whereBuilder.deleteCharAt(whereBuilder.length() - 1);
            }
            c++;
        }

        if (entityColumns.size() > 1) {
            whereBuilder.append(")");
        }
    }

    private Map<String, List<String>> getEntityColumnUris(ExistsExpression expression) {
        Map<String, List<String>> entityColumns = new HashMap<>();

        for (String column : expression.columns()) {
            ColumnUri columnUri = lookupColumnUri(column);

            entityColumns.computeIfAbsent(columnUri.conceptAlias(), k -> new ArrayList<>()).add(columnUri.uri());
        }
        return entityColumns;
    }

    @Override
    public void visit(ExpeditionExpression expression) {
        joinBuilder.joinExpeditions(true);

        List<String> expeditions = expression.expeditions();

        whereBuilder.append("expeditions.expeditionCode ");

        if (expeditions.size() == 1) {
            whereBuilder
                    .append("= '")
                    .append(expeditions.get(0))
                    .append("'");
        } else {
            whereBuilder
                    .append("IN ('")
                    .append(String.join("', '", expeditions))
                    .append("')");
        }
    }

    @Override
    public void visit(FTSExpression expression) {
        if (StringUtils.isBlank(expression.column())) {
            appendFTSTSVQuery(queryEntity.getConceptAlias(), expression.term());
        } else {
            ColumnUri columnUri = lookupColumnUri(expression.column());

            whereBuilder
                    .append("(to_tsvector(")
                    .append(columnUri.conceptAlias())
                    .append(".data->>'")
                    .append(columnUri.uri())
                    .append("') @@ to_tsquery('")
                    .append(expression.term())
                    .append("') AND ");

            appendFTSTSVQuery(columnUri.conceptAlias(), expression.term());
            whereBuilder.append(")");
        }
    }

    private void appendFTSTSVQuery(String conceptAlias, String term) {
        whereBuilder
                .append(conceptAlias)
                .append(".tsv @@ to_tsquery('")
                .append(term)
                .append("')");

    }

    @Override
    public void visit(LikeExpression expression) {
        ColumnUri columnUri = lookupColumnUri(expression.column());

        whereBuilder
                .append(columnUri.conceptAlias())
                .append(".data->>'")
                .append(columnUri.uri())
                .append("' ILIKE '")
                .append(expression.term())
                .append("' ");

    }

    @Override
    public void visit(LogicalExpression expression) {
        expression.left().accept(this);
        whereBuilder
                .append(expression.operator())
                .append(" ");
        expression.right().accept(this);
    }

    @Override
    public void visit(RangeExpression expression) {
        //TODO verify that column is a Integer, Date, DateTime, or Time dataType

        ColumnUri columnUri = lookupColumnUri(expression.column());

        RangeExpression.ParsedRange range = expression.parsedRange();

        whereBuilder.append("(");

        boolean hasLeft = range.leftValue() != null;
        boolean hasRight = range.rightValue() != null;

        if (hasLeft) {
            appendRange(columnUri, range.leftOperator(), range.leftValue());

            if (hasRight) {
                whereBuilder.append(" AND ");
            }
        }

        if (hasRight) {
            appendRange(columnUri, range.rightOperator(), range.rightValue());
        }

        whereBuilder.append(")");

    }

    private void appendRange(ColumnUri columnUri, ComparisonOperator operator, String value) {
        whereBuilder
                .append(columnUri.conceptAlias())
                .append(".data->>'")
                .append(columnUri.uri())
                .append("' ")
                .append(operator)
                .append(" '")
                .append(value)
                .append("'");
    }

    @Override
    public void visit(EmptyExpression emptyExpression) {
        throw new FimsRuntimeException(QueryCode.INVALID_QUERY, 400, "query must not be empty");
    }

    @Override
    public void visit(GroupExpression expression) {
        whereBuilder.append("(");
        expression.expression().accept(this);
        whereBuilder.deleteCharAt(whereBuilder.length() - 1); // remove the space from the last visit
        whereBuilder.append(") ");
    }

    @Override
    public void visit(AllExpression allExpression) {
       this.allQuery = true;
    }

    @Override
    public String query() {
        if (!allQuery && whereBuilder.toString().trim().length() == 0) {
            throw new FimsRuntimeException(QueryCode.INVALID_QUERY, 400, "query must not be empty");
        } else if (allQuery && whereBuilder.toString().trim().length() > 0) {
            throw new FimsRuntimeException(QueryCode.INVALID_QUERY, 400);
        }

        if (allQuery) {
            return "SELECT data FROM " + buildTable(queryEntity.getConceptAlias());
        }

        return "SELECT data FROM " +
                buildTable(queryEntity.getConceptAlias()) +
                joinBuilder.build() +
                " WHERE " +
                whereBuilder.toString().trim();
    }

    private String buildTable(String conceptAlias) {
        return PostgresUtils.entityTableAs(project.getProjectId(), conceptAlias);
    }

    private ColumnUri lookupColumnUri(String column) {
        try {
            return (multiEntityConfig()) ?
                    lookupMultiEntityColumnUri(column) :
                    lookupSingleEntityColumnUri(column);
        } catch (IllegalArgumentException e) {
            throw new FimsRuntimeException(QueryCode.UNKNOWN_COLUMN, 400, column);
        }
    }

    private ColumnUri lookupSingleEntityColumnUri(String column) {
        Entity entity = project.getProjectConfig().getEntities().get(0);

        if (pathBasedColumn(column)) {
            String[] columnPath = splitColumnPath(column);

            if (!entity.getConceptAlias().equals(columnPath[0])) {
                throw new FimsRuntimeException(QueryCode.UNKNOWN_ENTITY, 400, column);
            }

            return new ColumnUri(entity, entity.getAttributeUri(columnPath[1]));
        } else {
            return new ColumnUri(entity, entity.getAttributeUri(column));
        }
    }

    private ColumnUri lookupMultiEntityColumnUri(String column) {
        ColumnUri columnUri;

        if (pathBasedColumn(column)) {
            String[] columnPath = splitColumnPath(column);
            columnUri = lookupColumnUriFromPath(columnPath[0], columnPath[1]);
        } else {
            columnUri = lookupAmbiguousColumnUri(column);
        }

        if (!queryEntity.getConceptAlias().equals(columnUri.conceptAlias())) {
            joinBuilder.add(columnUri.entity());
        }

        return columnUri;
    }

    private boolean pathBasedColumn(String column) {
        return splitColumnPath(column).length == 2;
    }

    private String[] splitColumnPath(String column) {
        return column.split("\\.");
    }

    private ColumnUri lookupAmbiguousColumnUri(String column) {

        String entityAttributeUri = queryEntity.getAttributeUri(column);
        if (!StringUtils.isBlank(entityAttributeUri)) {
            return new ColumnUri(queryEntity, entityAttributeUri);
        }

        for (Entity entity : project.getProjectConfig().getEntities()) {
            entityAttributeUri = entity.getAttributeUri(column);

            if (!StringUtils.isBlank(entityAttributeUri)) {
                return new ColumnUri(entity, entityAttributeUri);
            }
        }

        throw new IllegalArgumentException("Could not find Attribute for column.");
    }

    private ColumnUri lookupColumnUriFromPath(String conceptAlias, String column) {
        Entity entity = project.getProjectConfig().getEntity(conceptAlias);

        if (entity == null) {
            throw new FimsRuntimeException(QueryCode.UNKNOWN_ENTITY, 400, String.join(".", conceptAlias, column));
        }

        return new ColumnUri(entity, entity.getAttributeUri(column));
    }

    private boolean multiEntityConfig() {
        return project.getProjectConfig().getEntities().size() > 1;
    }


    private static class ColumnUri {
        private final Entity entity;
        private final String uri;

        private ColumnUri(Entity entity, String uri) {
            Assert.notNull(uri);
            Assert.notNull(entity);
            this.entity = entity;
            this.uri = uri;
        }

        String conceptAlias() {
            return entity.getConceptAlias();
        }

        String uri() {
            return uri;
        }

        Entity entity() {
            return entity;
        }
    }
}
