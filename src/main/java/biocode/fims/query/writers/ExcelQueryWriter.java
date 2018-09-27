package biocode.fims.query.writers;

import biocode.fims.config.Config;
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

    /**
     * Constructor for writing a network based workbook. If there is
     * only a single project, use the ExcelWorkbookWriter(Project, int, User)
     * constructor
     *
     * @param config
     * @param queryResults
     * @param naan
     */
    public ExcelQueryWriter(Config config, QueryResults queryResults, int naan) {
        super(config, naan);
        this.queryResults = queryResults;
        this.writerSheetGenerator = new WriterSheetGenerator(queryResults, config);
    }

    /**
     * Constructor for writing a project specific workbook
     *
     * @param project      may be null
     * @param queryResults
     * @param naan
     */
    public ExcelQueryWriter(Project project, QueryResults queryResults, int naan) {
        super(project, naan, null);
        this.queryResults = queryResults;
        this.writerSheetGenerator = new WriterSheetGenerator(queryResults, project.getProjectConfig());
    }

    @Override
    public List<File> write() {
        if (queryResults.isEmpty()) {
            throw new FimsRuntimeException(QueryCode.NO_RESOURCES, 400);
        }

        List<WriterWorksheet> sheets = writerSheetGenerator.recordsToWriterSheets();

        return Collections.singletonList(super.write(sheets));
    }
}
