package biocode.fims.auth.oauth2;

import biocode.fims.entities.OAuthClient;
import biocode.fims.service.OAuthProviderService;
import org.apache.commons.cli.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * This is a convenience class for registering new OAuth clients
 */
public class OAuthClientRegisterer {

    /**
     * Given a hostname, register a client app for oAuth use. Will generate a new client id and client secret
     * for the client app
     *
     * @param args
     */
    public static void main(String args[]) {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("/applicationContext.xml");
        OAuthProviderService providerService = applicationContext.getBean(OAuthProviderService.class);

        // Some classes to help us
        CommandLineParser clp = new GnuParser();
        CommandLine cl;

        Options options = new Options();
        options.addOption("c", "callback url", true, "The callback url of the client app");

        try {
            cl = clp.parse(options, args);
        } catch (UnrecognizedOptionException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        } catch (ParseException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        if (!cl.hasOption("c")) {
            System.out.println("You must enter a callback url");
            return;
        }

        String host = cl.getOptionValue("c");

        OAuthClient oAuthClient = providerService.createOAuthClient(host);
        System.out.println("The oAuth2 client app at host: " + host
                + ".\n will need the following information:\n\nclientId: "
                + oAuthClient.getClientId() + "\nclientSecret: " + oAuthClient.getClientSecret());
    }
}
