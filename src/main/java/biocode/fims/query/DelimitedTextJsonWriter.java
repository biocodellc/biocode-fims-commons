package biocode.fims.query;

import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.settings.PathManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.List;

/**
 * Class to write Json to an delimited txt file
 *
 * @author RJ Ewing
 */
public class DelimitedTextJsonWriter implements JsonWriter {
    private final ArrayNode resources;
    private final String outputDirectory;

    private final List<JsonFieldTransform> columns;
    private final String delimiter;
    private boolean writeHeader = true;
    private boolean isCsv;

    /**
     * @param resources       {@link ArrayNode} of {@link ObjectNode}'s to be written to the excel file.
     *                        Each {@link ObjectNode#fields()} may only contain {@link ObjectNode} or basic java data type (String, Integer, int, etc...)
     * @param columns
     * @param outputDirectory
     * @param delimiter
     */
    public DelimitedTextJsonWriter(ArrayNode resources, List<JsonFieldTransform> columns, String outputDirectory,
                                   String delimiter) {
        this.resources = resources;
        this.outputDirectory = outputDirectory;
        this.columns = columns;
        this.delimiter = delimiter;
        isCsv = StringUtils.equals(delimiter.trim(), ",");
    }

    /**
     * @param resources       {@link ArrayNode} of {@link ObjectNode}'s to be written to the excel file.
     *                        Each {@link ObjectNode#fields()} may only contain {@link ObjectNode} or basic java data type (String, Integer, int, etc...)
     * @param columns
     * @param outputDirectory
     * @param delimiter
     * @param writeHeader
     */
    public DelimitedTextJsonWriter(ArrayNode resources, List<JsonFieldTransform> columns, String outputDirectory,
                                   String delimiter, boolean writeHeader) {
        this(resources, columns, outputDirectory, delimiter);
        this.writeHeader = writeHeader;
    }

    @Override
    public File write() {
        File file = PathManager.createUniqueFile("output.txt", outputDirectory);

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file)))) {

            if (writeHeader) {

                for (JsonFieldTransform column : columns) {

                    if (isCsv) {
                        StringEscapeUtils.escapeCsv(writer, column.getFieldName());
                    } else {
                        writer.write(column.getFieldName());
                    }
                    writer.write(delimiter);

                }

                writer.write("\n");
            }

            for (JsonNode resource : resources) {

                for (JsonFieldTransform column : columns) {
                    String val = resource.at(column.getPath()).asText("");
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
