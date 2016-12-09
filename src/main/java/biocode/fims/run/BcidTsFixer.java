package biocode.fims.run;

import biocode.fims.application.config.FimsAppConfig;
import biocode.fims.entities.Bcid;
import biocode.fims.service.BcidService;
import biocode.fims.settings.FimsPrinter;
import org.apache.commons.cli.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * class to fix the bcid ts for all bcids where ezidRequest = false.
 * This sets the ts to a different arbitrary ts for each bcid, so no 2 bcids have the same ts
 * user bcidTsFixer.py to generate mysql update script for bcids where ezidRequest = true
 *
 * @author RJ Ewing
 */
public class BcidTsFixer {
    private BcidService bcidService;

    public BcidTsFixer(BcidService bcidService) {
        this.bcidService = bcidService;
    }

    public void fixBcidTs(String resourceType) {
        List<Bcid> bcids = bcidService.getBcidsWithOutEzidRequest();

        // sort on bcidId desc
        bcids.sort(Comparator.comparingInt(Bcid::getBcidId).reversed());

        Calendar now = Calendar.getInstance();

        for (Bcid bcid : bcids) {
            if (resourceType != null) {
                if (resourceType.equals(bcid.getResourceType())) {
                    bcid.setTs(now.getTime());

                    bcidService.update(bcid);

                    now.add(Calendar.SECOND, -1);
                }
            } else {

                bcid.setTs(now.getTime());

                bcidService.update(bcid);

                now.add(Calendar.SECOND, -1);

            }
        }

    }

    public static void main(String[] args) throws IOException {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(FimsAppConfig.class);
        BcidService bcidService = applicationContext.getBean(BcidService.class);

        String resourceType = null;

        // Some classes to help us
        CommandLineParser clp = new GnuParser();
        HelpFormatter helpf = new HelpFormatter();
        CommandLine cl;

        // Define our commandline options
        Options options = new Options();
        options.addOption("h", "help", false, "print this help message and exit");
        options.addOption("r", "resourceType", true, "Bcid.resourceType");

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

        if (cl.hasOption("r")) {
            resourceType = cl.getOptionValue("r");
        }

        if (resourceType == null) {
            FimsPrinter.out.print("Are you sure you do not want to specify a resourceType?: ");
            //  open up standard input
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            //  read the response from the command-line; need to use try/catch with the
                if (!br.readLine().equalsIgnoreCase("y")) {
                    return;
                }
        }

        BcidTsFixer bcidTsFixer = new BcidTsFixer(bcidService);
        bcidTsFixer.fixBcidTs(resourceType);

    }
}
