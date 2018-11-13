package biocode.fims.query;

import biocode.fims.config.models.DataType;
import biocode.fims.config.models.Entity;
import org.springframework.util.Assert;

/**
 * @author rjewing
 */
public class ColumnUri implements QueryColumn {
    private final Entity entity;
    private final String uri;
    private boolean parentIdentifier;

    ColumnUri(Entity entity, String uri, boolean isParentIdentifier) {
        Assert.notNull(uri);
        Assert.notNull(entity);
        this.entity = entity;
        this.uri = uri;
        this.parentIdentifier = isParentIdentifier;
    }

    @Override
    public String table() {
        return entity.getConceptAlias();
    }

    @Override
    public String property() {
        return uri;
    }

    @Override
    public String column() {
        if (isLocalIdentifier()) return "local_identifier";
        else if (isParentIdentifier()) return "parent_identifier";
        return "data";
    }

    @Override
    public DataType dataType() {
        return entity.getAttributeByUri(uri).getDataType();
    }

    @Override
    public Entity entity() {
        return entity;
    }

    @Override
    public boolean isLocalIdentifier() {
        return uri.equals(entity.getUniqueKeyURI());
    }

    @Override
    public boolean isParentIdentifier() {
        return parentIdentifier;
    }
}
