package biocode.fims.query;

import biocode.fims.digester.Field;
import biocode.fims.digester.Validation;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.settings.PathManager;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Class to write Json to a cspace file. Currently just copying this over from QueryWriter class. This currently only
 * works for UCJeps project
 * <p>
 * TODO: Pass in a CspaceFields object similar to the FastaSequenceFields object which contains all the cspace fields.
 * TODO: We can generify the cspace fields by using the defined_by Attribute attribute
 *
 * @author RJ Ewing
 */
public class CspaceJsonWriter implements JsonWriter {
    private final ArrayNode resources;
    private final String outputDirectory;
    private final Validation validation;

    public CspaceJsonWriter(ArrayNode resources, String outputDirectory, Validation validation) {
        this.resources = resources;
        this.outputDirectory = outputDirectory;
        this.validation = validation;
    }

    @Override
    public File write() {
        File file = PathManager.createUniqueFile("output.cspace.xml", outputDirectory);

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file)))) {

            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<imports>\n");

            for (JsonNode resource : resources) {

                //Variables pertaining to row level only
                String year = "", month = "", day = "", taxon = "", identBy = "", date = "";
                String identyear = "", identmonth = "", identday = "", identdate = "";
                String Locality = "", Country = "", State_Province = "", County = "", Elevation = "", Elevation_Units = "", Latitude = "", Longitude = "", Coordinate_Source = "";
                StringBuilder common = new StringBuilder();
                StringBuilder naturalhistory = new StringBuilder();

                writer.write("<import service='CollectionObjects' type='CollectionObject'>\n");

                Iterator<String> iterator = resource.fieldNames();
                while (iterator.hasNext()) {
                    String fieldName = iterator.next();
                    String value = resource.get(fieldName).asText();

                    // Write out XML Values
                    if (fieldName.equals("bcid")) {
                        // check if resolution mechanism is attached
                        if (!value.contains("http")) {
                            value = "http://n2t.net/" + value;
                        }
                        common.append("\t<otherNumberList>\n" + "\t\t<otherNumber>\n" + "\t\t\t")
                                .append(writeXMLValue("numberValue", value))
                                .append("\n")
                                .append("\t\t\t<numberType>FIMS Identifier</numberType>\n")
                                .append("\t\t</otherNumber>\n")
                                .append("\t</otherNumberList>\n");
                    } else if (fieldName.equals("urn:habitat")) {
                        common.append("\t" + writeXMLValue("fieldCollectionNote", value) + "\n");
                    } else if (fieldName.equals("urn:barcodenumber")) {
                        common.append("\t" + writeXMLValue("objectNumber", value) + "\n");
                    } else if (fieldName.equals("urn:all_collectors")) {
                        common.append("\t<fieldCollectors>\n");
                        common.append("\t\t" + writeXMLValue("fieldCollector", fieldURILookup("Collector", value, validation)) + "\n");
                        common.append("\t</fieldCollectors>\n");
                    } else if (fieldName.equals("urn:coll_num")) {
                        common.append("\t" + writeXMLValue("fieldCollectionNumber", value) + "\n");
                    } else if (fieldName.equals("urn:coll_year")) {
                        year = value;
                    } else if (fieldName.equals("urn:coll_Month")) {
                        month = value;
                    } else if (fieldName.equals("urn:coll_day")) {
                        day = value;
                    } else if (fieldName.equals("urn:coll_date")) {
                        date = value;
                    } else if (fieldName.equals("urn:det_date_display")) {
                        identdate = value;
                    } else if (fieldName.equals("urn:brief_desc")) {
                        common.append("\t<briefDescriptions>\n" +
                                "\t\t" + writeXMLValue("briefDescription", value) + "\n" +
                                "\t</briefDescriptions>\n");
                    } else if (fieldName.equals("urn:label_header")) {
                        naturalhistory.append("\t" + writeXMLValue("labelHeader", fieldURILookup("Label_Header", value, validation)) + "\n");
                    } else if (fieldName.equals("urn:main_collector")) {
                        naturalhistory.append("\t" + writeXMLValue("fieldCollectionNumberAssignor", fieldURILookup("Collector", value, validation)) + "\n");
                    } else if (fieldName.equals("urn:scientificname")) {
                        taxon = writeXMLValue("taxon", fieldURILookup("ScientificName", value, validation));
                    } else if (fieldName.equals("urn:determinedby")) {
                        identBy = writeXMLValue("identBy", fieldURILookup("DeterminedBy", value, validation));
                    } else if (fieldName.equals("urn:comments")) {
                        common.append("\t<comments>\n");
                        common.append("\t\t" + writeXMLValue("comment", value) + "\n");
                        common.append("\t</comments>\n");
                    } else if (fieldName.equals("urn:det_year")) {
                        identyear = value;
                    } else if (fieldName.equals("urn:det_month")) {
                        identmonth = value;
                    } else if (fieldName.equals("urn:det_day")) {
                        identday = value;
                    } else if (fieldName.equals("urn:locality")) {
                        Locality = value;
                    } else if (fieldName.equals("urn:country")) {
                        Country = value;
                    } else if (fieldName.equals("urn:stateprovince")) {
                        State_Province = value;
                    } else if (fieldName.equals("urn:county")) {
                        County = value;
                    } else if (fieldName.equals("urn:elevation")) {
                        Elevation = value;
                    } else if (fieldName.equals("urn:elevation_units")) {
                        Elevation_Units = value;
                    } else if (fieldName.equals("urn:latitude")) {
                        Latitude = value;
                    } else if (fieldName.equals("urn:longitude")) {
                        Longitude = value;
                    } else if (fieldName.equals("urn:coordinate_source")) {
                        Coordinate_Source = value;
                    } else {
                        // All the biocode.fims.rest of the values
                        //naturalhistory.append("\t" + writeXMLValue(fieldName, value) + "\n");
                    }

                }

                //Set dates to their parts if they are provided
                // The following code will only parse dates if all of year, month, day are provided in the
                // correct format.
                if (!date.equals("")) {
                    String[] parts = date.split("-");
                    if (parts.length > 2) {
                        year = parts[0];
                        month = parts[1];
                        day = parts[2];
                    }

                }
                if (!identdate.equals("")) {
                    String[] parts = identdate.split("-");
                    if (parts.length > 2) {
                        identyear = parts[0];
                        identmonth = parts[1];
                        identday = parts[2];
                    }
                }

                // fieldCollectionDateGroup
                common.append(displayCSPACEDate("fieldCollectionDateGroup", date, year, month, day, 2));

                // TaxonomicIdentGroup
                naturalhistory.append("\t<taxonomicIdentGroupList>\n" +
                        "\t\t<taxonomicIdentGroup>\n" +
                        "\t\t\t" + taxon + "\n" +
                        "\t\t\t<qualifier></qualifier>\n" +
                        "\t\t\t" + identBy + "\n");
                naturalhistory.append(displayCSPACEDate("identDateGroup", date, year, month, day, 3));
                naturalhistory.append("\t\t</taxonomicIdentGroup>\n" +
                        "\t</taxonomicIdentGroupList>\n");

                naturalhistory.append("\t<localityGroupList>\n" +
                        "\t\t<localityGroup>\n" +
                        "\t\t\t" + writeXMLValue("fieldLocVerbatim", Locality) + "\n" +
                        "\t\t\t" + writeXMLValue("fieldLocCountry", Country) + "\n" +
                        "\t\t\t" + writeXMLValue("fieldLocState", State_Province) + "\n" +
                        "\t\t\t" + writeXMLValue("fieldLocCounty", County) + "\n" +
                        "\t\t\t" + writeXMLValue("minElevation", Elevation) + "\n" +
                        "\t\t\t" + writeXMLValue("elevationUnit", Elevation_Units) + "\n" +
                        "\t\t\t" + writeXMLValue("decimalLatitude", Latitude) + "\n" +
                        "\t\t\t" + writeXMLValue("decimalLongitude", Longitude) + "\n" +
                        "\t\t\t" + writeXMLValue("vLatitude", Latitude) + "\n" +
                        "\t\t\t" + writeXMLValue("vLongitude", Longitude) + "\n" +
                        "\t\t\t" + writeXMLValue("geoRefSource", Coordinate_Source) + "\n" +
                        "\t\t\t" + writeXMLValue("localitySource", Coordinate_Source) + "\n" +
                        "\t\t</localityGroup>\n" +
                        "\t</localityGroupList>\n");

                // collectionobjects_common element
                writer.write("<schema xmlns:collectionobjects_common=\"http://collectionspace.org/services/collectionobject\" name=\"collectionobjects_common\">\n");
                writer.write(common.toString());
                writer.write("</schema>\n");

                // collectionobjects_naturalhistory element
                writer.write("<schema xmlns:collectionobjects_naturalhistory=\"http://collectionspace.org/services/collectionobject/domain/naturalhistory\" name=\"collectionobjects_naturalhistory\">\n");
                writer.write(naturalhistory.toString());
                writer.write("</schema>\n");

                // End of this object/row
                writer.write("</import>\n");

            }

            // closing document tag
            writer.write("</imports>\n");


        } catch (IOException e) {
            throw new FimsRuntimeException(FileCode.WRITE_ERROR, 500);
        }

        return file;

    }

    private String displayCSPACEDate(String dateGroup, String date, String year, String month, String day,
                                     int tabStops) {
        // Set the appropriate number of tabs
        String tabs = "";
        for (int i = 0; i < tabStops; i++) {
            tabs += "\t";
        }

        StringBuilder common = new StringBuilder();
        // Field Date Group
        common.append(tabs + "<" + dateGroup + ">\n");
        if (!date.trim().equals("")) {
            common.append(tabs + "\t<dateDisplayDate>" + date + "</dateDisplayDate>\n");
        } else {
            common.append(tabs + "\t<dateDisplayDate>" + year + "-" + month + "-" + day + "</dateDisplayDate>\n");
        }
        if (!day.trim().equals("")) {
            common.append(tabs + "\t<dateEarliestSingleDay>" + day + "</dateEarliestSingleDay>\n");
        }
        if (!month.trim().equals("")) {
            common.append(tabs + "\t<dateEarliestSingleMonth>" + month + "</dateEarliestSingleMonth>\n");
        }
        if (!year.trim().equals("")) {
            common.append(tabs + "\t<dateEarliestSingleYear>" + year + "</dateEarliestSingleYear>\n");
        }
        if (!year.trim().equals("") && !month.trim().equals("") && !day.trim().equals("")) {
            common.append(tabs + "\t<dateEarliestScalarValue>" + year + "-" + month + "-" + day + "T00:00:00Z</dateEarliestScalarValue>\n");
        }
        common.append(tabs + "</" + dateGroup + ">\n");

        return common.toString();
    }

    /**
     * Special function built for CSPACE case that looks up the Field.uri and appends Field.value to it
     *
     * @param fieldName
     * @param value
     * @return
     */

    private String fieldURILookup(String fieldName, String value, Validation validation) {
        // Loop XML attribute value of ScientificName to get the REFNAME
        for (biocode.fims.digester.List l : validation.getLists()) {
            if (l.getAlias().equals(fieldName)) {

                for (Field f : l.getFields()) {
                    if (f.getValue().equals(value)) {
                        return f.getUri() + "'" + f.getValue() + "'";
                    }
                }
            }
        }
        return value;
    }

    private String writeXMLValue(String field, String value) {
        if (value == null || value.trim().equals("")) {
            return "<" + field + "/>";
        }
        if (StringUtils.containsAny(value, "<>&")) {
            return "<" + field + "><![CDATA[" + value + "]]></" + field + ">";
        } else {
            return "<" + field + ">" + value + "</" + field + ">";
        }

    }
}
