package biocode.fims.query;

import biocode.fims.projectConfig.models.DataType;
import biocode.fims.projectConfig.models.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.models.Project;
import biocode.fims.query.dsl.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.*;

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
    private Map<String, String> params;
    private Integer page;
    private Integer limit;

    public QueryBuilder(Project project, String entityConceptAlias) {
        this.project = project;
        this.queryEntity = project.getProjectConfig().entity(entityConceptAlias);
        this.whereBuilder = new StringBuilder();
        this.params = new HashMap<>();

        if (queryEntity == null) {
            throw new FimsRuntimeException(QueryCode.UNKNOWN_ENTITY, 400, entityConceptAlias);
        }

        this.joinBuilder = new JoinBuilder(queryEntity, project.getProjectConfig(), project.getProjectId());
    }

    /**
     * @param project
     * @param entityConceptAlias
     * @param page               0 based page to fetch
     * @param limit              # of records to return
     */
    public QueryBuilder(Project project, String entityConceptAlias, Integer page, Integer limit) {
        this(project, entityConceptAlias);
        this.page = page;
        this.limit = limit;
    }

    @Override
    public Integer page() {
        return page;
    }

    @Override
    public Integer limit() {
        return limit;
    }

    @Override
    public Entity entity() {
        return queryEntity;
    }

    @Override
    public void visit(ComparisonExpression expression) {
        ColumnUri columnUri = lookupColumnUri(expression.column());
        appendComparison(columnUri, expression.operator(), expression.term());
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
                    .append(".data ??");

            if (columns.size() == 1) {

                whereBuilder
                        .append(" ")
                        .append(putParam(columns.get(0)))
                        .append(" ");
            } else {
                whereBuilder
                        .append("& array[")
                        .append(String.join(", ", putParams(columns)))
                        .append("] ");
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
            // localIdentifier & parentIdentifier are never null, so exclude from query
            if (columnUri.isLocalIdentifier() || columnUri.isParentIdentifier()) continue;

            entityColumns.computeIfAbsent(columnUri.conceptAlias(), k -> new ArrayList<>()).add(columnUri.uri());
        }
        return entityColumns;
    }

    @Override
    public void visit(ExpeditionExpression expression) {
        joinBuilder.joinExpeditions(true);

        List<String> expeditions = expression.expeditions();

        whereBuilder.append("expeditions.expedition_code ");

        if (expeditions.size() == 1) {
            whereBuilder
                    .append("= ")
                    .append(putParam(expeditions.get(0)));
        } else {
            whereBuilder
                    .append("IN (")
                    .append(String.join(", ", putParams(expeditions)))
                    .append(")");
        }
    }

    @Override
    public void visit(SelectExpression expression) {
        expression.entites().forEach(conceptAlias -> joinBuilder.addSelect(this.project.getProjectConfig().entity(conceptAlias)));
        if (expression.expression() != null) {
            expression.expression().accept(this);
        }
    }

    @Override
    public void visit(FTSExpression expression) {
        String key = putParam(expression.term().replaceAll("\\s+", " & "));
        if (StringUtils.isBlank(expression.column())) {
            appendFTSTSVQuery(queryEntity.getConceptAlias(), key);
        } else {
            ColumnUri columnUri = lookupColumnUri(expression.column());

            whereBuilder
                    .append("(to_tsvector(")
                    .append(columnUri.conceptAlias())
                    .append(".data->>'")
                    .append(columnUri.uri())
                    .append("') @@ to_tsquery(")
                    .append(key)
                    .append(") AND ");

            appendFTSTSVQuery(columnUri.conceptAlias(), key);
            whereBuilder.append(")");
        }
    }

    private void appendFTSTSVQuery(String conceptAlias, String termKey) {
        whereBuilder
                .append(conceptAlias)
                .append(".tsv @@ to_tsquery(")
                .append(termKey)
                .append(")");

    }

    @Override
    public void visit(NotExpression notExpression) {
        whereBuilder
                .append("not ");
        notExpression.expression().accept(this);
    }

    @Override
    public void visit(LikeExpression expression) {
        ColumnUri columnUri = lookupColumnUri(expression.column());

        whereBuilder.append(columnUri.conceptAlias());
        addColumn(columnUri);
        whereBuilder
                .append(" ILIKE ")
                .append(putParam(expression.term()));
    }

    @Override
    public void visit(LogicalExpression expression) {
        expression.left().accept(this);
        whereBuilder
                .append(" ")
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
            appendComparison(columnUri, range.leftOperator(), range.leftValue());

            if (hasRight) {
                whereBuilder.append(" AND ");
            }
        }

        if (hasRight) {
            appendComparison(columnUri, range.rightOperator(), range.rightValue());
        }

        whereBuilder.append(")");

    }

    private void appendComparison(ColumnUri columnUri, ComparisonOperator operator, String value) {
        String valCast = null;
        String castFunc = null;

        if (operator != ComparisonOperator.EQUALS && operator != ComparisonOperator.NOT_EQUALS) {
            castFunc = lookupCastFunction(columnUri.dataType());
            valCast = lookupCast(columnUri.dataType());
        }

        if (castFunc != null) {
            whereBuilder
                    .append(castFunc)
                    .append("(");
        }

        whereBuilder.append(columnUri.conceptAlias());
        addColumn(columnUri);
        whereBuilder
                .append(castFunc == null ? " " : ") ")
                .append(operator)
                .append(" ")
                .append(putParam(value));

        if (castFunc != null) {
            whereBuilder
                    .append("::")
                    .append(valCast);
        }
    }

    @Override
    public void visit(EmptyExpression emptyExpression) {
        throw new FimsRuntimeException(QueryCode.INVALID_QUERY, 400, "query must not be empty");
    }

    @Override
    public void visit(GroupExpression expression) {
        whereBuilder.append("(");
        expression.expression().accept(this);
        whereBuilder.append(")");
    }

    @Override
    public void visit(AllExpression allExpression) {
        this.allQuery = true;
    }

    private void addColumn(ColumnUri columnUri) {
        if (columnUri.isLocalIdentifier()) {
            whereBuilder.append(".local_identifier");
        } else if (columnUri.isParentIdentifier()) {
            whereBuilder.append(".parent_identifier");
        } else {
            whereBuilder.append(".data->>'")
                    .append(columnUri.uri())
                    .append("'");
        }
    }

    @Override
    public String queryTable() {
        return buildTable(queryEntity.getConceptAlias());
    }

    @Override
    public ParametrizedQuery parameterizedQuery(boolean onlyPublicExpeditions) {
        if (!allQuery && whereBuilder.toString().trim().length() == 0) {
            throw new FimsRuntimeException(QueryCode.INVALID_QUERY, 400, "query must not be empty");
        } else if (allQuery && whereBuilder.toString().trim().length() > 0) {
            throw new FimsRuntimeException(QueryCode.INVALID_QUERY, 400);
        }

        String sql = buildSelect() + "FROM " +
                buildTable(queryEntity.getConceptAlias());

        StringBuilder orderBy = new StringBuilder()
                .append(" ORDER BY ")
                .append(queryEntity.getConceptAlias())
                .append(".local_identifier, ")
                .append(queryEntity.getConceptAlias())
                .append(".expedition_id");

        if (limit != null) {
            if (page != null) {
                orderBy.append(" OFFSET ").append(page * limit);
            }
            orderBy.append(" LIMIT ").append(limit);
        }

        if (allQuery && !onlyPublicExpeditions) {
            return new ParametrizedQuery(sql + joinBuilder.build() + orderBy.toString(), params);
        }

        if (onlyPublicExpeditions) {
            addPublicExpeditions();
        }

        return new ParametrizedQuery(
                sql + joinBuilder.build() + " WHERE " + whereBuilder.append(orderBy).toString(),
                params
        );
    }

    private void addPublicExpeditions() {
        joinBuilder.joinExpeditions(true);
        if (whereBuilder.toString().trim().length() == 0) {
            whereBuilder.append("expeditions.public = true");
        } else {
            whereBuilder.insert(0, "(");
            whereBuilder.append(") AND expeditions.public = true");
        }
    }

    private String buildTable(String conceptAlias) {
        return PostgresUtils.entityTableAs(project.getProjectId(), conceptAlias);
    }

    private String buildSelect() {
        StringBuilder s = new StringBuilder();

        s
                .append("SELECT ")
                .append(queryEntity.getConceptAlias())
                .append(".data AS \"")
                .append(queryEntity.getConceptAlias())
                .append("_data\", ")
                .append(queryEntity.getConceptAlias())
                .append("_entity_identifiers.identifier AS \"")
                .append(queryEntity.getConceptAlias())
                .append("_rootIdentifier\", ")
                .append("expeditions.expedition_code AS \"expeditionCode\", ")
                .append("expeditions.project_id AS \"projectId\"")
        ;

        for (Entity e : joinBuilder.selectEntities()) {
            s
                    .append(", ")
                    .append(e.getConceptAlias())
                    .append(".data AS \"")
                    .append(e.getConceptAlias())
                    .append("_data\", ")
                    .append(e.getConceptAlias())
                    .append("_entity_identifiers.identifier AS \"")
                    .append(e.getConceptAlias())
                    .append("_rootIdentifier\"");
        }
        s.append(" ");

        return s.toString();
    }

    private String lookupCastFunction(DataType dataType) {
        switch (dataType) {
            case INTEGER:
                return "convert_to_int";
            case FLOAT:
                return "convert_to_float";
            case DATE:
                return "convert_to_date";
            case DATETIME:
                return "convert_to_datetime";
            case TIME:
                return "convert_to_time";
            case BOOLEAN:
                return "convert_to_bool";
            default:
                return null;
        }
    }

    private String lookupCast(DataType dataType) {
        switch (dataType) {
            case INTEGER:
                return "int";
            case FLOAT:
                return "float";
            case DATE:
                return "date";
            case DATETIME:
                return "timestamp";
            case TIME:
                return "time";
            default:
                return null;
        }
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
        Entity entity = project.getProjectConfig().entities().get(0);

        if (pathBasedColumn(column)) {
            String[] columnPath = splitColumnPath(column);

            if (!entity.getConceptAlias().equals(columnPath[0])) {
                throw new FimsRuntimeException(QueryCode.UNKNOWN_COLUMN, 400, column);
            }

            // single entity config can't have a childEntity
            return new ColumnUri(entity, entity.getAttributeUri(columnPath[1]), false);
        } else {
            // single entity config can't have a childEntity
            return new ColumnUri(entity, entity.getAttributeUri(column), false);
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
            return new ColumnUri(queryEntity, entityAttributeUri, isParentIdentifier(queryEntity, column));
        }

        for (Entity entity : project.getProjectConfig().entities()) {
            entityAttributeUri = entity.getAttributeUri(column);

            if (!StringUtils.isBlank(entityAttributeUri)) {
                return new ColumnUri(entity, entityAttributeUri, isParentIdentifier(entity, column));
            }
        }

        throw new IllegalArgumentException("Could not find Attribute for column.");
    }

    private ColumnUri lookupColumnUriFromPath(String conceptAlias, String column) {
        Entity entity = project.getProjectConfig().entity(conceptAlias);

        if (entity == null) {
            throw new FimsRuntimeException(QueryCode.UNKNOWN_ENTITY, 400, String.join(".", conceptAlias, column));
        }

        return new ColumnUri(entity, entity.getAttributeUri(column), isParentIdentifier(entity, column));
    }

    private boolean multiEntityConfig() {
        return project.getProjectConfig().entities().size() > 1;
    }

    private boolean isParentIdentifier(Entity entity, String column) {
        return !StringUtils.isBlank(column) && entity.isChildEntity() && column.equals(project.getProjectConfig().entity(entity.getParentEntity()).getUniqueKey());
    }

    private String putParam(String val) {
        String key = String.valueOf(params.size() + 1);
        params.put(key, val);
        return ":" + key;
    }

    private List<String> putParams(List<String> vals) {
        List<String> keys = new LinkedList<>();

        for (String val : vals) {
            keys.add(putParam(val));
        }

        return keys;
    }

    private static class ColumnUri {
        private final Entity entity;
        private final String uri;
        private boolean parentIdentifier;

        private ColumnUri(Entity entity, String uri, boolean isParentIdentifier) {
            Assert.notNull(uri);
            Assert.notNull(entity);
            this.entity = entity;
            this.uri = uri;
            this.parentIdentifier = isParentIdentifier;
        }

        String conceptAlias() {
            return entity.getConceptAlias();
        }

        String uri() {
            return uri;
        }

        DataType dataType() {
            return entity.getAttributeByUri(uri).getDataType();
        }

        Entity entity() {
            return entity;
        }

        boolean isLocalIdentifier() {
            return uri.equals(entity.getUniqueKeyURI());
        }

        boolean isParentIdentifier() {
            return parentIdentifier;
        }
    }
}
