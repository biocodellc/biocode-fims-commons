package biocode.fims.fasta;

import biocode.fims.bcid.Database;
import biocode.fims.digester.Worksheet;
import biocode.fims.fimsExceptions.FimsException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.reader.ReaderManager;
import biocode.fims.reader.plugins.TabularDataReader;
import biocode.fims.renderers.RowMessage;
import biocode.fims.run.ProcessController;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

/**
 * FastaManager handles uploading of FASTA files and attaching the sequences to a dataset
 */
public abstract class FastaManager {
    // Store all messages related to this Worksheet
    private LinkedList<RowMessage> messages = new LinkedList<RowMessage>();
    protected ProcessController processController;
    protected HashMap<String, String> fastaData = null;

    private String fastaFilename;

    private Boolean hasErrors = false;
    private Boolean hasWarnings = false;
    public FastaManager(ProcessController processController, String fastaFilename) {
        this.processController = processController;
        this.fastaFilename = fastaFilename;
    }

    /**
     * verify that the identifiers in the fasta file are in a dataset. If only a FASTA file is being uploaded,
     * then we will retrieve the dataset from the fuseki triple store.
     *
     */
    public void validate(String outputFolder) {
        ArrayList<String> datasetIds;

        if (processController.getInputFilename() != null) {
            // Create the tabulardataReader for reading the input file
            ReaderManager rm = new ReaderManager();
            rm.loadReaders();

            TabularDataReader tdr = rm.openFile(processController.getInputFilename(),
                    processController.getDefaultSheetUniqueKey(), outputFolder);

            if (tdr == null) {
                processController.setHasErrors(true);
                processController.appendStatus("<br>Unable to open the file you attempted to upload.<br>");
                processController.setCommandLineSB(new StringBuilder("Unable to open the file you attempted to upload."));
                return;
            }

            datasetIds = fetchIds(tdr);
        } else {
            // fetch ids from fuseki
            datasetIds = fetchIds();
        }

        if (datasetIds.isEmpty()) {
            processController.appendStatus("<br>No data was found in the dataset.<br>");
            processController.setHasErrors(true);
            return;
        }

        // parse the FASTA file to get an array of sequence identifiers
        parseFasta();
        Set<String> fastaIds = fastaData.keySet();

        if (fastaData.isEmpty()) {
            processController.appendStatus("<br>No data was found in the fasta file.<br>");
            processController.setHasErrors(true);
            return;
        }

        // verify that all fastaIds exist in the dataset
        ArrayList<String> invalidIds = new ArrayList<>();
        for (String fastaId: fastaIds) {
            if (!datasetIds.contains(fastaId)) {
                invalidIds.add(fastaId);
            }
        }
        if (!invalidIds.isEmpty()) {
            processController.appendStatus("<br>The following sequences exist in the FASTA file that do not exist in the dataset.<br>");
            processController.appendStatus("<ul>");
            for (String id: invalidIds) {
                processController.appendStatus("<li>" + id + "</li>");
            }
            processController.appendStatus("</ul>");
            processController.setHasWarnings(true);
        }
    }

