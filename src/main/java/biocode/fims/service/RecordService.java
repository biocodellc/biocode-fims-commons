package biocode.fims.service;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.authorizers.QueryAuthorizer;
import biocode.fims.bcid.Identifier;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.fimsExceptions.errorCodes.GenericErrorCode;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.models.EntityIdentifier;
import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.config.models.Entity;
import biocode.fims.query.QueryBuilder;
import biocode.fims.query.QueryResult;
import biocode.fims.query.dsl.*;
import biocode.fims.repositories.EntityIdentifierRepository;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.rest.responses.RecordResponse;
import biocode.fims.run.DatasetAuthorizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
@Service
public class RecordService {
    private final EntityIdentifierRepository entityIdentifierRepository;
    private final RecordRepository recordRepository;
    private final QueryAuthorizer queryAuthorizer;
    private final DatasetAuthorizer datasetAuthorizer;
    private final FimsProperties props;

    @Autowired
    public RecordService(EntityIdentifierRepository entityIdentifierRepository, RecordRepository recordRepository,
                         QueryAuthorizer queryAuthorizer, DatasetAuthorizer datasetAuthorizer, FimsProperties properties) {
        this.entityIdentifierRepository = entityIdentifierRepository;
        this.recordRepository = recordRepository;
        this.queryAuthorizer = queryAuthorizer;
        this.datasetAuthorizer = datasetAuthorizer;
        props = properties;
    }

    public boolean delete(User user, String arkID) {
        Identifier identifier = parseIdentifier(arkID);
        EntityIdentifier entityIdentifier = getEntityIdentifier(identifier);

        if (entityIdentifier == null) {
            throw new FimsRuntimeException(GenericErrorCode.BAD_REQUEST, 400, "Invalid identifier");
        }

        if (!datasetAuthorizer.authorize(entityIdentifier, user)) {
            throw new ForbiddenRequestException("You are not authorized to delete this record");
        }

        return recordRepository.delete(identifier.getRootIdentifier(), identifier.getSuffix());
    }


    public RecordResponse get(User user, String arkID, boolean includeParent, boolean includeChildren) {
        Identifier identifier = parseIdentifier(arkID);
        EntityIdentifier entityIdentifier = getEntityIdentifier(identifier);

        if (entityIdentifier == null) return null;

        if (!queryAuthorizer.authorizedQuery(entityIdentifier.getExpedition().getExpeditionId(), user)) {
            throw new ForbiddenRequestException("You are not authorized to access this record's data.");
        }

        Project project = entityIdentifier.getExpedition().getProject();
        ProjectConfig config = project.getProjectConfig();
        Entity entity = config.entity(entityIdentifier.getConceptAlias());

        String uniqueKey = entity.getUniqueKey();
        if (entity.isChildEntity() && entity.getUniqueKey() == null) {
            uniqueKey = config.entity(entity.getParentEntity()).getUniqueKey();
        }

        Expression expression = new LogicalExpression(LogicalOperator.AND,
                new ExpeditionExpression(entityIdentifier.getExpedition().getExpeditionCode()),
                new ComparisonExpression(uniqueKey, identifier.getSuffix(), ComparisonOperator.EQUALS)
        );
        expression = new LogicalExpression(LogicalOperator.AND,
                new ProjectExpression(Collections.singletonList(entityIdentifier.getExpedition().getProject().getProjectId())),
                expression
        );

        if (includeParent || includeChildren) {
            List<String> selects = new ArrayList<>();

            if (includeParent && entity.getParentEntity() != null) {
                selects.add(entity.getParentEntity());
            }

            if (includeChildren) {
                selects.addAll(
                        config.entities().stream()
                                .filter(e -> entity.getConceptAlias().equals(e.getParentEntity()))
                                .map(Entity::getConceptAlias)
                                .collect(Collectors.toList())
                );
            }

            if (selects.size() > 0) {
                expression = new SelectExpression(String.join(",", selects), expression);
            }
        }

        Query query = new Query(new QueryBuilder(config, project.getNetwork().getId(), entityIdentifier.getConceptAlias()), config, expression);

        Map<String, String> parent = null;
        Map<String, String> record = null;
        List<Map<String, String>> children = new ArrayList<>();

        for (QueryResult result : recordRepository.query(query)) {
            if (result.entity().getConceptAlias().equals(entity.getConceptAlias())) {
                if (result.records().isEmpty()) throw new FimsRuntimeException(QueryCode.NO_RESOURCES, 400);

                record = result.get(false).get(0);
                record.put("entity", result.entity().getConceptAlias());
            } else if (result.entity().getConceptAlias().equals(entity.getParentEntity())) {
                parent = result.get(false).get(0);
                parent.put("entity", result.entity().getConceptAlias());
            } else {
                List<Map<String, String>> childs = result.get(false);
                childs.forEach(c -> c.put("entity", result.entity().getConceptAlias()));
                children.addAll(childs);
            }
        }

        if (record == null) return null;
        return new RecordResponse(project.getProjectId(), parent, record, children.size() == 0 ? null : children);
    }

    private EntityIdentifier getEntityIdentifier(Identifier identifier) {
        EntityIdentifier entityIdentifier;
        try {
            entityIdentifier = entityIdentifierRepository.findByIdentifier(new URI(identifier.getRootIdentifier()));
        } catch (URISyntaxException e) {
            throw new FimsRuntimeException(GenericErrorCode.SERVER_ERROR, 500);
        }
        return entityIdentifier;
    }

    private Identifier parseIdentifier(String arkID) {
        Identifier identifier;
        try {
            identifier = new Identifier(arkID, props.divider());
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new FimsRuntimeException(GenericErrorCode.BAD_REQUEST, 400, "Invalid identifier");
        }

        if (!identifier.hasSuffix()) {
            throw new FimsRuntimeException(GenericErrorCode.BAD_REQUEST, 400, "The provided identifier is a rootIdentifier and does not contain a suffix, therefore we can't fetch a record");
        }
        return identifier;
    }
}
