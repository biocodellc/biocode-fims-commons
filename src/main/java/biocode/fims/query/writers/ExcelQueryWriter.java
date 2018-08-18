package biocode.fims.query.writers;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.models.Project;
import biocode.fims.query.QueryResults;
import biocode.fims.run.ExcelWorkbookWriter;

import java.io.File;
import java.util.*;

/**
 * @author RJ Ewing
 */
public class ExcelQueryWriter extends ExcelWorkbookWriter implements QueryWriter {
    private final WriterSheetGenerator writerSheetGenerator;
    private QueryResults queryResults;

    public ExcelQueryWriter(Project project, QueryResults queryResults, int naan) {
        super(project, naan);
        this.queryResults = queryResults;
        this.writerSheetGenerator = new WriterSheetGenerator(queryResults, project.getProjectConfig());
    }

    @Override
    public File write() {
        if (queryResults.isEmpty()) {
            throw new FimsRuntimeException(QueryCode.NO_RESOURCES, 400);
        }

        List<WriterWorksheet> sheets = writerSheetGenerator.recordsToWriterSheets();

        return super.write(sheets);
    }
}
