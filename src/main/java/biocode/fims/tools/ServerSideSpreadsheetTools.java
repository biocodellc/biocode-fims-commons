package biocode.fims.tools;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.*;

/**
 * Class for saving and modifying upload spreadsheets on the server
 */
public class ServerSideSpreadsheetTools {
    Workbook workbook;

    public ServerSideSpreadsheetTools(File sourceFile) {
        try {
            FileInputStream fis = new FileInputStream(sourceFile);
            this.workbook = WorkbookFactory.create(fis);
        } catch (IOException|InvalidFormatException e) {
            throw new FimsRuntimeException(500, e);
        }
    }

    /**
     * Write the resulting spreadsheet
     *
     * @param outputFile
     */
    public void write(File outputFile) {
        try {
            FileOutputStream fileOut = new FileOutputStream(outputFile);
            workbook.write(fileOut);
            fileOut.close();
        } catch (IOException e) {
            throw new FimsRuntimeException(500, e);
        }
    }
}
