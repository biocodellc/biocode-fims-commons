package biocode.fims.rest.responses;

import javax.ws.rs.core.UriBuilder;

/**
 * @author RJ Ewing
 */
public class FileResponse extends UrlResponse {

    public FileResponse(UriBuilder uriBuilder, String fileId) {
        super(uriBuilder.path("files/" + fileId).build().toString());
    }

}
