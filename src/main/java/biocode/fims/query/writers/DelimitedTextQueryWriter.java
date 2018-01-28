package biocode.fims.query.writers;

import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.query.QueryResult;
import biocode.fims.settings.PathManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.*;

/**
 * @author RJ Ewing
 */
public class DelimitedTextQueryWriter implements QueryWriter {
    private final QueryResult queryResult;
    private final String delimiter;
    private boolean isCsv;

    private Set<String> columns;

    public DelimitedTextQueryWriter(QueryResult queryResult, String delimiter) {
        this.queryResult = queryResult;
        this.delimiter = delimiter;
        isCsv = StringUtils.equals(delimiter.trim(), ",");
    }

    @Override
    public File write() {
        File file = PathManager.createUniqueFile("output.txt", System.getProperty("java.io.tmpdir"));

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file)))) {

            List<Map<String, String>> records = queryResult.get(true);

            if (records.size() == 0) {
                throw new FimsRuntimeException(QueryCode.NO_RESOURCES, 400);
            }

            this.columns = records.get(0).keySet();

            for (String column : columns) {
                if (isCsv) {
                    StringEscapeUtils.escapeCsv(writer, column);
                } else {
                    writer.write(column);
                }

                writer.write(delimiter);

            }

            writer.write("\n");

            for (Map<String, String> record : records) {

                for (String column: columns) {
                    String val = record.getOrDefault(column, "");
                    if (isCsv) {
                        StringEscapeUtils.escapeCsv(writer, val);
                    } else {
                        writer.write(val);
                    }
                    writer.write(delimiter);
                }

                writer.write("\n");

            }

        } catch (IOException e) {
            throw new FimsRuntimeException(FileCode.WRITE_ERROR, 500);
        }

        return file;
    }
}