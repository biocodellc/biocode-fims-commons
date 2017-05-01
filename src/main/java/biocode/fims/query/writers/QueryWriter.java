package biocode.fims.query.writers;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.DataType;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.rest.SpringObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Loop a bunch of attributes, queried from some model and write to a spreadsheet
 * Keep track of columns we want to display, starting with an ArrayList of attributes, corresponding
 * to column names and tracking URI references as defined by the Mapping digester class.
 */
public class QueryWriter {
    // Loop all the columns associated with this worksheet
    List<Attribute> attributes;
    ArrayList extraColumns;
    Integer totalColumns;
    XSSFWorkbook wb = new XSSFWorkbook();
    Sheet sheet;
    private static Logger logger = LoggerFactory.getLogger(QueryWriter.class);

    /**
     * @param attributes ArrayList of attributes passed as argument is meant to come from digester.Mapping instance
     */
    public QueryWriter(List<Attribute> attributes, String sheetName) {
        this.attributes = attributes;
        totalColumns = attributes.size() - 1;
        extraColumns = new ArrayList();

        sheet = wb.createSheet(sheetName);
    }

    /**
     * Find the column position for this array
     *
     * @param columnName
     * @return
     */
    public Integer getColumnPosition(String columnName) {
        Iterator it = attributes.iterator();
        int count = 0;
        while (it.hasNext()) {
            Attribute attribute = (Attribute) it.next();
            if (columnName.equals(attribute.getColumn())) {
                return count;
            }
            count++;
        }

        // Track any extra columns we find
        Iterator itExtraColumns = extraColumns.iterator();
        // position counter at end of known columns
        int positionExtraColumn = attributes.size();
        // loop the existing extracolumns array, looking for matches, returning position if found
        while (itExtraColumns.hasNext()) {
            String col = (String) itExtraColumns.next();
            if (col.equals(columnName)) {
                return positionExtraColumn;
            }
            positionExtraColumn++;
        }

        // If we don't find it then add it to the extracolumns
        extraColumns.add(columnName);
        totalColumns++;
        return totalColumns;
    }

    /**
     * Create a header row for all columns (initial + extra ones encountered)
     *
     * @param sheet
     * @return
     */
    public org.apache.poi.ss.usermodel.Row createHeaderRow(Sheet sheet) {
        org.apache.poi.ss.usermodel.Row row = sheet.createRow((short) 0);

        Iterator it = attributes.iterator();
        int count = 0;
        while (it.hasNext()) {
            Attribute attribute = (Attribute) it.next();
            String colName = attribute.getColumn();
            row.createCell(count).setCellValue(colName);
            count++;
        }

        Iterator itExtraColumns = extraColumns.iterator();

        while (itExtraColumns.hasNext()) {
            String colName = (String) itExtraColumns.next();
            row.createCell(count).setCellValue(colName);
            count++;
        }

        return row;
    }


    /**
     * create a row at a specified index
     *
     * @param rowNum
     * @return
     */
    public Row createRow(int rowNum) {
        // make ALL rows one more than the expected rowNumber to account for the header row
        return sheet.createRow(rowNum + 1);
    }

    /**
     * Remove a row by its index
     *
     * @param rowIndex a 0 based index of removing row
     */
    public void removeRow(int rowIndex) {

        // account for header row
        int rowtoRemove = rowIndex + 1;

        int lastRowNum = sheet.getLastRowNum();
        if (rowtoRemove >= 0 && rowtoRemove < lastRowNum) {
            sheet.shiftRows(rowtoRemove + 1, lastRowNum, -1);
        }

        if (rowIndex == lastRowNum) {
            org.apache.poi.ss.usermodel.Row removingRow = sheet.getRow(rowtoRemove);
            if (removingRow != null) {
                sheet.removeRow(removingRow);
            }
        }
    }

