package biocode.fims.rest.services.subResources;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.rest.FimsController;
import biocode.fims.utils.FileUtils;
import biocode.fims.utils.StringGenerator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.collections.keyvalue.MultiKey;

import java.io.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author rjewing
 */
public class ResumableUploadResource extends FimsController {
    protected final Map<MultiKey, UploadEntry> resumableUploads;

    public enum UploadType {
        NEW, RESUMABLE, RESUME;

        // allows case-insensitive deserialization by jersey
        public static UploadType fromString(String key) {
            return key == null
                    ? null
                    : UploadType.valueOf(key.toUpperCase());
        }
    }

    public ResumableUploadResource(FimsProperties props) {
        super(props);
        this.resumableUploads = new ConcurrentHashMap<>();
    }

    protected void clearExpiredUploadEntries() {
        List<MultiKey> keysToRemove = new ArrayList<>();

        ZonedDateTime expiredTime = ZonedDateTime.now(ZoneOffset.UTC).minusHours(24);

        for (Map.Entry<MultiKey, UploadEntry> e : resumableUploads.entrySet()) {
            if (e.getValue().lastUpdated.isBefore(expiredTime)) {
                keysToRemove.add(e.getKey());
            }
        }

        keysToRemove.forEach(resumableUploads::remove);
    }

    protected UploadEntry getUploadEntry(MultiKey key, UploadType uploadType) {
        UploadEntry uploadEntry = null;

        if (UploadType.RESUME.equals(uploadType)) {
            uploadEntry = resumableUploads.get(key);

            if (uploadEntry == null) {
                throw new BadRequestException("Failed to resume upload. Please try again with a new upload.");
            }
        } else if (UploadType.RESUMABLE.equals(uploadType)) {
            String tempDir = System.getProperty("java.io.tmpdir");
            File targetFile = FileUtils.createUniqueFile(StringGenerator.generateString(20) + ".zip", tempDir);

            uploadEntry = new UploadEntry(targetFile);

            resumableUploads.put(key, uploadEntry);
        }
        return uploadEntry;
    }

    protected InputStream resumeUpload(InputStream is, UploadEntry uploadEntry) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(uploadEntry.targetFile)) {
            byte[] buffer = new byte[8192];
            int size;
            while ((size = is.read(buffer, 0, buffer.length)) != -1) {
                fos.write(buffer, 0, size);
                uploadEntry.size += size;
                uploadEntry.lastUpdated = ZonedDateTime.now(ZoneOffset.UTC);
            }

            // close request InputStream and replace w/ FileInputStream
            is.close();
            return new FileInputStream(uploadEntry.targetFile);
        } catch (EOFException e) {
            throw e;
        }
    }


    @JsonIgnoreProperties({"lastUpdated", "targetFile"})
    protected static class UploadEntry {
        @JsonProperty
        int size;
        ZonedDateTime lastUpdated;
        File targetFile;

        UploadEntry(File targetFile) {
            this.targetFile = targetFile;
            lastUpdated = ZonedDateTime.now(ZoneOffset.UTC);
        }
    }
}
