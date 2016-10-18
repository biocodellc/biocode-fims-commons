package biocode.fims.run;

import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.entities.User;
import biocode.fims.fileManagers.dataset.DatasetFileManager;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.UserService;
import biocode.fims.settings.FimsPrinter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;

/**
 * Special purpose class for Bulk Loading Data
 * This is coded just for DIPNET data for now, but meant to server as a template/inspiration for a more generic bulk-loader
 * Created by jdeck on 7/10/15.
 */
public class BulkLoader {
    private static Integer PROJECT_ID = 25;
    private static String USERNAME = "dipnetCurator";

    // output directory for processing and temp files
    static String outputDirectory = "/Users/jdeck/IdeaProjects/biscicol-fims/tripleOutput";
    // Input directory storing all the loaded files
    static String inputDirectory = "/Users/jdeck/Google Drive/!DIPnet_DB/Repository/1-cleaned_QC2_mdfasta_files";
    static String inputFastaDirectory = "/Users/jdeck/Google Drive/!DIPnet_DB/Repository/1_cleaned_nonaligned_fasta_files";

    private static ExpeditionService expeditionService;
    private static DatasetFileManager datasetFileManager;

    static boolean triplify = false;
    static boolean upload = true;
    static boolean expeditionCheck = true;
    static boolean forceAll = true;  // force loading, no questions

    private static User user;

    public static void main(String[] args) throws FileNotFoundException {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("/applicationContext.xml");
        UserService userService = applicationContext.getBean(UserService.class);
        expeditionService = applicationContext.getBean(ExpeditionService.class);
        datasetFileManager = applicationContext.getBean(DatasetFileManager.class);

        // Redirect output to file
        PrintStream out = new PrintStream(new FileOutputStream(outputDirectory + File.separator + "dipnetloading_output.txt"));
        System.setOut(out);

        // only argument is password
        String password = args[0];

        // Call the connection with password as single argument
        FimsPrinter.out.println("Authenticating ...");
        user = userService.getUser(USERNAME, password);

        if (user == null) {
            FimsPrinter.out.println("Unable to authenticate " + USERNAME +
                    " using the supplied credentials!");

            return;
        }

        // ONE-OFF Run the dataset Loader
        loadDataset("C2_acapla_CO1_all", "/Users/jdeck/Google Drive/!DIPnet_DB/Repository/1-cleaned_QC2_mdfasta_files/mdfastaQC2_acapla_CO1_all.txt");
        loadDataset("QC2_Eucmet_C01_HL_all","/Users/jdeck/Google Drive/!DIPnet_DB/Repository/1-cleaned_QC2_mdfasta_files/mdfastaQC2_Eucmet_CO1_HL.txt");

         /*
        // Loop all the files in the input directory
        Iterator it = FileUtils.iterateFiles(new File(inputDirectory), null, false);
        while (it.hasNext()) {
            File file = (File) it.next();
            String fileAbsolutePath = file.getAbsolutePath();
            String fileName = file.getName();
            String datasetCode;
            //Don't attempt to load conflicted files
            if (!fileName.contains("Conflict")) {
                if (fileName.contains(".txt")) {
                    datasetCode = fileName.replaceAll(".txt", "").replaceAll("mdfastaQC2_", "");

                    // Run the dataset Loader
                    loadDataset(datasetCode, fileAbsolutePath);
                }
            }
        }  */

    }

    /**
     * Load dataset
     */
    public static void loadDataset(String expeditionCode, String inputFile) {
        boolean readyToLoad = false;
        // Create the process controller object
        ProcessController pc = new ProcessController(PROJECT_ID, expeditionCode);
        pc.setUserId(user.getUserId());
        pc.setOutputFolder(outputDirectory);

        System.out.println("Initializing ...");

        HashMap<String, String> fileMap = new HashMap<>();
        fileMap.put("dataset", inputFile);

        File configFile = new ConfigurationFileFetcher(pc.getProjectId(), pc.getOutputFolder(), true).getOutputFile();

        // Build the process object
        Process p = new Process.ProcessBuilder(datasetFileManager, pc)
                .addFiles(fileMap)
                .configFile(configFile)
                .build();

        System.out.println("\tinputFilename = " + inputFile);

        // If there is errors, tell the user and stop the operation
        if (!p.validate()) {
            System.out.println(pc.getMessages().toJSONString());
            return;
        }

        // Check for warnings
        // first part is see if want to just force it, bypassing any warning messages
        if (forceAll) {
            FimsPrinter.out.println("NOT CHECKING FOR WARNINGS");
            readyToLoad = true;
            // Check for warnings if not forceload
        } else if (pc.getHasWarnings()) {
            System.out.println("WARNINGS PRESENT, NOT LOADING");
            System.out.println(pc.getMessages().toJSONString());
        }

        // Run the loader
        if (readyToLoad) {
            p.upload(false, false, expeditionService);
        }
    }

}