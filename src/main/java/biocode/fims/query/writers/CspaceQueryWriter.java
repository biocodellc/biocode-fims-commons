package biocode.fims.query.writers;

import biocode.fims.digester.Field;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.query.QueryResult;
import biocode.fims.settings.PathManager;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * Currently just copying this over from QueryWriter class. This currently only works for UCJeps project
 * <p>
 * TODO: Pass in a CspaceFields object similar to the FastaSequenceFields object which contains all the cspace fields.
 * TODO: We can generify the cspace fields by using the defined_by Attribute attribute
 *
 * @author RJ Ewing
 */
public class CspaceQueryWriter implements QueryWriter {

    private final QueryResult queryResult;
    private final ProjectConfig projectConfig;

    public CspaceQueryWriter(QueryResult queryResult, ProjectConfig projectConfig) {
        this.queryResult = queryResult;
        this.projectConfig = projectConfig;
    }

    @Override
    public File write() {
        File file = PathManager.createUniqueFile("output.cspace.xml", System.getProperty("java.io.tmpdir"));

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file)))) {

            List<Map<String, String>> records = queryResult.get(true);

            if (records.size() == 0) {
                throw new FimsRuntimeException(QueryCode.NO_RESOURCES, 400);
            }

            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<imports>\n");

            for (Map<String, String> record : records) {

                //Variables pertaining to row level only
                String year = "", month = "", day = "", taxon = "", identBy = "", date = "";
                String identyear = "", identmonth = "", identday = "", identdate = "";
                String Locality = "", Country = "", State_Province = "", County = "", Elevation = "", Elevation_Units = "", Latitude = "", Longitude = "", Coordinate_Source = "";
                StringBuilder common = new StringBuilder();
                StringBuilder naturalhistory = new StringBuilder();

                writer.write("<import service='CollectionObjects' type='CollectionObject'>\n");

                for (Map.Entry<String, String> e : record.entrySet()) {
                    String fieldName = e.getKey();
                    String value = e.getValue();

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
                    } else if (fieldName.equals("Habitat")) {
                        common.append("\t" + writeXMLValue("fieldCollectionNote", value) + "\n");
                    } else if (fieldName.equals("Barcode_Number")) {
                        common.append("\t" + writeXMLValue("objectNumber", value) + "\n");
                    } else if (fieldName.equals("All_Collectors")) {
                        common.append("\t<fieldCollectors>\n");
                        common.append("\t\t" + writeXMLValue("fieldCollector", fieldURILookup("Collector", value)) + "\n");
                        common.append("\t</fieldCollectors>\n");
                    } else if (fieldName.equals("Coll_Num")) {
                        common.append("\t" + writeXMLValue("fieldCollectionNumber", value) + "\n");
                    } else if (fieldName.equals("Coll_Year")) {
                        year = value;
                    } else if (fieldName.equals("Coll_Month")) {
                        month = value;
                    } else if (fieldName.equals("Coll_Day")) {
                        day = value;
                    } else if (fieldName.equals("Coll_Date")) {
                        date = value;
                    } else if (fieldName.equals("Det_Date_Display")) {
                        identdate = value;
                    } else if (fieldName.equals("Plant_Description")) {
                        common.append("\t<briefDescriptions>\n" +
                                "\t\t" + writeXMLValue("briefDescription", value) + "\n" +
                                "\t</briefDescriptions>\n");
                    } else if (fieldName.equals("Label_Header")) {
                        naturalhistory.append("\t" + writeXMLValue("labelHeader", fieldURILookup("Label_Header", value)) + "\n");
                    } else if (fieldName.equals("Main_Collector")) {
                        naturalhistory.append("\t" + writeXMLValue("fieldCollectionNumberAssignor", fieldURILookup("Collector", value)) + "\n");
                    } else if (fieldName.equals("ScientificName")) {
                        taxon = writeXMLValue("taxon", fieldURILookup("ScientificName", value));
                    } else if (fieldName.equals("DeterminedBy")) {
                        identBy = writeXMLValue("identBy", fieldURILookup("DeterminedBy", value));
                    } else if (fieldName.equals("Comments")) {
                        common.append("\t<comments>\n");
                        common.append("\t\t" + writeXMLValue("comment", value) + "\n");
                        common.append("\t</comments>\n");
                    } else if (fieldName.equals("Det_Year")) {
                        identyear = value;
                    } else if (fieldName.equals("Det_Month")) {
                        identmonth = value;
                    } else if (fieldName.equals("Det_Day")) {
                        identday = value;
                    } else if (fieldName.equals("Locality")) {
                        Locality = value;
                    } else if (fieldName.equals("Country")) {
                        Country = value;
                    } else if (fieldName.equals("State_Province")) {
                        State_Province = value;
                    } else if (fieldName.equals("County")) {
                        County = value;
                    } else if (fieldName.equals("Elevation")) {
                        Elevation = value;
                    } else if (fieldName.equals("Elevation_Units")) {
                        Elevation_Units = value;
                    } else if (fieldName.equals("Latitude")) {
                        Latitude = value;
                    } else if (fieldName.equals("Longitude")) {
                        Longitude = value;
                    } else if (fieldName.equals("Coordinate_Source")) {
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

    private String fieldURILookup(String fieldName, String value) {
        // Loop XML attribute value of ScientificName to get the REFNAME
        biocode.fims.digester.List l = projectConfig.findList(fieldName);

        if (l == null) {
            return value;
        }

        for (Field f : l.getFields()) {
            if (f.getValue().equals(value)) {
                return f.getUri() + "'" + f.getValue() + "'";
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
