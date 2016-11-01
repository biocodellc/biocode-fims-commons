package biocode.fims.fileManagers.dataset;

import org.json.simple.JSONArray;

import java.util.List;

/**
 * Domain object to hold datasets
 */
public class Dataset {

    private final List<String> columns;
    // TODO possible don't need this
    private final JSONArray samples;

    public Dataset(List<String> columns, JSONArray samples) {
        this.columns = columns;
        this.samples = samples;
    }

    public List<String> getColumns() {
        return columns;
    }

    public JSONArray getSamples() {
        return samples;
    }

    @Override
    public String toString() {
        return "Dataset{" +
                "columns=" + columns +
                ", samples=" + samples +
                '}';
    }
}
