package biocode.fims.settings;

import biocode.fims.fimsExceptions.FimsConnectorException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;


/**
 * biocode.fims.settings.SettingsManager provides a generic way to configure BiSciCol core classes
 * from a properties file.  The basic idea is that any object that supports
 * the Configurable interface can be passed a biocode.fims.settings.SettingsManager, and it will then
 * use the biocode.fims.settings.SettingsManager to configure itself.  biocode.fims.settings.SettingsManager is implemented
 * as a singleton to ensure that all BiSciCol objects use common configuration
 * information.
 */
public class SettingsManager {
    private static SettingsManager instance = null;

    private Properties props;
    private String propsFile;

    private SettingsManager(String propsFile) {
        this.propsFile = propsFile;
        loadProperties();
    }

    /**
     * Get a reference to the global biocde.fims.SettingsManager object. The SettingsManager should have
     * been initialized upon startup of Jersey app or the class main method. The properties file should be a
     * context-param in the web.xml file named propsFilename.
     * @return
     */
    public static SettingsManager getInstance() {
        if (instance == null) {
            throw new FimsConnectorException("SettingsManager should be initialized on startup using the FimsApplication" +
                "class or in the calling class main method. Check web.xml to verify correct settings.", 500);
        }
        return instance;
    }
    /**
     * Get a reference to the global biocode.fims.settings.SettingsManager object, specifying a
     * properties file to use.  If this is the first request for a
     * biocode.fims.settings.SettingsManager instance, then a new biocode.fims.settings.SettingsManager object will be
     * created using the specified properties file.  Otherwise, the existing
     * biocode.fims.settings.SettingsManager will be returned and the specified properties file is
     * ignored.
     *
     * @param file A properties file to use in initializing the
     *                  biocode.fims.settings.SettingsManager.
     *
     * @return A reference to the global biocode.fims.settings.SettingsManager object.
     */
    public static SettingsManager getInstance(String file) {
        if (instance == null) {
            String propsfile = Thread.currentThread().getContextClassLoader().getResource(file).getFile();
            instance = new SettingsManager(propsfile);
        }

        return instance;
    }

    /**
     * Get the path of the properties file associated with this biocode.fims.settings.SettingsManager.
     *
     * @return The path of the properties file used by this biocode.fims.settings.SettingsManager.
     */
    public String getPropertiesFile() {
        return propsFile;
    }

    /**
     * Specify a properties file for this biocode.fims.settings.SettingsManager to use.
     *
     * @param propsfile The path to a properties file.
     */
    public void setPropertiesFile(String propsfile) {
        this.propsFile = propsfile;
    }

    /**
     * Attempt to load the properties file associated with this biocode.fims.settings.SettingsManager.
     * This method must be called to properly initialize the biocode.fims.settings.SettingsManager
     * before it can be used by Configurable classes.
     */
    private void loadProperties() {
        try {
            props = new Properties();
            FileInputStream in = new FileInputStream(propsFile);

            props.load(in);
            in.close();
        } catch (FileNotFoundException e) {
            throw new FimsConnectorException("Unable to find settings file " + propsFile +
                    ". Make sure you have included this file in the root class of your deployed application!", 500, e);
        } catch (IOException e) {
            throw new FimsConnectorException("Error while loading the settings file " + propsFile + ". Is the file correct?",
                    500, e);
        }
    }

    /**
     * Get the value associated with the specified key.  If the key is not found
     * in the properties file, then an empty string is returned.
     *
     * @param key The key to search for in the properties file.
     *
     * @return The value associated with the key if it exists, otherwise, an
     *         empty string.
     */
    public String retrieveValue(String key) {
        return retrieveValue(key, "");
    }

    /**
     * Get the value associated with the specified key; return a default value
     * if the key is not found.  The string specified by defaultval is returned
     * as the default value if the key cannot be found.
     *
     * @param key        The key to search for in the properties file.
     * @param defaultval The default value to return if the key cannot be found.
     *
     * @return The value associated with the key if it exists, otherwise, the
     *         specified default value.
     */
    public String retrieveValue(String key, String defaultval) {
        return props.getProperty(key, defaultval);
    }
}
