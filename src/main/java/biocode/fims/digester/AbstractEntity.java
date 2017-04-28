package biocode.fims.digester;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.StringUtils;

import java.util.LinkedList;

/**
 * @author rjewing
 */
public abstract class AbstractEntity {
    private final LinkedList<Attribute> attributes = new LinkedList<Attribute>();
    private String worksheet;
    private String uniqueKey;
    private String conceptAlias;
    private String conceptURI;
    private String conceptForwardingAddress;

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

    public String getWorksheet() {
        return worksheet;
    }

    public void setWorksheet(String worksheet) {
        this.worksheet = worksheet;
    }

    public String getUniqueKey() {
        return uniqueKey;
    }

    public void setUniqueKey(String uniqueKey) {
        this.uniqueKey = uniqueKey;
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

    public String getConceptForwardingAddress() {
        return conceptForwardingAddress;
    }

    public void setConceptForwardingAddress(String conceptForwardingAddress) {
        this.conceptForwardingAddress = conceptForwardingAddress;
    }

    /**
     * If this entity is represented as a worksheet
     * @return
     */
    public boolean hasWorksheet() {
        return !StringUtils.isBlank(worksheet);
    }

    @JsonIgnore
    public boolean hasWorksheet(String sheetName) {
        return worksheet != null && worksheet.equals(sheetName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractEntity)) return false;

        AbstractEntity that = (AbstractEntity) o;

        if (getWorksheet() != null ? !getWorksheet().equals(that.getWorksheet()) : that.getWorksheet() != null)
            return false;
        if (getUniqueKey() != null ? !getUniqueKey().equals(that.getUniqueKey()) : that.getUniqueKey() != null)
            return false;
        if (getConceptAlias() != null ? !getConceptAlias().equals(that.getConceptAlias()) : that.getConceptAlias() != null)
            return false;
        if (getConceptURI() != null ? !getConceptURI().equals(that.getConceptURI()) : that.getConceptURI() != null)
            return false;
        return getConceptForwardingAddress() != null ? getConceptForwardingAddress().equals(that.getConceptForwardingAddress()) : that.getConceptForwardingAddress() == null;
    }

    @Override
    public int hashCode() {
        int result = getWorksheet() != null ? getWorksheet().hashCode() : 0;
        result = 31 * result + (getUniqueKey() != null ? getUniqueKey().hashCode() : 0);
        result = 31 * result + (getConceptAlias() != null ? getConceptAlias().hashCode() : 0);
        result = 31 * result + (getConceptURI() != null ? getConceptURI().hashCode() : 0);
        result = 31 * result + (getConceptForwardingAddress() != null ? getConceptForwardingAddress().hashCode() : 0);
        return result;
    }
}
