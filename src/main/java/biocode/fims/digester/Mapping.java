package biocode.fims.digester;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.settings.FimsPrinter;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.digester3.Digester;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;

/**
 * Mapping builds the D2RQ structure for converting between relational format to RDF.
 */
public class Mapping {

    private final LinkedList<Entity> entities = new LinkedList<Entity>();
    private final LinkedList<Relation> relations = new LinkedList<Relation>();
    private Metadata metadata;

    public Mapping() {

    }

    public void addMetadata(Metadata m) {
        metadata = m;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    /**
     * The default sheetname is the one referenced by the root entity
     *
     * @return
     */
    public String getDefaultSheetName() {
        return getRootEntity().getWorksheet();
    }

    /**
     * the root entity is the first entity
     * @return
     */
    public Entity getRootEntity() {
        return entities.getFirst();
    }

    /**
     * Get the conceptForwardingAddress for the entity with the given identifier
     *
     * @return
     */
    public String getConceptForwardingAddress(String identifier) {
        String forwardingAddress = null;
        for (Entity entity : entities) {
            if (StringUtils.equals(String.valueOf(entity.getIdentifier()), identifier)) {
                forwardingAddress = entity.getConceptForwardingAddress();
                break;
            }
        }
        return forwardingAddress;
    }

    /**
     * The default unique key is the one referenced by the root entity
     *
     * @return
     */
    public String getDefaultSheetUniqueKey() {
        return getRootEntity().getUniqueKey();
    }

    /**
     * Get a list of the column names for each entity in the mapping
     *
     * @return LinkedList<String> of columnNames
     */
    public LinkedList<String> getColumnNames() {
        LinkedList<String> columnNames = new LinkedList<>();
        for (Entity entity : entities) {
            for (Attribute attribute : entity.getAttributes()) {
                columnNames.add(attribute.getColumn());
            }
        }
        return columnNames;
    }

    public java.util.List<String> getColumnNamesForWorksheet(String sheetName) {
        List<String> columnNames = new ArrayList<>();
        for (Attribute a : getAllAttributes(sheetName)) {
            columnNames.add(a.getColumn());
        }
        return columnNames;
    }

    /**
     * Add an Entity to this Mapping by appending to the LinkedList of entities
     *
     * @param e
     */
    public void addEntity(Entity e) {
        entities.addLast(e);
    }

    public LinkedList<Entity> getEntities() {
        return entities;
    }

    /**
     * Add a Relation to this Mapping by appending to the LinkedList of relations
     *
     * @param r
     */
    public void addRelation(Relation r) {
        relations.addLast(r);
    }

    public LinkedList<Relation> getRelations() {
        return relations;
    }

    /**
     * Find Entity with a given conceptAlias
     *
     * @param conceptAlias
     * @return
     */
    public Entity findEntity(String conceptAlias) {
        for (Entity entity : entities) {
            if (conceptAlias.equals(entity.getConceptAlias()))
                return entity;
        }
        return null;
    }

    /**
     * find entity with the given conceptUri
     * @param conceptUri
     * @return
     */
    public Entity findEntityByConceptUri(String conceptUri) {
        for (Entity entity: entities) {
            if (conceptUri.equals(entity.getConceptURI())) {
                return entity;
            }
        }

        return null;
    }

    /**
     * Return a list of ALL attributes defined for entities for a particular worksheet
     *
     * @return
     */
    public ArrayList<Attribute> getAllAttributes(String worksheet) {
        Set<Attribute> attributes = new HashSet<>();

        for (Entity entity : entities) {
            if (entity.hasWorksheet(worksheet))
                attributes.addAll(entity.getAttributes());
        }

        // TODO return a Set<Attribute>
        return new ArrayList<>(attributes);
    }

    /**
     * convience method to get all attributes for the default sheet
     *
     * @return
     */
    public ArrayList<Attribute> getDefaultSheetAttributes() {
        return getAllAttributes(getDefaultSheetName());
    }

    /**
     * return all {@link Entity} objects that have the given {@Attribute}.uri
     *
     * @param uri
     * @return
     */
    public ArrayList<Entity> getEntititesWithAttributeUri(String uri) {
        ArrayList<Entity> entities = new ArrayList<>();

        for (Entity entity : this.entities) {
            for (Attribute attribute : entity.getAttributes()) {
                if (attribute.getUri().equals(uri)) {
                    entities.add(entity);
                }
            }
        }
        return entities;
    }

    /**
     * Lookup any property associated with a column name from a list of attributes
     * (generated from  functions)
     *
     * @param attributes
     * @return
     */
    public URI lookupAnyProperty(URI property, ArrayList<Attribute> attributes) {
        Iterator it = attributes.iterator();
        while (it.hasNext()) {
            Attribute a = (Attribute) it.next();
            if (a.getUri().equalsIgnoreCase(property.toString())) {
                try {
                    return new URI(a.getUri());
                } catch (URISyntaxException e) {
                    throw new FimsRuntimeException(500, e);
                }
            }
        }
        return null;
    }
    /**
     * Lookup the column associated with a uri from a list of attributes
     *
     * @param attributes
     * @return
     */
    public String lookupColumnForUri(String uri, List<Attribute> attributes) {
        for (Attribute attribute: attributes) {
            if (attribute.getUri().equalsIgnoreCase(uri)) {
                return attribute.getColumn();
            }
        }

        return null;
    }

    /**
     * Lookup the uri associated with a columnName from a list of attributes
     *
     * @param attributes
     * @return
     */
    public String lookupUriForColumn(String columnName, List<Attribute> attributes) {
        for (Attribute attribute: attributes) {
            if (attribute.getColumn().equalsIgnoreCase(columnName)) {
                return attribute.getUri();
            }
        }

        return null;
    }

    /**
     * Process mapping component rules
     */
    public synchronized void addMappingRules(File configFile) {
        ConvertUtils.register(new EnumConverter(), DataType.class);
        Digester d = new Digester();
        d.push(this);

        // Create entity objects
        d.addObjectCreate("fims/mapping/entity", Entity.class);
        // the last 2 params provide backwards compatibility for config files that still use worksheetUniqueKey
        d.addSetProperties("fims/mapping/entity", "worksheetUniqueKey", "uniqueKey");
        d.addSetNext("fims/mapping/entity", "addEntity");

        // Add attributes associated with this entity
        d.addObjectCreate("fims/mapping/entity/attribute", Attribute.class);
        d.addSetProperties("fims/mapping/entity/attribute");
        d.addCallMethod("fims/mapping/entity/attribute", "addDefinition", 0);
        // Next two lines are newer, may not appear in all configuration files
        d.addCallMethod("fims/mapping/entity/attribute/synonyms", "addSynonyms", 0);
        d.addSetNext("fims/mapping/entity/attribute", "addAttribute");

        // Create relation objects
        d.addObjectCreate("fims/mapping/relation", Relation.class);
        d.addSetNext("fims/mapping/relation", "addRelation");
        d.addCallMethod("fims/mapping/relation/subject", "addSubject", 0);
        d.addCallMethod("fims/mapping/relation/predicate", "addPredicate", 0);
        d.addCallMethod("fims/mapping/relation/object", "addObject", 0);

        // Add metadata
        d.addObjectCreate("fims/metadata", Metadata.class);
        d.addSetProperties("fims/metadata");
        d.addCallMethod("fims/metadata", "addTextAbstract", 0);
        d.addSetNext("fims/metadata", "addMetadata");

        try {
            d.parse(configFile);
        } catch (IOException e) {
            throw new FimsRuntimeException(500, e);
        } catch (SAXException e) {
            throw new FimsRuntimeException(500, e);
        }
    }

    public Attribute lookupAttribute(String columnName, String sheetName) {
        for (Attribute a: getAllAttributes(sheetName)) {
            if (a.getColumn().equals(columnName)) {
                return a;
            }
        }

        return null;
    }
}
