package biocode.fims.reader.plugins;


import biocode.fims.digester.Attribute;
import biocode.fims.digester.Entity;
import biocode.fims.digester.Mapping;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordMetadata;
import biocode.fims.models.records.RecordSet;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.util.Assert;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Provides the ability to parse CSV formatted files.  This implementation is
 * mostly compliant with the RFC 4180 CSV specification.  In particular, 1) each
 * record is expected to be on a single line; 2) fields may be enclosed in
 * double quotes; 3) double quotes inside of a double-quoted string should be
 * escaped with a double quote; 4) space and tab characters are not trimmed from
 * the beginning or end of fields.
 * <p>
 * This implementation has some additional features and/or limitations that
 * might not be supported by other CSV libraries.  First, if a field is
 * double-quoted, there should be no additional characters outside of the double
 * quotes.  Second, records must not span multiple lines.  Third, standard
 * escape sequences (e.g., "\"\t") are also supported inside of quoted fields.
 * Fourth, blank lines are ignored.
 * <p>
 * Finally, note that this class expects text to consist of simple ASCII
 * (1 byte) characters, and that characters 0-8 and 10-31 are all treated as
 * empty whitespace and ignored (unless they occur within a quoted string).
 */
public class CSVReader implements TabularDataReader {
    public static final String SHEET_NAME_KEY = "sheetName";

    private static final List<String> EXTS = Collections.singletonList("csv");
    private String sheetName;

    private File file;
    private Mapping mapping;
    private RecordMetadata recordMetadata;

    private StreamTokenizer st;
    private boolean hasNext = false;

    private Map<String, List<Record>> entityRecords;
    private List<Entity> recordEntites;

    private LinkedList<String> reclist;
    //private int currtable = 1;

    // A reference to the file that opened this reader
    protected String fileName;

    // The number of rows in the active worksheet
    private int numRows;

    List<String> colNames;

    /**
     * This is only to be used for passing the class into the DataReaderFactory
     */
    public CSVReader() {
        reclist = new LinkedList<String>();
    }

    /**
     * @param file
     * @param mapping
     * @param recordMetadata must contain the key SHEET_NAME_KEY declaring the {@link Entity#getWorksheet()} of the csv file
     */
    public CSVReader(File file, Mapping mapping, RecordMetadata recordMetadata) {
        Assert.notNull(file);
        Assert.notNull(mapping);
        Assert.notNull(recordMetadata);
        this.file = file;
        this.mapping = mapping;
        this.recordMetadata = recordMetadata;
        this.entityRecords = new HashMap<>();
        this.reclist = new LinkedList<>();

        if (!recordMetadata.has(SHEET_NAME_KEY)) {
            throw new FimsRuntimeException(DataReaderCode.MISSING_METADATA, 500);
        }

        this.sheetName = String.valueOf(recordMetadata.remove(SHEET_NAME_KEY));
    }


    @Override
    public List<RecordSet> getRecordSets() {

        init();
        instantiateRecords();

        return generateRecordSets();
    }

    private void instantiateRecords() {
        while (tableHasNextRow()) {
            String[] row = tableGetNextRow();

            for (Entity e : recordEntites) {
                try {
                    Record r = recordMetadata.type().newInstance();

                    for (Attribute a : e.getAttributes()) {
                        if (colNames.contains(a.getColumn())) {
                            String val = row[colNames.indexOf(a.getColumn())];

                            r.set(a.getUri(), val);
                        }
                    }

                    entityRecords.computeIfAbsent(e.getConceptAlias(), k -> new ArrayList<>()).add(r);

                } catch (InstantiationException | IllegalAccessException e1) {
                    throw new FimsRuntimeException("", 500);
                }
            }

        }
    }

    private List<RecordSet> generateRecordSets() {
        List<RecordSet> recordSets = new ArrayList<>();

        for (Map.Entry<String, List<Record>> e : entityRecords.entrySet()) {
            recordSets.add(
                    new RecordSet(e.getKey(), e.getValue())
            );
        }

        return recordSets;
    }

