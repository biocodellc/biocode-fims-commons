package biocode.fims.fileManagers.fimsMetadata;

import biocode.fims.digester.Validation;
import biocode.fims.digester.Mapping;
import biocode.fims.renderers.RowMessage;
import biocode.fims.renderers.EntityMessages;
import biocode.fims.renderers.SimpleMessage;
import biocode.fims.run.ProcessController;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import validation.EntityMessagesUtils;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author rjewing
 */
public class FimsMetadataValidationTest {

    private Mapping mapping;
    private FimsMetadataFileManager fm;
    private ProcessController pc;
    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        classLoader = getClass().getClassLoader();
    }

    @Test
    public void test_all_validation_rules_run_as_expected() {
        File datasetFile = new File(classLoader.getResource("testDataset.csv").getFile());
        init(datasetFile);
        fm.validate();

        EntityMessages expected = getExpectedMessages();
        JSONObject worksheetMessages = getValidationMessages();

        Assert.assertEquals(EntityMessagesUtils.sheetMessagesToJSONObject(expected), worksheetMessages);
    }

    private EntityMessages getExpectedMessages() {
        EntityMessages entityMessages = new EntityMessages("Samples", "Samples");

        // validForURI rule
        entityMessages.addErrorMessage("Non-valid URI characters",
                new SimpleMessage("\"materialSampleID\" contains some bad characters: not valid uri"));
        entityMessages.addErrorMessage("Non-valid URI characters",
                new SimpleMessage("\"materialSampleID\" contains some bad characters: not valid uri"));

        /* validDataTypeFormat rules */
        // date format rule
        entityMessages.addErrorMessage("Invalid DataFormat",
                new SimpleMessage("\"Observation_Date\" contains invalid date values. Format must be an Excel DATE or one of [YYYY-MM-DD]: 100, 12/15/17, 12-15-2017"));

        // uniqueValue rule
        entityMessages.addErrorMessage("Unique value constraint did not pass",
                new SimpleMessage("\"materialSampleID\" column is defined as unique but some values used more than once: 1"));

        // RequiredColumns error rule
        entityMessages.addErrorMessage("Missing column(s)",
                new SimpleMessage("\"materialSampleID\" has a missing cell value"));

        // case-sensitive controlledVocabulary rule
        entityMessages.addErrorMessage("\"hasLegs\" contains invalid value <a  href=\"#\" onclick=\"list('yesNo','hasLegs');\">see list</a>",
                new RowMessage("\"no\" not an approved \"hasLegs\"", 1));
        entityMessages.addErrorMessage("\"hasLegs\" contains invalid value <a  href=\"#\" onclick=\"list('yesNo','hasLegs');\">see list</a>",
                new RowMessage("\"not sure\" not an approved \"hasLegs\"", 3));
        entityMessages.addErrorMessage("\"hasLegs\" contains invalid value <a  href=\"#\" onclick=\"list('yesNo','hasLegs');\">see list</a>",
                new RowMessage("\"n/a\" not an approved \"hasLegs\"", 4));

        // case-insensitive controlledVocabulary rule
        entityMessages.addErrorMessage("\"phylum\" contains invalid value <a  href=\"#\" onclick=\"list('phylum','phylum');\">see list</a>",
                new RowMessage("\"unknown\" not an approved \"phylum\"", 3));

        return entityMessages;
    }


    @Test
    public void missing_column_fails_hash_building() {
        File datasetFile = new File(classLoader.getResource("missingColumnFailHashDataset.csv").getFile());
        init(datasetFile);

        fm.validate();

        EntityMessages expected = new EntityMessages("Samples", "Samples");
        expected.addErrorMessage("Initial Spreadsheet check", new SimpleMessage("Error building hashes.  Likely a required column constraint failed."));

        JSONObject worksheetMessages = getValidationMessages();

        assertEquals(EntityMessagesUtils.sheetMessagesToJSONObject(expected), worksheetMessages);

    }

    @Test
    public void duplicate_column_fails() {
        File datasetFile = new File(classLoader.getResource("duplicateColumnDataset.csv").getFile());
        init(datasetFile);

        fm.validate();

        EntityMessages expected = new EntityMessages("Samples", "Samples");
        expected.addErrorMessage("Initial Spreadsheet check", new SimpleMessage("DUPLICATE_COLUMNS"));

        JSONObject worksheetMessages = getValidationMessages();

        assertEquals(EntityMessagesUtils.sheetMessagesToJSONObject(expected), worksheetMessages);

    }

    private JSONObject getValidationMessages() {
        JSONObject worksheets =(JSONObject) pc.getMessages().get("worksheets");
        return (JSONObject) worksheets.get(mapping.getDefaultSheetName());
    }


    private void init(File datasetFile) {
        File configFile = new File(classLoader.getResource("test.xml").getFile());

        mapping = new Mapping();
        mapping.addMappingRules(configFile);
        Validation validation = new Validation();
        validation.addValidationRules(configFile, mapping);

        pc = new ProcessController(0, null);
        pc.setMapping(mapping);
        pc.setValidation(validation);

        pc.setOutputFolder(System.getProperty("java.io.tmpdir"));

        fm = new FimsMetadataFileManager(null, null, null, null, null);
        try {
            fm.setFilename(datasetFile.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        fm.setProcessController(pc);
    }
}
