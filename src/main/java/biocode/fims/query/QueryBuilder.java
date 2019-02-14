package biocode.fims.query;

import biocode.fims.config.Config;
import biocode.fims.config.models.DataType;
import biocode.fims.config.models.Entity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.query.dsl.*;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * {@link QueryBuildingExpressionVisitor} to build PostgreSQL SELECT queries.
 *
 * @author rjewing
 */
public class QueryBuilder implements QueryBuildingExpressionVisitor {

    private final StringBuilder whereBuilder;
    private final JoinBuilder joinBuilder;
    private final int networkId;
    private Entity queryEntity;
    private Config config;
    private boolean allQuery;
    private Map<String, Object> params;
    private Integer page;
    private Integer limit;

    public QueryBuilder(Config config, int networkId, String entityConceptAlias) {
        this.config = config;
        this.networkId = networkId;
        this.queryEntity = config.entity(entityConceptAlias);
        this.whereBuilder = new StringBuilder();
        this.params = new HashMap<>();

        if (queryEntity == null) {
            throw new FimsRuntimeException(QueryCode.UNKNOWN_ENTITY, 400, entityConceptAlias);
        }

        this.joinBuilder = new JoinBuilder(queryEntity, config, networkId);
    }

    /**
     * @param config
     * @param networkId
     * @param entityConceptAlias
     * @param page               0 based page to fetch
     * @param limit              # of records to return
     */
    public QueryBuilder(Config config, int networkId, String entityConceptAlias, Integer page, Integer limit) {
        this(config, networkId, entityConceptAlias);
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
        QueryColumn queryColumn = lookupQueryColumn(expression.column());
        appendComparison(queryColumn, expression.operator(), expression.term());
    }

