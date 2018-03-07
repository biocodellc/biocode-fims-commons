package biocode.fims.query.writers;

import biocode.fims.query.QueryResult;
import biocode.fims.query.QueryResults;
import biocode.fims.utils.FileUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
public abstract class AbstractQueryWriter implements QueryWriter {
    protected final QueryResults queryResults;

    protected AbstractQueryWriter(QueryResults queryResults) {
        this.queryResults = queryResults;
    }

    abstract File writeResult(QueryResult queryResult);

    @Override
    public File write() {
        List<File> files = queryResults.stream()
                .map(this::writeResult)
                .collect(Collectors.toList());

        if (files.size() == 1) {
            return files.get(0);
        } else {
            Map<String, File> fileMap = new HashMap<>();

            for (File f: files) {
                fileMap.put(f.getName(), f);
            }

            return FileUtils.zip(fileMap, System.getProperty("java.io.tmpdir"));
        }
    }
}
