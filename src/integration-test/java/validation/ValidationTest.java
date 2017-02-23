package validation;

import biocode.fims.digester.Validation;
import biocode.fims.digester.Mapping;
import biocode.fims.fileManagers.fimsMetadata.FimsMetadataFileManager;
import biocode.fims.renderers.MessagesGroup;
import biocode.fims.renderers.SheetMessages;
import biocode.fims.renderers.SimpleMessage;
import biocode.fims.run.ProcessController;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author rjewing
 */
public class ValidationTest {

    private Mapping mapping;
    private FimsMetadataFileManager fm;
    private ProcessController pc;
    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        classLoader = getClass().getClassLoader();
    }

    @Test
    public void missing_column_fails_hash_building() {
        File datasetFile = new File(classLoader.getResource("missingColumnFailHashDataset.csv").getFile());
        init(datasetFile);

        fm.validate();

        SheetMessages expected = new SheetMessages();
        expected.addErrorMessage("Initial Spreadsheet check", new SimpleMessage("Error building hashes.  Likely a required column constraint failed."));

        JSONObject worksheetMessages = getValidationMessages();

        assertEquals(sheetMessagesToJSONObject(expected), worksheetMessages);

    }

    @Test
    public void duplicate_column_fails() {
        File datasetFile = new File(classLoader.getResource("duplicateColumnDataset.csv").getFile());
        init(datasetFile);

        fm.validate();

        SheetMessages expected = new SheetMessages();
        expected.addErrorMessage("Initial Spreadsheet check", new SimpleMessage("DUPLICATE_COLUMNS"));

        JSONObject worksheetMessages = getValidationMessages();

        assertEquals(sheetMessagesToJSONObject(expected), worksheetMessages);

    }

    private JSONObject getValidationMessages() {
        JSONObject worksheets =(JSONObject) pc.getMessages().get("worksheets");
        return (JSONObject) worksheets.get(mapping.getDefaultSheetName());
    }


    private JSONObject sheetMessagesToJSONObject(SheetMessages sheetMessages) {
        JSONObject messages = new JSONObject();

        JSONObject errors = new JSONObject();
        for (MessagesGroup g: sheetMessages.getErrorMessages()) {
            errors.put(g.getName(), g.messages());
        }

        JSONObject warnings = new JSONObject();
        for (MessagesGroup g: sheetMessages.getWarningMessages()) {
            warnings.put(g.getName(), g.messages());
        }

        messages.put("errors", errors);
        messages.put("warnings", warnings);
        return messages;
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