    /**
     * Write data to a particular cell given the row/column(predicate) and a value
     *
     * @param row
     * @param predicate
     * @param value
     */
    public void createCell(Row row, String predicate, String value) {
        String colName = predicate;
        //System.out.println(colName);
        DataType datatype = null;
        // Loop attributes and use column names instead of URI value in column position lookups
        Iterator it = attributes.iterator();
        while (it.hasNext()) {
            Attribute attribute = (Attribute) it.next();
            // map column names to datatype
            try {
                if (attribute.getUri().equals(predicate)) {
                    colName = attribute.getColumn();
                    datatype = attribute.getDatatype();
                }

            } catch (Exception e) {
                //TODO should we be catching Exception?
                logger.error("Exception", e);
                // For now, do nothing.
            }
        }


        Cell cell = row.createCell(getColumnPosition(colName));

        // Set the value conditionally, we can specify datatypes in the configuration file so interpret them
        // as appropriate here.
        // TODO handle other DataTypes?
        if (datatype != null && (
                datatype.equals(DataType.INTEGER) || datatype.equals(DataType.FLOAT))) {
            //fimsPrinter.out.println("value = " + value);
            //Its a number(int or float).. Excel treats both as numeric
            XSSFCellStyle style = (XSSFCellStyle) wb.createCellStyle();
            try {
                if (datatype.equals(DataType.INTEGER)) {
                    style.setDataFormat(HSSFDataFormat.getBuiltinFormat("0"));
                    cell.setCellValue(Integer.parseInt(value));
                    cell.setCellStyle(style);
                } else {
                    style.setDataFormat(HSSFDataFormat.getBuiltinFormat("0.0"));
                    cell.setCellStyle(style);
                    // even though this is a float datatype, cell.setCellValue only accepts double values.
                    // if we pass a float value, it will be cast to double, and incorrect "percision" is added,
                    // giving us a different value
                    cell.setCellValue(Double.parseDouble(value));
                }
            } catch (NumberFormatException e) {
                logger.warn("error converting {} to float value", value);
                cell.setCellValue(value);
            }
        } else {
            cell.setCellValue(value);
        }
    }

    /**
     * Write output to a file
     */
    public String writeExcel(File file) {
        // Header Row
        createHeaderRow(sheet);
        // Write the output to a file
        FileOutputStream fileOut = null;
        try {
            //File file = new File(fileLocation);
            fileOut = new FileOutputStream(file);

            //File outputFile = t.createExcelFile("Samples", "tripleOutput", a);

            wb.write(fileOut);

            fileOut.close();
            return file.getAbsolutePath();
        } catch (FileNotFoundException e) {
            throw new FimsRuntimeException(500, e);
        } catch (IOException e) {
            throw new FimsRuntimeException(500, e);
        }
    }

    /**
     * This returns the query results as a ArrayNode of ObjectNodes where each ObjectNode contains "field":"value" pairs
     *
     * @return
     */
    public ArrayNode getJSON() {
        ArrayNode dataset = new SpringObjectMapper().createArrayNode();

        // Iterate through the rows.
        for (Row row : sheet) {
            ObjectNode resource = dataset.addObject();

            for (int index = 0; index < attributes.size(); index++) {
                Cell cell = row.getCell(index, Row.CREATE_NULL_AS_BLANK);
                Object value = null;

                if (cell != null) {

                    switch (cell.getCellType()) {

                        case Cell.CELL_TYPE_STRING:
                            value = cell.getRichStringCellValue().getString();
                            break;

                        case Cell.CELL_TYPE_NUMERIC:
                            if (DateUtil.isCellDateFormatted(cell)) {
                                value = cell.getDateCellValue();
                            } else {
                                value = cell.getNumericCellValue();
                                // excel will return numbers as double. If it is an integer, get the int value
                                if (attributes.get(index).getDatatype() == DataType.INTEGER) {
                                    value = ((Double) value).intValue();
                                }
                            }
                            break;

                        case Cell.CELL_TYPE_BOOLEAN:
                            value = cell.getBooleanCellValue();
                            break;

                        case Cell.CELL_TYPE_FORMULA:
                            value = cell.getCellFormula();
                            break;

                        case Cell.CELL_TYPE_BLANK:
                            value = "";
                            break;

                        default:
                            value = cell.toString();

                    }
                }

                resource.put(attributes.get(index).getColumn(), String.valueOf(value));
            }
        }
        return dataset;
    }

    /**
     * Return the default sheet used for processing
     *
     * @return
     */

    public XSSFWorkbook getWorkbook() {
        return wb;
    }

