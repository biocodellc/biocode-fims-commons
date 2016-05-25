package biocode.fims.run;

import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.Entity;
import biocode.fims.digester.Mapping;
import biocode.fims.digester.Validation;
import biocode.fims.entities.*;
import biocode.fims.reader.ReaderManager;
import biocode.fims.reader.plugins.TabularDataReader;
import biocode.fims.service.BcidService;
import biocode.fims.service.ExpeditionService;
import biocode.fims.settings.FimsPrinter;
import biocode.fims.settings.SettingsManager;
import org.apache.commons.digester3.Digester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Core class for running fims processes.  Here you specify the input file, configuration file, output folder, and
 * a expedition code, which is used to specify Bcid roots in the BCID (http://code.google.com/p/bcid/) system.
 * The main class is configured to run this from the command-line while the class itself can be extended to run
 * in different situations, while specifying  fimsPrinter and FimsInputter classes for a variety of styles of output
 * and
 * input
 */
public class Process {

    public File configFile;
    private ExpeditionService expeditionService;

    private Mapping mapping;
    private Validation validation;

    String outputFolder;
    String outputPrefix;
    private ProcessController processController;
    private static Logger logger = LoggerFactory.getLogger(Process.class);

    private static SettingsManager sm = SettingsManager.getInstance();

    /**
     * Setup class variables for processing FIMS data.
     *
     * @param outputFolder  Where to store output files
     */
    public Process(
            String outputFolder,
            ProcessController processController,
            ExpeditionService expeditionService) {
        this.expeditionService = expeditionService;

        // Read the Configuration File
        configFile = new ConfigurationFileFetcher(processController.getProjectId(), outputFolder, false).getOutputFile();

        init(outputFolder, processController);
    }

    /**
     * Setup class variables for processing FIMS data.
     *
     * @param outputFolder  Where to store output files
     */
    public Process(
            String outputFolder,
            ProcessController processController,
            File configFile,
            ExpeditionService expeditionService) {

        // Read the Configuration File
        this.configFile = configFile;
        this.expeditionService = expeditionService;

        init(outputFolder, processController);
    }

    private void init(String outputFolder, ProcessController processController) {
        // Update the processController Settings
        this.processController = processController;

        this.outputFolder = outputFolder;

        // Control the file outputPrefix... set them here to expedition codes.
        this.outputPrefix = processController.getExpeditionCode() + "_output";

        if (processController.getMapping() == null) {
            // Parse the Mapping object (this object is used extensively in downstream functions!)
            mapping = new Mapping();
            mapping.addMappingRules(new Digester(), configFile);
            processController.setMapping(mapping);
        } else {
            mapping = processController.getMapping();
        }
        processController.setDefaultSheetUniqueKey(mapping.getDefaultSheetUniqueKey());

        if (processController.getValidation() == null) {
            // Load validation object as this is used in downstream functions
            validation = new Validation();
            validation.addValidationRules(new Digester(), configFile, mapping);

            processController.setValidation(validation);
        } else {
            validation = processController.getValidation();
        }
    }

    /**
     * a constructor for DeepRoots lookupPrefix method
     */
    public Process() {}

    /**
     * Always use this method to fetch the process Controller from the process class as it has the current status
     *
     * @return
     */
    public ProcessController getProcessController() {
        return processController;
    }

    public Mapping getMapping() {
        return mapping;
    }

    /**
     * Check the status of this expedition
     */
    public Expedition runExpeditionCheck() {
        Expedition expedition = expeditionService.getExpedition(
                processController.getExpeditionCode(),
                processController.getProjectId()
        );

        if (expedition == null) {
            processController.setExpeditionCreateRequired(true);
        } else if (Boolean.valueOf(sm.retrieveValue("ignoreUser")) ||
                expedition.getUser().getUserId() == processController.getUserId() )
            processController.setExpeditionAssignedToUserAndExists(true);
        return expedition;
    }

    /**
     * Create an expedition
     */
    public void runExpeditionCreate(BcidService bcidService) {
        runExpeditionCheck();
        if (processController.isExpeditionCreateRequired()) {
            System.out.println("Creating expedition " + processController.getExpeditionCode() + "...");
            createExpedition(processController, bcidService);
        }
        processController.setExpeditionCreateRequired(false);
        processController.setExpeditionAssignedToUserAndExists(true);
    }

    private boolean createExpedition(ProcessController processController, BcidService bcidService) {
        String status = "\tCreating expedition " + processController.getExpeditionCode() + " ... this is a one time process " +
                "before loading each spreadsheet and may take a minute...\n";
        processController.appendStatus(status);
        FimsPrinter.out.println(status);

        Expedition expedition = new Expedition.ExpeditionBuilder(
                processController.getExpeditionCode())
                .expeditionTitle(processController.getExpeditionTitle())
                .isPublic(processController.getPublicStatus())
                .build();

        expeditionService.create(expedition, processController.getUserId(), processController.getProjectId(), null, mapping);

        return true;

    }

    /**
     * runAll method is designed to go through the FIMS process for a local application.  The REST services
     * would handle user input/output differently
     *
     */
    public void runAllLocally() {

        // Validation Step
        runValidation();

        // If there is errors, tell the user and stop the operation
        if (processController.getHasErrors()) {
            FimsPrinter.out.println(processController.printMessages());
            return;
        }
        // Run the validation step
        if (!processController.isValidated() && processController.getHasWarnings()) {
            String message = "\tWarnings found on " + mapping.getDefaultSheetName() + " worksheet.\n" + processController.printMessages();
            FimsPrinter.out.println(message);
            processController.setClearedOfWarnings(true);
            processController.setValidated(true);
        }
    }

    public void runValidation() {
        TabularDataReader tdr = getTabularDataReader();
        if (tdr == null) {
            processController.setHasErrors(true);
            processController.appendStatus("<br>Unable to open the file you attempted to upload.<br>");
            processController.setCommandLineSB(new StringBuilder("Unable to open the file you attempted to upload."));
            return;
        }

        // Run the validation
        validation.run(tdr, outputPrefix, outputFolder, mapping);

        tdr.closeFile();

        // get the Messages from each worksheet and add them to the processController
        validation.getMessages(processController);
    }

    /**
     * get a TabularDataReader object. the inputFilename must be set on processController. You also need
     * to call TabularDataReader.closeFile() when you are done with the TabularDataReader
     *
     * @return TabulardataReader
     */
    public TabularDataReader getTabularDataReader() {
        // Create the tabulardataReader for reading the input file
        ReaderManager rm = new ReaderManager();
        TabularDataReader tdr = null;
        rm.loadReaders();

        tdr = rm.openFile(processController.getInputFilename(), mapping.getDefaultSheetName(), outputFolder);

        return tdr;
    }
}
