package biocode.fims.service;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.authorizers.QueryAuthorizer;
import biocode.fims.bcid.Identifier;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.fimsExceptions.errorCodes.GenericErrorCode;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.models.EntityIdentifier;
import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.projectConfig.models.Entity;
import biocode.fims.query.QueryBuilder;
import biocode.fims.query.QueryResult;
import biocode.fims.query.dsl.*;
import biocode.fims.repositories.EntityIdentifierRepository;
import biocode.fims.repositories.ProjectConfigRepository;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.rest.responses.RecordResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
@Service
public class RecordService {
    private final EntityIdentifierRepository entityIdentifierRepository;
    private final RecordRepository recordRepository;
    private final QueryAuthorizer queryAuthorizer;
    private final ProjectConfigRepository projectConfigRepository;
    private final FimsProperties props;

    @Autowired
    public RecordService(EntityIdentifierRepository entityIdentifierRepository, RecordRepository recordRepository,
                         QueryAuthorizer queryAuthorizer, ProjectConfigRepository projectConfigRepository,
                         FimsProperties properties) {
        this.entityIdentifierRepository = entityIdentifierRepository;
        this.recordRepository = recordRepository;
        this.queryAuthorizer = queryAuthorizer;
        this.projectConfigRepository = projectConfigRepository;
        props = properties;
    }


    public RecordResponse get(User user, String arkID, boolean includeParent, boolean includeChildren) {
        Identifier identifier;
        try {
            identifier = new Identifier(arkID, props.divider());
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new FimsRuntimeException(GenericErrorCode.BAD_REQUEST, 400, "Invalid identifier");
        }

        if (!identifier.hasSuffix()) {
            throw new FimsRuntimeException(GenericErrorCode.BAD_REQUEST, 400, "The provided identifier is a rootIdentifier and does not contain a suffix, therefore we can't fetch a record");
        }

        EntityIdentifier entityIdentifier;
        try {
            entityIdentifier = entityIdentifierRepository.findByIdentifier(new URI(identifier.getRootIdentifier()));
        } catch (URISyntaxException e) {
            throw new FimsRuntimeException(GenericErrorCode.SERVER_ERROR, 500);
        }

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

        Query query = new Query(new QueryBuilder(project, entityIdentifier.getConceptAlias()), config, expression);

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

        return new RecordResponse(parent, record, children.size() == 0 ? null : children);
    }
}
