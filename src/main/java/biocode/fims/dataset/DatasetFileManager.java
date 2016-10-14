package biocode.fims.dataset;

import biocode.fims.digester.Mapping;
import biocode.fims.digester.Validation;
import biocode.fims.reader.ReaderManager;
import biocode.fims.reader.plugins.TabularDataReader;
import biocode.fims.run.ProcessController;

/**
 * Created by rjewing on 6/10/16.
 */
public class DatasetFileManager implements FileManager {
    private String filename;
    private ProcessController processController;
    private final String[] extensions = new String[] {"xls", "xlsx", "csv", "txt"};

    /**
     * this constructor is only ment to be used in {@link FileManagerFactory}
     */
    DatasetFileManager() {}

    public DatasetFileManager(String filename, ProcessController processController) {
        this.filename = filename;
        this.processController = processController;
    }

    @Override
    public void validate(String outputFolder) {
        Mapping mapping = processController.getMapping();
        Validation validation = processController.getValidation();
        String outputPrefix = processController.getExpeditionCode() + "_output";

        // Create the tabularDataReader for reading the input file
        ReaderManager rm = new ReaderManager();
        rm.loadReaders();
        TabularDataReader tdr = rm.openFile(filename, mapping.getDefaultSheetName(), outputFolder);

        if (tdr == null) {
            processController.setHasErrors(true);
            processController.appendStatus("<br>Unable to open the file you attempted to upload.<br>");
            return;
        }

        // Run the validation
        validation.run(tdr, outputPrefix, outputFolder, mapping);

        tdr.closeFile();

        // get the Messages from each worksheet and add them to the processController
        validation.getMessages(processController);
    }

    @Override
    public void upload() {

    }

    @Override
    public String[] getExtensions() {
        return extensions;
    }
}
