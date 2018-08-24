package biocode.fims.utils;

import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utils class for working with files
 */
public class FileUtils {
    private final static Logger logger = LoggerFactory.getLogger(FileUtils.class);

    /**
     * Create new file in given folder, add incremental number to base if filename already exists.
     *
     * @param pFilename Name of the file.
     * @return The new file.
     */
    public static File createUniqueFile(String pFilename, String pOutputFolder) {

        // Get just the filename
        File fileFilename = new File(pFilename);
        String fileName = fileFilename.getName();

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1)
            dotIndex = fileName.length();
        String base = fileName.substring(0, dotIndex);
        String ext = fileName.substring(dotIndex);

        File file = new File(pOutputFolder, fileName);
        int i = 1;
        while (file.exists())
            file = new File(pOutputFolder, base + "." + i++ + ext);
        return file;
    }

    /**
     * Create a File in a given folder and overwrite any existing file.
     *
     * @param pFilename Name of the file.
     * @return The new file.
     */
    public static File createFile(String pFilename, String pOutputFolder) {
        File file = new File(pOutputFolder, pFilename);
        if (file.exists()) {
            file.delete();
            return new File(pOutputFolder, pFilename);
        } else {
            return file;
        }
    }

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
        File f = createUniqueFile( new StringGenerator().generateString(20) + '.' + ext, tempDir);

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

    public static File zip(List<File> files) {
        Map<String, File> fileMap = new HashMap<>();

        for (File f : files) {
            // we create a uniqueFile which may end up like sample.2.csv. This will make it sample.csv
            String name = f.getName().replaceFirst("\\.\\d+\\.", ".");
            fileMap.put(name, f);
        }

        return FileUtils.zip(fileMap, System.getProperty("java.io.tmpdir"));
    }

    /**
     *
     * @param fileMap new filename, File pairs
     * @param outputDir
     * @return
     */
    public static File zip(Map<String, File> fileMap, String outputDir) {
        File zipFile = createUniqueFile("files.zip", outputDir);

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