    /**
     * fetches the graph from the most recent dataset loaded for the expeditionCode and projectId set in the
     * processController. This should be called before a new dataset has been uploaded. This is useful for fetching the
     * collection ids from an existing dataset or copying over the sequences from an old datset to a new dataset.
     *
     * @return graph or null
     */
    // TODO should this be abstract? Decide after MySql inplementation
    public String fetchGraph() {
        ResultSet rs = null;
        PreparedStatement stmt = null;
        Database db = new Database();

        try {
            String query = "SELECT b.graph FROM bcids b, expeditionBcids eb, expeditions e, " +
                    "(SELECT b1.graph, e.expeditionCode, max(b1.ts) as maxts FROM bcids b1, expeditionBcids eb, expeditions e " +
                    "   WHERE eb.bcidId=b1.bcidId AND eb.expeditionId=e.expeditionId AND b1.resourceType = \"http://purl.org/dc/dcmitype/Dataset\" " +
                    "   AND e.expeditionCode = ? AND e.projectId = ? GROUP BY e.expeditionCode) as b2 " +
                    "WHERE b.bcidId = eb.bcidId AND eb.expeditionId = e.expeditionId AND e.expeditionCode = ? AND " +
                    "e.projectId = ? AND b.resourceType = \"http://purl.org/dc/dcmitype/Dataset\" AND b.ts = b2.maxts";

            stmt = db.getConn().prepareStatement(query);
            stmt.setString(1, processController.getExpeditionCode());
            stmt.setInt(2, processController.getProjectId());
            stmt.setString(3, processController.getExpeditionCode());
            stmt.setInt(4, processController.getProjectId());

            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("b.graph");
            }

        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
            db.close();
        }

        return null;
    }

    public String getFastaFilename() {
        return fastaFilename;
    }

    /**
     * triplify and upload a fasta file to the specified graph
     * @param graph
     */
    public abstract void upload(String graph);

    /**
     * copy over the fasta sequences <urn:sequence> from the previousGraph to the newGraph. Only copy the sequences where
     * the ark: exists in the new graph
     *
     * @param previousGraph
     * @param newGraph
     */
    public abstract void copySequences(String previousGraph, String newGraph);

    /**
     * fetch the latest uploaded dataset and then parse the returned Resources.
     *
     * @return An array of identifiers that exist in the dataset
     */
    protected abstract ArrayList<String> fetchIds();

    /**
     * get the values for each row in the
     * @param tdr
     * @return
     */
    private ArrayList<String> fetchIds(TabularDataReader tdr) {
        ArrayList<String> datasetIds = new ArrayList<>();

        Worksheet sheet = null;
        String sheetName = "";
        try {
            sheet = processController.getValidation().getWorksheets().get(0);
            sheetName = sheet.getSheetname();
            tdr.setTable(sheetName);
        } catch (FimsException e) {
            // An error here means the sheetname was not found, throw an application message
            sheet.getMessages().addLast(new RowMessage("Unable to find a required worksheet named '" + sheetName + "' (no quotes)", "Spreadsheet check", RowMessage.ERROR));
            hasErrors = true;
            return datasetIds;
        }

        // Default rule... always check that there is some data
        if (tdr.getNumRows() < 1) {
            messages.addLast(new RowMessage("No data found", "Spreadsheet check", RowMessage.ERROR));
            hasErrors = true;
        } else {
            for (int row = 1; row <= tdr.getNumRows(); row++) {
                datasetIds.add(tdr.getStringValue(processController.getDefaultSheetUniqueKey(), row));
            }
        }

        return datasetIds;
    }

    /**
     * parse the fasta file identifier-sequence pairs
     * @return HashMap<String, String> with (identifier, sequence) pairs
     */
    private HashMap<String, String> parseFasta() {
        fastaData = new HashMap<>();
        try {
            FileReader input = new FileReader(fastaFilename);
            BufferedReader bufRead = new BufferedReader(input);
            String line;
            String identifier = null;
            String sequence = "";

            while ((line = bufRead.readLine()) != null) {
                // > deliminates the next identifier, sequence block in the fasta file
                if (line.startsWith(">")) {
                    if (!sequence.isEmpty()|| identifier != null) {
                        fastaData.put(identifier, sequence);
                        // after putting the sequence into the hashmap, reset the sequence
                        sequence = "";
                    }
                    // parse the identifier - minus the deliminator
                    identifier = line.substring(1);
                } else {
                    // if we are here, we are inbetween 2 identifiers. This means this is all sequence data
                    sequence += line;
                }
            }

            // need to put the last sequence data into the hashmap
            fastaData.put(identifier, sequence);
        } catch (IOException e) {
            throw new ServerErrorException(e);
        }
        return fastaData;
    }

}
