package biocode.fims.reader;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.DataType;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.fimsExceptions.errorCodes.ValidationCode;
import biocode.fims.settings.PathManager;
import biocode.fims.utils.DateUtils;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;


/**
 * Takes a data source represented by a JSONArray and converts it to a
 * CSV file
 */
public final class CsvJsonConverter {
    private final JSONArray dataset;
    private final String filenamePrefix;
    private final String outputDir;

    private File csvFile;

    private static Logger logger = LoggerFactory.getLogger(CsvJsonConverter.class);

    /**
     * @param dataset
     * @param outputDir A valid filepath for the new csv file
     * @param filenamePrefix
     */
    public CsvJsonConverter(JSONArray dataset, String outputDir, String filenamePrefix) {
        this.dataset = dataset;
        this.outputDir = outputDir;
        this.filenamePrefix = filenamePrefix;
    }


    /**
     * converts the dataset resources to a csv file
     */
    public void convert(List<Attribute> attributes) {
        if (dataset.isEmpty()) {
            throw new FimsRuntimeException(ValidationCode.EMPTY_DATASET, 400);
        }

        // For storing data into CSV files
        StringBuffer data = new StringBuffer();
        csvFile = PathManager.createFile(filenamePrefix + ".csv", outputDir);

        try {
            FileOutputStream fos = new FileOutputStream(csvFile);

            for (Object s : dataset) {
                JSONObject resource = (JSONObject) s;

                for (Attribute a : attributes) {
                    if (resource.containsKey(a.getColumn())) {
                        String value = (String) resource.get(a.getColumn());
                        // convert dates to ISO 8601 format
                        if (!StringUtils.isBlank(value) && (a.getDatatype() == DataType.DATETIME || a.getDatatype() == DataType.DATE ||
                                a.getDatatype() == DataType.TIME)) {
                            value = DateUtils.convertDateToFormat(value, a.getDataformat().split(",")[0], a.getDataformat().split(","));

                        }

                        data.append(value + ",");
                    } else {
                        // if the column doesn't exist in the dataset, add a placeholder in the csv file.
                        // This is required because we later use the attributes list to insert the csv data
                        // into the db. If a column in missing from the dataset and a placeholder isn't entered in the
                        // csv, then the csv data becomes mis-aligned with attributes list, causing invalid data
                        // to be persisted
                        data.append(",");
                    }
                }

                data.append('\n');
            }

            fos.write(data.toString().getBytes());
            fos.close();

        } catch (IOException ioe) {
            throw new ServerErrorException(ioe);
        }
    }

    public File getCsvFile() {
        return csvFile;
    }

}
