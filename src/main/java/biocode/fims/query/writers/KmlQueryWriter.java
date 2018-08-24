package biocode.fims.query.writers;

import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.query.QueryResult;
import biocode.fims.utils.FileUtils;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author RJ Ewing
 */
public class KmlQueryWriter implements QueryWriter {
    private final QueryResult queryResult;
    private final String latColumn;
    private final String lngColumn;
    private final String nameColumn;

    private KmlQueryWriter(Builder builder) {
        this.queryResult = builder.queryResult;
        this.latColumn = builder.latColumn;
        this.lngColumn = builder.lngColumn;
        this.nameColumn = builder.nameColumn;
    }

    @Override
    public List<File> write() {
        File file = FileUtils.createUniqueFile("output.kml", System.getProperty("java.io.tmpdir"));

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file)))) {

            List<Map<String, String>> records = queryResult.get(false);

            if (records.size() == 0) {
                throw new FimsRuntimeException(QueryCode.NO_RESOURCES, 400);
            }

            startDocument(writer);

            for (Map<String, String> record: records) {
                writePlacemark(writer, record);
            }

            closeDocument(writer);

        } catch (IOException e) {
            throw new FimsRuntimeException(FileCode.WRITE_ERROR, 500);
        }

        return Collections.singletonList(file);
    }

    private void writePlacemark(Writer writer, Map<String, String> record) throws IOException {
        String lat = record.getOrDefault(latColumn, "");
        String lng = record.getOrDefault(lngColumn, "");

        if (!lat.trim().equals("") || !lng.trim().equals("")) {

            writer.write("\t<Placemark>\n");

            writer.write("\t\t<name>");
            writer.write(record.getOrDefault(nameColumn, ""));
            writer.write("\t\t</name>\n");

            writer.write("\t\t<description>\n");
            writer.write("\t\t<![CDATA[");

            for (Map.Entry<String, String> e: record.entrySet()) {
                if (!e.getKey().equals(latColumn) || !e.getKey().equals(lngColumn) || !e.getKey().equals(nameColumn)) {
                    writer.write("<br>");
                    StringEscapeUtils.escapeXml(writer, e.getKey());
                    writer.write("=");
                    StringEscapeUtils.escapeXml(writer, e.getValue());
                }
            }

            writer.write("\t\t]]>\n");
            writer.write("\t\t</description>\n");

            writer.write("\t\t<Point>\n");
            writer.write("\t\t\t<coordinates>");
            StringEscapeUtils.escapeXml(writer, lng);
            writer.write(",");
            StringEscapeUtils.escapeXml(writer, lat);
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

    public static class Builder {
        private final QueryResult queryResult;

        private String latColumn;
        private String lngColumn;
        private String nameColumn;


        public Builder(QueryResult queryResult) {
            this.queryResult = queryResult;
        }

        /**
         * @param col the record column referencing the latitude of the record. Used for the
         *            Placemark.Point.coordinates element latitude for each record in the kml file
         */
        public Builder latColumn(String col) {
            this.latColumn = col;
            return this;
        }

        /**
         * @param col the record column referencing the longitude of the record. Used for the
         *            Placemark.Point.coordinates element longitude for each record in the kml file
         */
        public Builder lngColumn(String col) {
            this.lngColumn = col;
            return this;
        }

        /**
         * @param col the record column to use for the Placemark.name elment for each record in the kml file
         */
        public Builder nameColumn(String col) {
            this.nameColumn = col;
            return this;
        }

        public KmlQueryWriter build() {
            if (latColumn == null || lngColumn == null || nameColumn == null) {
                throw new FimsRuntimeException("Server Error", "latColumn, lngColumn, and nameColumn are required.", 500);
            }

            return new KmlQueryWriter(this);
        }
    }

}