    public String writeHTML(File file) {
        StringBuilder sbHeader = new StringBuilder();
        StringBuilder sb = new StringBuilder();
        // Header Row
        createHeaderRow(sheet);

        // Iterate through the rows.
        int count = 0;
        int LIMIT = 10000;
        for (Row row : sheet) {
            if (count < LIMIT) {

                StringBuilder sbRow = new StringBuilder();

                for (int cn = 0; cn < row.getLastCellNum(); cn++) {
                    Cell cell = row.getCell(cn, Row.CREATE_NULL_AS_BLANK);
                    if (cell == null) {
                        sbRow.append("<td></td>");

                    } else {
                        switch (cell.getCellType()) {
                            case Cell.CELL_TYPE_STRING:
                                sbRow.append("<td>" + cell.getRichStringCellValue().getString() + "</td>");
                                break;
                            case Cell.CELL_TYPE_NUMERIC:
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    sbRow.append("<td>" + cell.getDateCellValue() + "</td>");
                                } else {
                                    sbRow.append("<td>" + cell.getNumericCellValue() + "</td>");
                                }
                                break;
                            case Cell.CELL_TYPE_BOOLEAN:
                                sbRow.append("<td>" + cell.getBooleanCellValue() + "</td>");
                                break;
                            case Cell.CELL_TYPE_FORMULA:
                                sbRow.append("<td>" + cell.getCellFormula() + "</td>");
                                break;
                            default:
                                sbRow.append("<td>" + cell.toString() + "</td>");
                        }
                    }

                }
                if (count == 0) {
                    sbHeader.append("<tr>\n");
                    sbHeader.append("\t" + sbRow + "\n");
                    sbHeader.append("</tr>\n");
                } else {
                    sb.append("<tr>\n");
                    sb.append("\t" + sbRow + "\n");
                    sb.append("\t<tr>\n");
                }
            }
            count++;
        }


