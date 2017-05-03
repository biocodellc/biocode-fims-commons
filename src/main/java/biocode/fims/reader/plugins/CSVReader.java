package biocode.fims.reader.plugins;


import biocode.fims.digester.Attribute;
import biocode.fims.digester.Entity;
import biocode.fims.digester.Mapping;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.fimsExceptions.errorCodes.ValidationCode;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordMetadata;
import biocode.fims.models.records.RecordSet;
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
public class CSVReader implements DataReader {
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
    private List<String> colNames;

    /**
     * This is only to be used for passing the class into the DataReaderFactory
     */
    public CSVReader() {
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

        if (!recordMetadata.has(SHEET_NAME_KEY)) {
            throw new FimsRuntimeException(DataReaderCode.MISSING_METADATA, 500);
        }

        this.sheetName = String.valueOf(recordMetadata.remove(SHEET_NAME_KEY));
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
    public List<RecordSet> getRecordSets() {

        init();
        instantiateRecords();

        return generateRecordSets();
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
        setColumnNames();

        recordEntites = mapping.getEntitiesForSheet(sheetName);
    }

    private void setColumnNames() {
        // Get the first row to populate Column Names
        colNames = nextRow();

        Set<String> colSet = new HashSet<>();

        for (String col : colNames) {
            if (!colSet.add(col)) {
                throw new FimsRuntimeException(DataReaderCode.DUPLICATE_COLUMNS, 400, "csv file", col);
            }
        }

    }

    private void instantiateRecords() {
        while (hasNext) {
            LinkedList<String> row = nextRow();

            for (Entity e : recordEntites) {
                try {
                    Record r = recordMetadata.type().newInstance();

                    for (Attribute a : e.getAttributes()) {
                        if (colNames.contains(a.getColumn())) {
                            String val = row.get(
                                    colNames.indexOf(a.getColumn())
                            );

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

    private LinkedList<String> nextRow() {
        if (!hasNext)
            throw new NoSuchElementException();

        int prevToken = ',';
        LinkedList<String> row = new LinkedList<>();

        try {
            while (st.ttype != StreamTokenizer.TT_EOL &&
                    st.ttype != StreamTokenizer.TT_EOF) {
                if (st.ttype == ',') {
                    // See if we just passed an empty field.
                    if (prevToken == ',') {
                        row.add("");
                    }
                } else {
                    // See if we just passed an escaped double quote inside
                    // of a quoted string.
                    if (prevToken != ',')
                        row.add(row.removeLast() + "\"" + st.sval);
                    else {
                        row.add(st.sval);
                    }
                }

                prevToken = st.ttype;
                st.nextToken();
            }
        } catch (IOException e) {
        }

        // Test for the special case of a blank last field.
        if (prevToken == ',') {
            row.add("");
        }

        setHasNext();

        return row;
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

}
