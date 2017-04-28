package biocode.fims.digester;

import javax.xml.bind.annotation.XmlEnum;

/**
 * Specifies the valid dataformat values for an {@link Attribute}
 */
//@XmlEnum(String.class)
public enum DataType {
    INTEGER, STRING, DATE, DATETIME, TIME, FLOAT
}
