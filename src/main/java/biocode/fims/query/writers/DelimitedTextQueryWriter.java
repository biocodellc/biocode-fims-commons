package biocode.fims.query.writers;

import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.projectConfig.ProjectConfig;
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

    public DelimitedTextQueryWriter(QueryResults queryResults, String delimiter, ProjectConfig config) {
        this.delimiter = delimiter;
        isCsv = StringUtils.equals(delimiter.trim(), ",");
        this.writerSheetGenerator = new WriterSheetGenerator(queryResults, config);
    }

    @Override
    public File write() {
        List<WriterWorksheet> sheets = writerSheetGenerator.recordsToWriterSheets();

        List<File> files = sheets.stream()
                .map(this::writeWorksheet)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (files.size() == 0) throw new FimsRuntimeException(QueryCode.NO_RESOURCES, 400);

        if (files.size() == 1) {
            return files.get(0);
        } else {
            Map<String, File> fileMap = new HashMap<>();

            for (File f : files) {
                // we create a uniqueFile which may end up like sample.2.csv. This will make it sample.csv
                String name = f.getName().replaceFirst("\\.\\d+\\.", ".");
                fileMap.put(name, f);
            }

            return FileUtils.zip(fileMap, System.getProperty("java.io.tmpdir"));
        }
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
