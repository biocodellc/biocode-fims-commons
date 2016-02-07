package biocode.fims.run;

import biocode.fims.bcid.Bcid;
import biocode.fims.bcid.BcidMinter;
import biocode.fims.bcid.Database;
import biocode.fims.bcid.ExpeditionMinter;
import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.Entity;
import biocode.fims.digester.Mapping;
import biocode.fims.digester.Validation;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.FimsException;
import biocode.fims.reader.ReaderManager;
import biocode.fims.reader.plugins.TabularDataReader;
import biocode.fims.settings.FimsPrinter;
import biocode.fims.settings.SettingsManager;
import biocode.fims.settings.StandardPrinter;
import org.apache.commons.cli.*;
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

    Mapping mapping;
    Validation validation;

    String outputFolder;
    String outputPrefix;
    private ProcessController processController;
    private static Logger logger = LoggerFactory.getLogger(Process.class);
    protected int projectId;

    private static SettingsManager sm;
    static {
        sm = SettingsManager.getInstance();
    }

    /**
     * Setup class variables for processing FIMS data.
     *
     * @param outputFolder  Where to store output files
     */
    public Process(
            String inputFilename,
            String outputFolder,
            ProcessController processController) {

        // Read the Configuration File
        configFile = new ConfigurationFileFetcher(processController.getProjectId(), outputFolder, false).getOutputFile();

        init(inputFilename, outputFolder, processController);
    }

    /**
     * Setup class variables for processing FIMS data.
     *
     * @param inputFilename The data to run.process, usually an Excel spreadsheet
     * @param outputFolder  Where to store output files
     */
    public Process(
            String inputFilename,
            String outputFolder,
            ProcessController processController,
            File configFile) {

        // Read the Configuration File
        this.configFile = configFile;

        init(inputFilename, outputFolder, processController);
    }

    private void init(String inputFilename, String outputFolder, ProcessController processController) {
        // Update the processController Settings
        this.processController = processController;

        projectId = processController.getProjectId();

        processController.setInputFilename(inputFilename);
        this.outputFolder = outputFolder;

        // Control the file outputPrefix... set them here to expedition codes.
        this.outputPrefix = processController.getExpeditionCode() + "_output";

        // Parse the Mapping object (this object is used extensively in downstream functions!)
        mapping = new Mapping();
        mapping.addMappingRules(new Digester(), configFile);
        processController.setDefaultSheetUniqueKey(mapping.getDefaultSheetUniqueKey());

        if (processController.getValidation() == null) {
            // Load validation object as this is used in downstream functions
            validation = new Validation();
            validation.addValidationRules(new Digester(), configFile);

            processController.setValidation(validation);
        } else {
            validation = processController.getValidation();
        }
    }

    /**
     * A constructor for when we're running queries or reading template files
     *
     * @param configFile
     */
    public Process(
            int projectId,
            File configFile) {
        this.projectId = projectId;
        this.configFile = configFile;
        this.outputPrefix = "output";

        // Parse the Mapping object (this object is used extensively in downstream functions!)
        mapping = new Mapping();
        mapping.addMappingRules(new Digester(), configFile);
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

    public int getProjectId() {
        return projectId;
    }

    /**
     * Check the status of this expedition
     */
    public void runExpeditionCheck() {
        ExpeditionMinter expeditionMinter = new ExpeditionMinter();
        Boolean checkExpedition = expeditionMinter.expeditionExistsInProject(processController.getExpeditionCode(), projectId);
        processController.setExpeditionCreateRequired(!checkExpedition);
        if (!checkExpedition) {
            processController.setExpeditionAssignedToUserAndExists(true);
        }
        expeditionMinter.close();
    }

    /**
     * Create an expedition
     */
    public void runExpeditionCreate() {
        runExpeditionCheck();
        if (processController.isExpeditionCreateRequired()) {
            System.out.println("Creating expedition " + processController.getExpeditionCode() + "...");
            createExpedition(processController, mapping);
        }
        processController.setExpeditionCreateRequired(false);
        processController.setExpeditionAssignedToUserAndExists(true);
    }

    private boolean createExpedition(ProcessController processController, Mapping mapping) {
        String status = "\tCreating expedition " + processController.getExpeditionCode() + " ... this is a one time process " +
                "before loading each spreadsheet and may take a minute...\n";
        processController.appendStatus(status);
        FimsPrinter.out.println(status);

        ExpeditionMinter expedition = new ExpeditionMinter();
        try {
            // Mint a expedition
            expedition.mint(
                    processController.getExpeditionCode(),
                    processController.getExpeditionTitle(),
                    processController.getUserId(),
                    projectId,
                    processController.getPublicStatus()
            );
        } catch (FimsException e) {
            expedition.close();
            throw new BadRequestException(e.getMessage());
        }

        // Loop the mapping file and create a BCID for every entity that we specified there!
        if (mapping != null) {
            LinkedList<Entity> entities = mapping.getEntities();
            Iterator it = entities.iterator();
            while (it.hasNext()) {
                Entity entity = (Entity) it.next();

                String s = "\t\tCreating bcid root for " + entity.getConceptAlias() + " and resource type = " + entity.getConceptURI() + "\n";
                processController.appendStatus(s);

                // Detect if this is user=demo or not.  If this is "demo" then do not request EZIDs.
                // User account Demo can still create Data Groups, but they just don't get registered and will be purged periodically
                boolean ezidRequest = true;
                Database db = new Database();
                String username = db.getUserName(processController.getUserId());
                if (username.equals("demo") || sm.retrieveValue("ezidRequests").equalsIgnoreCase("false")) {
                    ezidRequest = false;
                }

                // Create the entity BCID
                BcidMinter bcidMinter = new BcidMinter(ezidRequest);

                String identifier = bcidMinter.createEntityBcid(new Bcid(processController.getUserId(), entity.getConceptAlias(),
                        entity.getConceptAlias(), "", null, null, false, false));
                bcidMinter.close();
                // Associate this Bcid with this expedition
                expedition.attachReferenceToExpedition(processController.getExpeditionCode(), identifier, processController.getProjectId());
            }
        }
        expedition.close();

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
            FimsPrinter.out.println(processController.getCommandLineSB().toString());
            return;
        }
        // Run the validation step
        if (!processController.isValidated() && processController.getHasWarnings()) {
            String message = "\tWarnings found on " + mapping.getDefaultSheetName() + " worksheet.\n" + processController.getCommandLineSB().toString();
            FimsPrinter.out.println(message);
            processController.setClearedOfWarnings(true);
            processController.setValidated(true);
        }
    }

    public void runValidation() {
        // Create the tabulardataReader for reading the input file
        ReaderManager rm = new ReaderManager();
        TabularDataReader tdr = null;
        rm.loadReaders();

        tdr = rm.openFile(processController.getInputFilename(), mapping.getDefaultSheetName(), outputFolder);

        if (tdr == null) {
            processController.setHasErrors(true);
            processController.appendStatus("<br>Unable to open the file you attempted to upload.<br>");
            processController.setCommandLineSB(new StringBuilder("Unable to open the file you attempted to upload."));
            return;
        }

        // Run the validation
        validation.run(tdr, outputPrefix, outputFolder, mapping);

        //
        processController = validation.printMessages(processController);
    }

    /**
     * Run the program from the command-line
     *
     * @param args
     */
    public static void main(String args[]) {
        //processController processController = new processController();
        String defaultOutputDirectory = System.getProperty("user.dir") + File.separator + "tripleOutput";
        SettingsManager.getInstance("biocode-fims.props");
        Integer projectId = 0;
        //System.out.print(defaultOutputDirectory);

        // Test configuration :
        // -d -t -u -i sampledata/Apogon***.xls

        // Direct output using the StandardPrinter subClass of fimsPrinter which send to fimsPrinter.out (for command-line usage)
        FimsPrinter.out = new StandardPrinter();


        // Some classes to help us
        CommandLineParser clp = new GnuParser();
        HelpFormatter helpf = new HelpFormatter();
        CommandLine cl;

        // The expedition code corresponds to a expedition recognized by BCID
        String expeditionCode = "";
        // The configuration template
        //String configuration = "";
        // The input file
        String inputFile = "";
        // The directory that we write all our files to
        String outputDirectory = "tripleOutput";

        // Define our commandline options
        Options options = new Options();
        options.addOption("h", "help", false, "print this help message and exit");
        options.addOption("f", "format", true, "excel|html|json|cspace  specifying the return format for the query");

        options.addOption("e", "expeditionCode", true, "Expedition code.  You will need to obtain a data code before " +
                "loading data");
        options.addOption("o", "outputDirectory", true, "Output Directory");
        options.addOption("i", "input_file", true, "Input Spreadsheet");
        options.addOption("p", "projectId", true, "Project Identifier.  A numeric integer corresponding to your project");
        options.addOption("configFile", true, "Use a local config file instead of getting from server");

        // Create the commands parser and parse the command line arguments.
        try {
            cl = clp.parse(options, args);
        } catch (UnrecognizedOptionException e) {
            FimsPrinter.out.println("Error: " + e.getMessage());
            return;
        } catch (ParseException e) {
            FimsPrinter.out.println("Error: " + e.getMessage());
            return;
        }

        // Help
        if (cl.hasOption("h")) {
            helpf.printHelp("fims ", options, true);
            return;
        }

        // No options returns help message
        if (cl.getOptions().length < 1) {
            helpf.printHelp("fims ", options, true);
            return;
        }

        // Sanitize project specification
        if (cl.hasOption("p")) {
            try {
                projectId = new Integer(cl.getOptionValue("p"));
            } catch (Exception e) {
                FimsPrinter.out.println("Bad option for projectId");
                helpf.printHelp("fims ", options, true);
                return;
            }
        }

        // Set input file
        if (cl.hasOption("i"))
            inputFile = cl.getOptionValue("i");

        // Set output directory
        if (cl.hasOption("o"))
            outputDirectory = cl.getOptionValue("o");

        // Set expeditionCode
        if (cl.hasOption("e"))
            expeditionCode = cl.getOptionValue("e");

        // Set default output directory if one is not specified
        if (!cl.hasOption("o")) {
            FimsPrinter.out.println("Using default output directory " + defaultOutputDirectory);
            outputDirectory = defaultOutputDirectory;
        }

        // Check that output directory is writable
        try {
            if (!new File(outputDirectory).canWrite()) {
                FimsPrinter.out.println("Unable to write to output directory " + outputDirectory);
                return;
            }
        } catch (Exception e) {
            FimsPrinter.out.println("Unable to write to output directory " + outputDirectory);
            return;
        }

        // Run the command
        ProcessController processController = new ProcessController(projectId, expeditionCode);
        Process p;

        // use local configFile if specified
        if (cl.hasOption("configFile")) {
            System.out.println("using local config file = " + cl.getOptionValue("configFile").toString());
            p = new Process(
                    inputFile,
                    outputDirectory,
                    processController,
                    new File(cl.getOptionValue("configFile")));
        } else {
            p = new Process(
                    inputFile,
                    outputDirectory,
                    processController
            );
        }

        FimsPrinter.out.println("Initializing ...");
        FimsPrinter.out.println("\tinputFilename = " + inputFile);

        // Run the processor
        p.runAllLocally();
    }

}