    private void init() {
        try {
            st = new StreamTokenizer(new FileReader(file));
        } catch (FileNotFoundException e) {
            throw new FimsRuntimeException(FileCode.READ_ERROR, 500);
        }

        st.resetSyntax();
        st.eolIsSignificant(true);
        st.whitespaceChars(0, 31);
        st.wordChars(' ', 255);
        st.wordChars('\t', '\t');
        st.quoteChar('"');
        st.ordinaryChar(',');

        setHasNext();

        // Get the first row to populate Column Names
        colNames = Arrays.asList(tableGetNextRow());

        recordEntites = mapping.getEntities().stream()
                .filter(e -> {
                    return sheetName.equals(e.getWorksheet());
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean handlesExtension(String ext) {
        return EXTS.contains(ext.toLowerCase());
    }

    @Override
    public DataReader newInstance(File file, Mapping mapping, RecordMetadata recordMetadata) {
        return new CSVReader(file, mapping, recordMetadata);
    }

    @Override
    public String[] tableGetNextRow() {
        if (!tableHasNextRow())
            throw new NoSuchElementException();

        int prevToken = ',';
        int fieldcnt = 0;
        reclist.clear();

        try {
            while (st.ttype != StreamTokenizer.TT_EOL &&
                    st.ttype != StreamTokenizer.TT_EOF) {
                if (st.ttype == ',') {
                    // See if we just passed an empty field.
                    if (prevToken == ',') {
                        reclist.add("");
                        fieldcnt++;
                    }
                } else {
                    // See if we just passed an escaped double quote inside
                    // of a quoted string.
                    if (prevToken != ',')
                        reclist.add(reclist.removeLast() + "\"" + st.sval);
                    else {
                        reclist.add(st.sval);
                        fieldcnt++;
                    }
                }

                prevToken = st.ttype;
                st.nextToken();
            }
        } catch (IOException e) {
        }

        // Test for the special case of a blank last field.
        if (prevToken == ',') {
            reclist.add("");
            fieldcnt++;
        }

        setHasNext();

        String[] ret = new String[fieldcnt];
        for (int cnt = 0; cnt < fieldcnt; cnt++)
            ret[cnt] = reclist.remove();

        return ret;
    }

    /**
     * See if the specified file is a CSV file.  Since no "magic number" can
     * be defined for CSV files, this test is limited to seeing if the file
     * extension is "csv".  This method also tests if the file actually exists.
     *
     * @param filepath The file to test.
     * @return True if the specified file exists and appears to be a CSV file,
     * false otherwise.
     */
    @Override
    public boolean testFile(String filepath) {
        // test if the file exists
        File file = new File(filepath);
        if (!file.exists())
            return false;

        int index = filepath.lastIndexOf('.');

        if (index != -1 && index != (filepath.length() - 1)) {
            // get the extension
            String ext = filepath.substring(index + 1);

            if (ext.equals("csv"))
                return true;
        }

        return false;
    }


    public boolean openFile(String filepath, String defaultSheetName, String outputFolder) {
        // Set the input file name
        fileName = filepath;

        try {
            st = new StreamTokenizer(new FileReader(filepath));
        } catch (FileNotFoundException e) {
            return false;
        }

        st.resetSyntax();
        st.eolIsSignificant(true);
        st.whitespaceChars(0, 31);
        st.wordChars(' ', 255);
        st.wordChars('\t', '\t');
        st.quoteChar('"');
        st.ordinaryChar(',');

        setHasNext();

        // Get the first row to populate Column Names
        colNames = Arrays.asList(tableGetNextRow());
        return true;
    }

    /**
     * Internal method to see if there is another line with data remaining in
     * the file.  Any completely blank lines will be skipped.  At exit, st.ttype
     * will be the first token of the next record in the file.
     */
    private void setHasNext() {
        int tokentype = StreamTokenizer.TT_EOF;

        do {
            try {
                tokentype = st.nextToken();
            } catch (IOException e) {
            }
        } while (tokentype == StreamTokenizer.TT_EOL);

        if (tokentype != StreamTokenizer.TT_EOF) {
            hasNext = true;
        } else {
            hasNext = false;
        }
    }

    @Override
    public boolean hasNextTable() {
        return false;
    }

    @Override
    public void moveToNextTable() {
        throw new NoSuchElementException();
    }

    @Override
    public String getCurrentTableName() {
        return "table1";
    }

    @Override
    public boolean tableHasNextRow() {
        return hasNext;
    }


    @Override
    public void closeFile() {
    }

    @Override
    public List<String> getColNames() {
        return colNames;
    }

    @Override
    public Sheet getSheet() {
        return null;
    }

    @Override
    public int getNumRows() {
        if (numRows > 0) {
            return numRows;
        }

        try {
            int lines = countLines(fileName);
            // decrement by one... assumption is that we MUST have a header for FIMS to work.
            lines--;
            numRows = lines;
            return numRows;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * An efficient method for counting the lines in a file
     *
     * @param filename
     * @return
     * @throws IOException
     */
    public static int countLines(String filename) throws IOException {
        int count = 0;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filename))) {
            while ((bufferedReader.readLine()) != null) {
                count++;
            }
        }
        return count;
    }

    @Override
    public File getInputFile() {
        return null;
    }

    @Override
    public String getStringValue(String column, int row) {
        return null;
    }

    @Override
    public String getStringValue(int col, int row) {
        return null;
    }

    @Override
    public Double getDoubleValue(String column, int row) {
        return null;
    }

    @Override
    public Integer getColumnPosition(String colName) {
        return null;
    }

    @Override
    public void setTable(String table) {
        table = "sheet1";

    }

    @Override
    public String getFormatString() {
        return "CSV";
    }

    @Override
    public String getShortFormatDesc() {
        return "CSV";
    }

    @Override
    public String getFormatDescription() {
        return "comma-separated values";
    }

    @Override
    public String[] getFileExtensions() {
        return new String[]{"csv"};
    }

}
