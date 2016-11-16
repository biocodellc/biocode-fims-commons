package biocode.fims.digester;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.settings.FimsPrinter;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.digester3.Digester;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
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
     * The default sheetname is the one referenced by the first entity
     * TODO: set defaultSheetName in a more formal manner, currently we're basing this on a "single" spreadsheet model
     *
     * @return
     */
    public String getDefaultSheetName() {
        // TODO this will fail if the 1st entity doesn't have a worksheet
        if (entities.size() > 0) {
            return entities.getFirst().getWorksheet();
        }
        return null;
    }

    /**
     * Get the conceptForwardingAddress for the entity with the given identifier
     *
     * @return
     */
    public String getConceptForwardingAddress(String identifier) {
        String forwardingAddress = null;
        for (Entity entity : entities) {
            if (entity.getIdentifier().toString().equals(identifier)) {
                forwardingAddress = entity.getConceptForwardingAddress();
                break;
            }
        }
        return forwardingAddress;
    }

    /**
     * The default unique key is the one referenced by the first entity
     *
     * @return
     */
    public String getDefaultSheetUniqueKey() {
        // TODO this will fail if the 1st entity doesn't have a worksheet
        if (entities.size() > 0) {
            return entities.getFirst().getWorksheetUniqueKey();
        }
        return null;
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
     * Find Entity defined by given worksheet and worksheetUniqueKey
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
     * Just tell us where the file is stored...
     */
    public void print() {
//        FimsPrinter.out.println("\ttriple output file = " + triplifier.getTripleOutputFile());
        //fimsPrinter.out.println("\tsparql update file = " + triplifier.getUpdateOutputFile());
    }

    /**
     * Loop through the entities and relations we have defined...
     */
    public void printObject() {
        FimsPrinter.out.println("Mapping has " + entities.size() + " entries");

        for (Iterator<Entity> i = entities.iterator(); i.hasNext(); ) {
            Entity e = i.next();
            e.print();
        }

        for (Iterator<Relation> i = relations.iterator(); i.hasNext(); ) {
            Relation r = i.next();
            r.print();
        }
    }

    /**
     * Return a list of ALL attributes defined for entities for a particular worksheet
     *
     * @return
     */
    public ArrayList<Attribute> getAllAttributes(String worksheet) {
        ArrayList<Attribute> attributes = new ArrayList<Attribute>();

        for (Entity entity : entities) {
            if (entity.hasWorksheet() && entity.getWorksheet().equals(worksheet))
                attributes.addAll(entity.getAttributes());
        }

        return attributes;
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
     * Lookup any property associated with a column name from a list of attributes
     * (generated from getAllAttributes functions)
     *
     * @param attributes
     * @return
     */
    public URI lookupColumn(String columnName, ArrayList<Attribute> attributes) {
        Iterator it = attributes.iterator();
        while (it.hasNext()) {
            Attribute a = (Attribute) it.next();
            if (a.getColumn().equalsIgnoreCase(columnName)) {
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
     * Process mapping component rules
     */
    public synchronized void addMappingRules(File configFile) {
        ConvertUtils.register(new EnumConverter(), DataType.class);
        Digester d = new Digester();
        d.push(this);

        // Create entity objects
        d.addObjectCreate("fims/mapping/entity", Entity.class);
        d.addSetProperties("fims/mapping/entity");
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
}