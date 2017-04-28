package biocode.fims.utils;

import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.settings.PathManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utils class for working with files
 */
public class FileUtils {
    private final static Logger logger = LoggerFactory.getLogger(FileUtils.class);

    public static String getExtension(String filename, String defaultExt) {
        String splitArray[] = filename.split("\\.");
        if (splitArray.length == 0) {
            // if no extension is found, then return defaultExt
            return defaultExt;
        } else {
            return splitArray[splitArray.length - 1];
        }
    }

    /**
     * take an InputStream and extension and write it to a file in the operating systems temp dir.
     *
     * @param is
     * @param ext
     *
     * @return
     */
    public static String saveTempFile(InputStream is, String ext) {
        String tempDir = System.getProperty("java.io.tmpdir");
        File f = PathManager.createUniqueFile( new StringGenerator().generateString(20) + '.' + ext, tempDir);

        try {
            OutputStream os = new FileOutputStream(f);
            try {
                byte[] buffer = new byte[4096];
                for (int n; (n = is.read(buffer)) != -1; )
                    os.write(buffer, 0, n);
            } finally {
                os.close();
            }
        } catch (IOException e) {
            logger.warn("IOException", e);
            return null;
        }
        return f.getAbsolutePath();
    }

    /**
     *
     * @param fileMap new filename, File pairs
     * @param outputDir
     * @return
     */
    public static File zip(Map<String, File> fileMap, String outputDir) {
        File zipFile = PathManager.createUniqueFile("files.zip", outputDir);

        try {
            ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(zipFile));

            // Create a buffer for reading the files
            byte[] buf = new byte[1024];

            for (Map.Entry<String, File> fileEntry: fileMap.entrySet()) {
                 FileInputStream in = new FileInputStream(fileEntry.getValue());
                zout.putNextEntry(new ZipEntry(fileEntry.getKey()));

                int len;
                while ((len = in.read(buf)) > 0) {
                    zout.write(buf, 0, len);
                }
                zout.closeEntry();
                in.close();
            }

            zout.close();
            return zipFile;
        } catch (IOException e) {
            throw new FimsRuntimeException(FileCode.WRITE_ERROR, 500, e);
        }
    }

}
