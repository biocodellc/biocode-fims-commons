package biocode.fims.query;

import biocode.fims.projectConfig.models.DataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author rjewing
 */
public enum QueryType {
    EQUALS(DataType.DATETIME, DataType.DATE, DataType.TIME, DataType.FLOAT, DataType.INTEGER, DataType.STRING),
    EXISTS(DataType.DATETIME, DataType.DATE, DataType.TIME, DataType.FLOAT, DataType.INTEGER, DataType.STRING),
    FUZZY(DataType.STRING),
    GREATER_THEN(DataType.DATETIME, DataType.DATE, DataType.TIME, DataType.FLOAT, DataType.INTEGER),
    GREATER_THEN_EQUALS(DataType.DATETIME, DataType.DATE, DataType.TIME, DataType.FLOAT, DataType.INTEGER),
    LESS_THEN(DataType.DATETIME, DataType.DATE, DataType.TIME,DataType.FLOAT, DataType.INTEGER),
    LESS_THEN_EQUALS(DataType.DATETIME, DataType.DATE, DataType.TIME,DataType.FLOAT, DataType.INTEGER);

    private List<DataType> dataTypes = new ArrayList<>();

    QueryType(DataType... dataTypes) {
        this.dataTypes = Arrays.asList(dataTypes);
    }

    public List<DataType> getDataTypes() {
        return dataTypes;
    }
}
