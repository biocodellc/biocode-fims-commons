package biocode.fims.run;

/**
 * @author rjewing
 */
public class ProcessControllerTest {

    /**
     * temporary test used to help refactor current validation messages into a ValidationMessages class.
     * <p>
     * This will serve to verify that the new class returns the same json response.
     */
//    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
//    @Test
//    public void getMessages_test() {
//        ProcessorStatus pc = new ProcessorStatus(0, null);
//
//        JSONObject expected = new JSONObject();
//        JSONObject worksheets = new JSONObject();
//        expected.put("worksheets", worksheets);
//        expected.put("config", null);
//
//        // test empty worksheet messages
//        assertEquals(expected, pc.getMessages());
//
//        JSONObject configMessages = new JSONObject();
//        expected.put("config", configMessages);
//        pc.setConfigMessages(configMessages);
//
//        // test empty worksheet and empty config messages
//        assertEquals(expected, pc.getMessages());
//
//        ConfigurationFileErrorMessager cfm = new ConfigurationFileErrorMessager();
//        cfm.add(null, "invalid value in TestingValidation", "TestingValidation");
//        configMessages = cfm.getMessages();
//        pc.setConfigMessages(configMessages);
//        expected.put("config", configMessages);
//
//        // test empty worksheet with config error warningMessage
//        assertEquals(expected, pc.getMessages());
//
//        String groupMessage1 = "Group 1";
//        RowMessage rm = new RowMessage("wrong value type", groupMessage1, RowMessage.ERROR);
//        pc.addMessage("Samples", rm);
//
//        JSONObject samplesWorksheet = new JSONObject();
//        JSONObject samplesErrors = new JSONObject();
//        JSONObject samplesWarnings = new JSONObject();
//
//        List<String> groupMessage1ErrorMessages = new ArrayList<>();
//        groupMessage1ErrorMessages.add("wrong value type");
//        samplesErrors.put(groupMessage1, groupMessage1ErrorMessages);
//
//        samplesWorksheet.put("errors", samplesErrors);
//        samplesWorksheet.put("warnings", samplesWarnings);
//        worksheets.put("Samples", samplesWorksheet);
//        expected.put("worksheets", worksheets);
//
//        // test 1 sheet warningMessage with 1 error
//        assertEquals(expected, pc.getMessages());
//
//        RowMessage rm2 = new RowMessage("another wrong value type", groupMessage1, RowMessage.ERROR);
//        pc.addMessage("Samples", rm2);
//
//        groupMessage1ErrorMessages.add("another wrong value type");
//        samplesErrors.put(groupMessage1, groupMessage1ErrorMessages);
//
//        samplesWorksheet.put("errors", samplesErrors);
//        worksheets.put("Samples", samplesWorksheet);
//        expected.put("worksheets", worksheets);
//
//        // test 1 sheet warningMessage with 2 errors
//        assertEquals(expected, pc.getMessages());
//
//        RowMessage rm3 = new RowMessage("warning warningMessage", groupMessage1, RowMessage.WARNING);
//        pc.addMessage("Samples", rm3);
//
//        List<String> groupMessage1WarningMessages = new ArrayList<>();
//        groupMessage1WarningMessages.add("warning warningMessage");
//        samplesWarnings.put(groupMessage1, groupMessage1WarningMessages);
//
//        samplesWorksheet.put("warnings", samplesWarnings);
//        worksheets.put("Samples", samplesWorksheet);
//        expected.put("worksheets", worksheets);
//
//        // test 1 sheet with 1 group warningMessage with 2 errors and 1 warning
//        assertEquals(expected, pc.getMessages());
//
//        String groupMessage2 = "Group 2";
//        RowMessage rm4 = new RowMessage("warning warningMessage group 2", groupMessage2, RowMessage.WARNING);
//        pc.addMessage("Samples", rm4);
//
//        List<String> groupMessage2WarningMessages = new ArrayList<>();
//        groupMessage2WarningMessages.add("warning warningMessage group 2");
//        samplesWarnings.put(groupMessage2, groupMessage2WarningMessages);
//
//        samplesWorksheet.put("warnings", samplesWarnings);
//        worksheets.put("Samples", samplesWorksheet);
//        expected.put("worksheets", worksheets);
//
//        // test 1 sheet with 2 group messages. 1 group warningMessage with 2 errors & 1 warning. 1 group warningMessage with 1 warning
//        assertEquals(expected, pc.getMessages());
//
//
//        JSONObject aWorksheet = new JSONObject();
//        JSONObject aErrors = new JSONObject();
//        JSONObject aWarnings = new JSONObject();
//
//        String groupMessage3 = "Group 3";
//        RowMessage rm5 = new RowMessage("warning warningMessage group 3", groupMessage3, RowMessage.WARNING);
//        pc.addMessage("another", rm5);
//
//        List<String> groupMessage3WarningMessages = new ArrayList<>();
//        groupMessage3WarningMessages.add("warning warningMessage group 3");
//        aWarnings.put(groupMessage3, groupMessage3WarningMessages);
//
//        aWorksheet.put("errors", aErrors);
//        aWorksheet.put("warnings", aWarnings);
//        worksheets.put("another", aWorksheet);
//        expected.put("worksheets", worksheets);
//
//        // test 2 sheets. 1 sheet with 1 group warningMessage and 1 warning. Other sheet with 2 group messages. 1 group warningMessage with 2 errors & 1 warning. 1 group warningMessage with 1 warning
//        assertEquals(expected, pc.getMessages());
//
//        System.out.println(expected.toJSONString());
//    }

}