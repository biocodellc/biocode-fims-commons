package biocode.fims.query.writers;

import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.query.QueryResult;
import biocode.fims.query.QueryResults;
import biocode.fims.utils.FileUtils;
import biocode.fims.utils.FileUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author RJ Ewing
 */
public class DelimitedTextQueryWriter extends AbstractQueryWriter {
    private final String delimiter;
    private boolean isCsv;

    private Set<String> columns;

    public DelimitedTextQueryWriter(QueryResults queryResults, String delimiter) {
        super(queryResults);
        this.delimiter = delimiter;
        isCsv = StringUtils.equals(delimiter.trim(), ",");
    }

    @Override
    protected File writeResult(QueryResult queryResult) {
        String ext = (isCsv) ? "csv" : "txt";
        File file = FileUtils.createUniqueFile(queryResult.entity().getConceptAlias() + "_output." + ext, System.getProperty("java.io.tmpdir"));

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

                for (String column : columns) {
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