        return writeFile("<table border=1>\n" + sbHeader.toString() + sb.toString() + "</table>", file);
    }

    private String writeFile(String content, File file) {

        FileOutputStream fop = null;

        try {
            fop = new FileOutputStream(file);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            // get the content in bytes
            byte[] contentInBytes = content.getBytes();

            fop.write(contentInBytes);
            fop.flush();
            fop.close();

        } catch (IOException e) {
            logger.warn("IOException", e);
        } finally {
            try {
                if (fop != null) {
                    fop.close();
                }
            } catch (IOException e) {
                logger.warn("IOException", e);
            }
        }
        return file.getAbsolutePath();
    }


    public String writeKML(File file) {
        createHeaderRow(sheet);

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                "\t<Document>\n");


        // Iterate through the rows.
        ArrayList rows = new ArrayList();
        for (Iterator<Row> rowsIT = sheet.rowIterator(); rowsIT.hasNext(); ) {
            Row row = rowsIT.next();
            //JSONObject jRow = new JSONObject();

            // Iterate through the cells.
            ArrayList cells = new ArrayList();
            for (Iterator<Cell> cellsIT = row.cellIterator(); cellsIT.hasNext(); ) {
                Cell cell = cellsIT.next();
                /* if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC)
                cells.add(cell.getNumericCellValue());
            else
                cells.add(cell.getStringCellValue());
                */
                cells.add(cell);
            }
            rows.add(cells);
        }

        Iterator rowsIt = rows.iterator();
        int count = 0;

        /*   <?xml version="1.0" encoding="UTF-8"?>
        <kml xmlns="http://www.opengis.net/kml/2.2">
          <Document>
            <Placemark>
              <name>CDATA example</name>
              <description>
                <![CDATA[
                  <h1>CDATA Tags are useful!</h1>
                  <p><font color="red">Text is <i>more readable</i> and
                  <b>easier to write</b> when you can avoid using entity
                  references.</font></p>
                ]]>
              </description>
              <Point>
                <coordinates>102.595626,14.996729</coordinates>
              </Point>
            </Placemark>
          </Document>
        </kml>*/
        while (rowsIt.hasNext()) {
            ArrayList cells = (ArrayList) rowsIt.next();
            Iterator cellsIt = cells.iterator();

            // don't take the first row, its a header.
            if (count > 1) {
                StringBuilder header = new StringBuilder();

                StringBuilder description = new StringBuilder();
                StringBuilder name = new StringBuilder();

                header.append("\t<Placemark>\n");
                description.append("\t\t<description>\n");
                String decimalLatitude = null;
                String decimalLongitude = null;
                description.append("\t\t<![CDATA[");

                int fields = 0;
                // take all the fields
                while (cellsIt.hasNext()) {
                    Cell c = (Cell) cellsIt.next();
                    Integer index = c.getColumnIndex();
                    String value = c.toString();
                    String fieldname = sheet.getRow(0).getCell(index).toString();

                    //Only take the first 10 fields for data....
                    if (fields < 10)
                        description.append("<br>" + fieldname + "=" + value);

                    if (fieldname.equalsIgnoreCase("decimalLatitude") && !value.equals(""))
                        decimalLatitude = value;
                    if (fieldname.equalsIgnoreCase("decimalLongitude") && !value.equals(""))
                        decimalLongitude = value;
                    if (fieldname.equalsIgnoreCase("materialSampleID"))
                        name.append("\t\t<name>" + value + "</name>\n");

                    fields++;
                }
                description.append("\t\t]]>\n");
                description.append("\t\t</description>\n");

                if (decimalLatitude != null && decimalLongitude != null) {
                    sb.append(header);
                    sb.append(name);
                    sb.append(description);

                    sb.append("\t\t<Point>\n");
                    sb.append("\t\t\t<coordinates>" + decimalLongitude + "," + decimalLatitude + "</coordinates>\n");
                    sb.append("\t\t</Point>\n");

                    sb.append("\t</Placemark>\n");
                }

            }
            count++;
        }

        sb.append("</Document>\n" +
                "</kml>");
        return writeFile(sb.toString(), file);
    }

    public String writeCSV(File file, Boolean writeHeader) {
        return writeTabularDataFile(file, writeHeader, ",");
    }


    /**
     * Write a tab delimited output
     *
     * @param file
     * @return
     */
    public String writeTAB(File file, Boolean writeHeader) {
        return writeTabularDataFile(file, writeHeader, "\t");
    }

    private String writeTabularDataFile(File file, boolean writeHeader, String delimeter) {
        // Header Row
        if (writeHeader) {
            createHeaderRow(sheet);
        }
        // Write the output to a file
        FileOutputStream fileOut = null;
        try {
            //File file = new File(fileLocation);
            fileOut = new FileOutputStream(file);

            Iterator rowIt = sheet.rowIterator();
            while (rowIt.hasNext()) {
                Row row = (Row) rowIt.next();

                for (int cn = 0; cn < row.getLastCellNum(); cn++) {
                    Cell cell = row.getCell(cn, Row.CREATE_NULL_AS_BLANK);
                    if (cell == null) {
                        fileOut.write((delimeter).getBytes());
                    } else {
                        byte[] contentInBytes;
                        switch (cell.getCellType()) {
                            case Cell.CELL_TYPE_STRING:
                                contentInBytes = cell.getRichStringCellValue().getString().getBytes();
                                break;

                            case Cell.CELL_TYPE_NUMERIC:
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    contentInBytes = String.valueOf(cell.getDateCellValue()).getBytes();
                                } else {
                                    Double value = cell.getNumericCellValue();
                                    // excel will return numbers as double. If it is an integer, get the int contentInBytes
                                    if (attributes.get(cn).getDatatype() == DataType.INTEGER) {
                                        contentInBytes = String.valueOf(value.intValue()).getBytes();
                                    } else {
                                        contentInBytes = String.valueOf(value).getBytes();
                                    }
                                }
                                break;

                            case Cell.CELL_TYPE_BOOLEAN:
                                contentInBytes = String.valueOf(cell.getBooleanCellValue()).getBytes();
                                break;

                            case Cell.CELL_TYPE_FORMULA:
                                contentInBytes = String.valueOf(cell.getCellFormula()).getBytes();
                                break;

                            case Cell.CELL_TYPE_BLANK:
                                contentInBytes = "".getBytes();
                                break;

                            default:
                                contentInBytes = cell.toString().getBytes();

                        }
                        fileOut.write(contentInBytes);
                        fileOut.write((delimeter).getBytes());
                    }
                }
                fileOut.write(("\n").getBytes());
            }
            fileOut.close();
            return file.getAbsolutePath();
        } catch (FileNotFoundException e) {
            throw new FimsRuntimeException(500, e);
        } catch (IOException e) {
            throw new FimsRuntimeException(500, e);
        }

    }
}
