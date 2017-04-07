package biocode.fims.query.writers;

import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.settings.PathManager;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.List;

/**
 * Class to write Json to a kml file
 *
 * @author RJ Ewing
 */
public class KmlJsonWriter implements JsonWriter {
    private final ArrayNode resources;
    private final String outputDirectory;

    private final List<JsonFieldTransform> descriptionColumns;
    private final JsonPointer latPath;
    private final JsonPointer longPath;
    private final JsonPointer namePath;


    public static class KmlJsonWriterBuilder {
        private ArrayNode resources;
        private String outputDirectory;

        private List<JsonFieldTransform> descriptionColumns;
        private JsonPointer latPath;
        private JsonPointer longPath;
        private JsonPointer namePath;

        /**
         * @param resources          {@link ArrayNode} of {@link ObjectNode}'s to be written to the excel file.
         *                           Each {@link ObjectNode#fields()} may only contain {@link ObjectNode} or basic java data type (String, Integer, int, etc...)
         * @param descriptionColumns JsonFieldTransform's to include in the Placemark.description element CDATA.
         *                           max length is 10, any additional JsonWriterColumns are ignored
         */
        public KmlJsonWriterBuilder(ArrayNode resources, String outputDirectory, List<JsonFieldTransform> descriptionColumns) {
            this.resources = resources;
            this.outputDirectory = outputDirectory;
            this.descriptionColumns = descriptionColumns;
        }

        /**
         * @param latPath the {@link JsonPointer} to the resource field to use for the Placemark.Point.coordinates element latitude for each resource in the kml file
         * @return
         */
        public KmlJsonWriterBuilder latPath(JsonPointer latPath) {
            this.latPath = latPath;
            return this;
        }

        /**
         * @param longPath the {@link JsonPointer} to the resource field to use for the Placemark.Point.coordinates element longitude for each resource in the kml file
         * @return
         */
        public KmlJsonWriterBuilder longPath(JsonPointer longPath) {
            this.longPath = longPath;
            return this;
        }

        /**
         * @param namePath the {@link JsonPointer} to the resource field to use for the Placemark.name element for each resource in the kml file
         * @return
         */
        public KmlJsonWriterBuilder namePath(JsonPointer namePath) {
            this.namePath = namePath;
            return this;
        }

        public KmlJsonWriter build() {
            if (latPath == null || longPath == null || namePath == null) {
                throw new FimsRuntimeException("Server Error", "latPath, longPath, and namePath are required.", 500);
            }

            return new KmlJsonWriter(this);
        }
    }

    public KmlJsonWriter(KmlJsonWriterBuilder builder) {
        this.resources = builder.resources;
        this.outputDirectory = builder.outputDirectory;
        this.descriptionColumns = builder.descriptionColumns;
        this.latPath = builder.latPath;
        this.longPath = builder.longPath;
        this.namePath = builder.namePath;
    }

    @Override
    public File write() {
        File file = PathManager.createUniqueFile("output.kml", outputDirectory);

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file)))) {

            startDocument(writer);

            for (JsonNode resource : resources) {
                writePlacemark(writer, (ObjectNode) resource);
            }

            closeDocument(writer);

        } catch (IOException e) {
            throw new FimsRuntimeException(FileCode.WRITE_ERROR, 500);
        }

        return file;
    }

    private void writePlacemark(Writer writer, ObjectNode resource) throws IOException {

        if (!StringUtils.isBlank(resource.at(latPath).asText()) && ! StringUtils.isBlank(resource.at(longPath).asText())) {

            writer.write("\t<Placemark>\n");

            writer.write("\t\t<name>");
            writer.write(resource.at(namePath).asText());
            writer.write("\t\t</name>\n");

            writer.write("\t\t<description>\n");
            writer.write("\t\t<![CDATA[");

            for (JsonFieldTransform column : descriptionColumns) {
                writer.write("<br>");
                StringEscapeUtils.escapeXml(writer, column.getFieldName());
                writer.write("=");
                StringEscapeUtils.escapeXml(writer, resource.at(column.getPath()).asText());
            }

            writer.write("\t\t]]>\n");
            writer.write("\t\t</description>\n");

            writer.write("\t\t<Point>\n");
            writer.write("\t\t\t<coordinates>");
            StringEscapeUtils.escapeXml(writer, resource.at(longPath).asText());
            writer.write(",");
            StringEscapeUtils.escapeXml(writer, resource.at(latPath).asText());
            writer.write("</coordinates>\n");
            writer.write("\t\t</Point>\n");

            writer.write("\t</Placemark>\n");
        }

    }

    private void startDocument(Writer writer) throws IOException {
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                "\t<Document>\n");
    }

    private void closeDocument(Writer writer) throws IOException {
        writer.write("</Document>\n" +
                "</kml>");
    }

}