    @Override
    public void visit(ExistsExpression expression) {
        Map<String, List<QueryColumn>> entityColumns = getEntityQueryColumns(expression);

        if (entityColumns.size() > 1) {
            whereBuilder.append("(");
        }

        int c = 1;
        for (Map.Entry<String, List<QueryColumn>> entry : entityColumns.entrySet()) {
            String table = entry.getKey();
            List<QueryColumn> columns = entry.getValue();

            whereBuilder
                    .append(table)
                    .append(".data") // always use the data column
                    .append(" ??");

            if (columns.size() == 1) {

                whereBuilder
                        .append(" ")
                        .append(putParam(columns.get(0).property()))
                        .append(" ");
            } else {
                whereBuilder
                        .append("& array[")
                        .append(String.join(", ", putParams(columns.stream().map(QueryColumn::property).collect(Collectors.toList()))))
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

    private Map<String, List<QueryColumn>> getEntityQueryColumns(ExistsExpression expression) {
        Map<String, List<QueryColumn>> entityColumns = new HashMap<>();

        for (String column : expression.columns()) {
            QueryColumn queryColumn = lookupQueryColumn(column);
            entityColumns.computeIfAbsent(queryColumn.table(), k -> new ArrayList<>()).add(queryColumn);
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
    public void visit(ProjectExpression expression) {
        joinBuilder.joinExpeditions(true);

        List<Integer> projects = expression.projects();

        whereBuilder.append("expeditions.project_id ");

        if (projects.size() == 1) {
            whereBuilder
                    .append("= ")
                    .append(putParam(projects.get(0)));
        } else {
            whereBuilder
                    .append("IN (")
                    .append(String.join(", ", putParams(projects)))
                    .append(")");
        }
    }

    @Override
    public void visit(SelectExpression expression) {
        expression.entities().stream()
                .map(config::entity)
                .filter(Objects::nonNull) // drop any entities that don't exist in the project
                .forEach(joinBuilder::addSelect);

        if (expression.expression() != null) {
            expression.expression().accept(this);
        }
    }

    @Override
    public void visit(FTSExpression expression) {
        String key = putParam(expression.term().replaceAll("\\s+", " & "));
        if (StringUtils.isBlank(expression.column())) {
            List<Entity> parentEntities = config.parentEntities(queryEntity.getConceptAlias());

            if (parentEntities.isEmpty()) {
                appendFTSTSVQuery(queryEntity.getConceptAlias(), key);
            } else {
                whereBuilder.append("(");

                appendFTSTSVQuery(queryEntity.getConceptAlias(), key);
                whereBuilder.append(" OR ");

                for (Entity entity : parentEntities) {
                    joinBuilder.add(entity);
                    appendFTSTSVQuery(entity.getConceptAlias(), key);
                    whereBuilder.append(" OR ");
                }

                // remove trailing OR
                whereBuilder.delete(whereBuilder.length() - 4, whereBuilder.length());
                whereBuilder.append(")");
            }
        } else {
            QueryColumn queryColumn = lookupQueryColumn(expression.column());

            whereBuilder
                    .append("(to_tsvector(")
                    .append(queryColumn.table())
                    .append(".")
                    .append(queryColumn.column())
                    .append("->>'")
                    .append(queryColumn.property())
                    .append("') @@ to_tsquery(")
                    .append(key)
                    .append(") AND ");

            appendFTSTSVQuery(queryColumn.table(), key);
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
        QueryColumn queryColumn = lookupQueryColumn(expression.column());

        addColumn(queryColumn);
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

        QueryColumn queryColumn = lookupQueryColumn(expression.column());

        RangeExpression.ParsedRange range = expression.parsedRange();

        whereBuilder.append("(");

        boolean hasLeft = range.leftValue() != null;
        boolean hasRight = range.rightValue() != null;

        if (hasLeft) {
            appendComparison(queryColumn, range.leftOperator(), range.leftValue());

            if (hasRight) {
                whereBuilder.append(" AND ");
            }
        }

        if (hasRight) {
            appendComparison(queryColumn, range.rightOperator(), range.rightValue());
        }

        whereBuilder.append(")");

    }

    private void appendComparison(QueryColumn queryColumn, ComparisonOperator operator, String value) {
        String valCast = null;
        String castFunc = null;

        if (operator != ComparisonOperator.EQUALS && operator != ComparisonOperator.NOT_EQUALS) {
            castFunc = lookupCastFunction(queryColumn.dataType());
            valCast = lookupCast(queryColumn.dataType());
        }

        if (castFunc != null) {
            whereBuilder
                    .append(castFunc)
                    .append("(");
        }

        addColumn(queryColumn);
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

    private void addColumn(QueryColumn queryColumn) {
        whereBuilder.append(queryColumn.table());
        if (queryColumn.isLocalIdentifier()) {
            whereBuilder.append(".local_identifier");
        } else if (queryColumn.isParentIdentifier()) {
            whereBuilder.append(".parent_identifier");
        } else {
            whereBuilder.append(".")
                    .append(queryColumn.column())
                    .append("->>'")
                    .append(queryColumn.property())
                    .append("'");
        }
    }

    @Override
    public String queryTable() {
        return buildTable(queryEntity.getConceptAlias());
    }

    @Override
    public void setProjectConfig(ProjectConfig config) {
        this.config = config;
        this.queryEntity = config.entity(this.queryEntity.getConceptAlias());
        if (this.queryEntity == null) {
            throw new FimsRuntimeException(QueryCode.UNKNOWN_ENTITY, 400, this.queryEntity.getConceptAlias());
        }
        this.joinBuilder.setProjectConfig(config, queryEntity);
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

        sql += joinBuilder.build();

        if (whereBuilder.toString().trim().length() > 0) {
            sql += " WHERE " + whereBuilder.append(orderBy).toString();
        } else {
            sql += orderBy.toString();
        }

        return new ParametrizedQuery(sql, params);
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
        return PostgresUtils.entityTableAs(networkId, conceptAlias);
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

    private QueryColumn lookupQueryColumn(String column) {
        try {
            return (multiEntityConfig()) ?
                    lookupMultiEntityQueryColumn(column) :
                    lookupSingleEntityQueryColumn(column);
        } catch (IllegalArgumentException e) {
            throw new FimsRuntimeException(QueryCode.UNKNOWN_COLUMN, 400, column);
        }
    }

    private QueryColumn lookupSingleEntityQueryColumn(String column) {
        Entity entity = config.entities().get(0);

        if (pathBasedColumn(column)) {
            String[] columnPath = splitColumnPath(column);

            if (isExpeditionPropQuery(columnPath[0])) {
                joinBuilder.joinExpeditions(true);
                return new ExpeditionProp(config, columnPath[1]);
            }

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

    private QueryColumn lookupMultiEntityQueryColumn(String column) {
        QueryColumn queryColumn;

        if (pathBasedColumn(column)) {
            String[] columnPath = splitColumnPath(column);
            queryColumn = lookupQueryColumnFromPath(columnPath[0], columnPath[1]);
        } else {
            queryColumn = lookupAmbiguousQueryColumn(column);
        }


        if (!(queryColumn instanceof ExpeditionProp) && !queryEntity.getConceptAlias().equals(queryColumn.table())) {
            joinBuilder.add(queryColumn.entity());
        }

        return queryColumn;
    }

    private boolean pathBasedColumn(String column) {
        return splitColumnPath(column).length == 2;
    }

    private String[] splitColumnPath(String column) {
        return column.split("\\.");
    }

    private QueryColumn lookupAmbiguousQueryColumn(String column) {

        String entityAttributeUri = queryEntity.getAttributeUri(column);
        if (!StringUtils.isBlank(entityAttributeUri)) {
            return new ColumnUri(queryEntity, entityAttributeUri, isParentIdentifier(queryEntity, column));
        }

        for (Entity entity : config.entities()) {
            entityAttributeUri = entity.getAttributeUri(column);

            if (!StringUtils.isBlank(entityAttributeUri)) {
                return new ColumnUri(entity, entityAttributeUri, isParentIdentifier(entity, column));
            }
        }

        throw new IllegalArgumentException("Could not find Attribute for column.");
    }

    private QueryColumn lookupQueryColumnFromPath(String conceptAlias, String column) {
        if (isExpeditionPropQuery(conceptAlias)) {
            joinBuilder.joinExpeditions(true);
            return new ExpeditionProp(config, column);
        }

        Entity entity = config.entity(conceptAlias);

        if (entity == null) {
            throw new FimsRuntimeException(QueryCode.UNKNOWN_ENTITY, 400, String.join(".", conceptAlias, column));
        }

        return new ColumnUri(entity, entity.getAttributeUri(column), isParentIdentifier(entity, column));
    }

    private boolean isExpeditionPropQuery(String table) {
        return table.equalsIgnoreCase("expedition");
    }

    private boolean multiEntityConfig() {
        return config.entities().size() > 1;
    }

    private boolean isParentIdentifier(Entity entity, String column) {
        return !StringUtils.isBlank(column) && entity.isChildEntity() && column.equals(config.entity(entity.getParentEntity()).getUniqueKey());
    }

    private String putParam(Object val) {
        String key = String.valueOf(params.size() + 1);
        params.put(key, val);
        return ":" + key;
    }

    private List<String> putParams(List<?> vals) {
        List<String> keys = new LinkedList<>();

        for (Object val : vals) {
            keys.add(putParam(val));
        }

        return keys;
    }
}
