package biocode.fims.digester;

import biocode.fims.settings.FimsPrinter;

import java.util.LinkedList;

/**
 * Entity representation
 */
public class Entity {

    private String worksheet;
    private String worksheetUniqueKey;
    private String conceptAlias;
    private String conceptURI;
    private String entityId;

    private final LinkedList<Attribute> attributes = new LinkedList<Attribute>();

    /**
     * Add an Attribute to this Entity by appending to the LinkedList of attributes
     *
     * @param a
     */
    public void addAttribute(Attribute a) {
        attributes.addLast(a);
    }

    public LinkedList<Attribute> getAttributes() {
        return attributes;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getWorksheet() {
        return worksheet;
    }

    public void setWorksheet(String worksheet) {
        this.worksheet = worksheet;
    }

    public String getWorksheetUniqueKey() {
        return worksheetUniqueKey;
    }

    public void setWorksheetUniqueKey(String worksheetUniqueKey) {
        this.worksheetUniqueKey = worksheetUniqueKey;
    }

    public String getConceptAlias() {
        return conceptAlias;
    }

    public void setConceptAlias(String conceptAlias) {
        this.conceptAlias = conceptAlias.replace(" ", "_");
    }

    public String getConceptURI() {
        return conceptURI;
    }

    public void setConceptURI(String conceptURI) {
        this.conceptURI = conceptURI;
    }

    /**
    /**
     * Get the table.column notation
     *
     * @return
     */
    public String getColumn() {
        return worksheet + "." + worksheetUniqueKey;
    }

    /**
     * Basic Text printer
     */
    public void print() {
        FimsPrinter.out.println("  EntityId:" + entityId);
        FimsPrinter.out.println("    worksheet=" + worksheet);
        FimsPrinter.out.println("    worksheetUniqueKey=" + worksheetUniqueKey);
        FimsPrinter.out.println("    conceptName=" + conceptAlias);
        FimsPrinter.out.println("    conceptURI=" + conceptURI);
        //fimsPrinter.out.println("    Bcid=" + Bcid);
        if (attributes.size() > 0) {
            for (Attribute attribute : attributes)
                attribute.print();
        }

    }
}