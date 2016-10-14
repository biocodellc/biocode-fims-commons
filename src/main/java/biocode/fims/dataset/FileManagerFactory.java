package biocode.fims.dataset;

import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.run.ProcessController;
import biocode.fims.utils.FileUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory class to instentiate the appropriate {@link FileManager} implementation for the given file
 */
public class FileManagerFactory {
    private static final List<FileManager> fileManagerNames;

    static {
        fileManagerNames = new ArrayList<>();
        fileManagerNames.add(new DatasetFileManager());
    }

    public static FileManager getFileManager(String filename, ProcessController processController) {
        String ext = FileUtils.getExtension(filename, null);
        for (FileManager fm: fileManagerNames) {
            for (String supportedExt : fm.getExtensions()) {
                if (supportedExt.equalsIgnoreCase(ext)) {
                    try {
                        return fm.getClass().getDeclaredConstructor(String.class, ProcessController.class)
                                .newInstance(filename, processController);
                    } catch (NoSuchMethodException|InstantiationException|IllegalAccessException|InvocationTargetException e) {
                        throw new ServerErrorException(e);
                    }
                }
            }

        }
        return null;
    }

}
