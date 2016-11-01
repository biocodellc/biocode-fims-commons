package biocode.fims.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

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
        File f = new File(tempDir, new StringGenerator().generateString(20) + '.' + ext);

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

}
