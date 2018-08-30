package biocode.fims.query.writers;

import biocode.fims.config.Config;
import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.query.QueryResults;
import biocode.fims.utils.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author RJ Ewing
 */
public class DelimitedTextQueryWriter implements QueryWriter {
    private final String delimiter;
    private final boolean isCsv;
    private final WriterSheetGenerator writerSheetGenerator;

    private WriterWorksheet worksheet;

    public DelimitedTextQueryWriter(QueryResults queryResults, String delimiter, Config config) {
        this.delimiter = delimiter;
        isCsv = StringUtils.equals(delimiter.trim(), ",");
        this.writerSheetGenerator = new WriterSheetGenerator(queryResults, config);
    }

    @Override
    public List<File> write() {
        List<WriterWorksheet> sheets = writerSheetGenerator.recordsToWriterSheets();

        List<File> files = sheets.stream()
                .map(this::writeWorksheet)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (files.size() == 0) throw new FimsRuntimeException(QueryCode.NO_RESOURCES, 400);

        return files;
    }

    private File writeWorksheet(WriterWorksheet worksheet) {
        this.worksheet = worksheet;

        String ext = (isCsv) ? "csv" : "txt";
        File file = FileUtils.createUniqueFile(worksheet.sheetName + "_output." + ext, System.getProperty("java.io.tmpdir"));

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file)))) {

            if (worksheet.data.size() == 0) {
                return null;
            }

            writeHeader(writer);
            writeData(writer);

        } catch (IOException e) {
            throw new FimsRuntimeException(FileCode.WRITE_ERROR, 500);
        }

        return file;
    }

    private void writeData(Writer writer) throws IOException {
        for (Map<String, String> record : worksheet.data) {

            for (String column : worksheet.columns) {
                String val = record.getOrDefault(column, "");
                if (isCsv) {
                    StringEscapeUtils.escapeCsv(writer, val);
                } else {
                    writer.write(val);
                }
                writer.write(delimiter);
            }

            writer.write("\n");
        }
    }

    private void writeHeader(Writer writer) throws IOException {
        for (String column : worksheet.columns) {
            if (isCsv) {
                StringEscapeUtils.escapeCsv(writer, column);
            } else {
                writer.write(column);
            }

            writer.write(delimiter);
        }

        writer.write("\n");
    }
}
