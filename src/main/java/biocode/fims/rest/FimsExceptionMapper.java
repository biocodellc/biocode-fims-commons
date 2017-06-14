package biocode.fims.rest;

import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.FimsAbstractException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.fimsExceptions.UnauthorizedRequestException;
import biocode.fims.run.ProcessorStatus;
import biocode.fims.utils.ErrorInfo;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.List;

/**
 * class to catch an exception thrown from a rest service and map the necessary information to a request
 */
@Provider
public class FimsExceptionMapper implements ExceptionMapper<Exception> {
    @Context
    protected HttpServletRequest request;
    @Context
    protected ExtendedUriInfo uriInfo;
    @Context
    protected HttpHeaders httpHeaders;

    protected static Logger logger = LoggerFactory.getLogger(FimsExceptionMapper.class);

    @Override
    public Response toResponse(Exception e) {
        logException(e);
        ErrorInfo errorInfo = getErrorInfo(e);
        String mediaType;

        HttpSession session = request.getSession();

        // check if the called service is expected to return HTML of JSON
        // try to get the mediaType of the matched method. If an exception was thrown before the resource was constructed
        // then getMatchedMethod will return null. If that's the case then we should look to the accept header for the
        // correct response type.
        try {
            mediaType = uriInfo.getMatchedResourceMethod().getProducedTypes().get(0).toString();
        } catch(IndexOutOfBoundsException | NullPointerException ex) {
            List<MediaType> accepts = httpHeaders.getAcceptableMediaTypes();
            logger.debug("NullPointerException thrown while retrieving mediaType in FimsExceptionMapper.java");
            // if request accepts JSON, return the error in JSON, otherwise use html
            if (accepts.contains(MediaType.TEXT_HTML_TYPE)) {
                mediaType = MediaType.TEXT_HTML;
            } else {
                mediaType = MediaType.APPLICATION_JSON;
            }
        }

        if (mediaType.contains( MediaType.TEXT_HTML)) {
            // add errorInfo to session to be used on custom error page
            session.setAttribute("errorInfo", errorInfo);
            return Response.status(errorInfo.getHttpStatusCode()).build();
        } else {
            return Response.status(errorInfo.getHttpStatusCode())
                    .entity(errorInfo.toJSON())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    // method to set the relevant information in ErrorInfo
    protected ErrorInfo getErrorInfo(Exception e) {
        String usrMessage;
        String developerMessage = null;
        Integer httpStatusCode = getHttpStatus(e);

        if (e instanceof FimsAbstractException) {
            usrMessage = ((FimsAbstractException) e).getUsrMessage();
            developerMessage = ((FimsAbstractException) e).getDeveloperMessage();
        } else {
            usrMessage = "Server Error";
        }

        return new ErrorInfo(usrMessage, developerMessage, httpStatusCode, (Exception) e);

    }

    protected Integer getHttpStatus(Exception e) {
        // if the throwable is an instance of WebApplicationException, get the status code
        if (e instanceof WebApplicationException) {
            return ((WebApplicationException) e).getResponse().getStatus();
        } else if (e instanceof FimsAbstractException) {
            return ((FimsAbstractException) e).getHttpStatusCode();
        } else {
            return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        }
    }

    protected void logException(Exception e) {
        // don't log BadRequestexceptions or UnauthorizedRequestExceptions or ForbiddenRequestExceptions
        if (!(e instanceof BadRequestException || e instanceof UnauthorizedRequestException ||
                e instanceof ForbiddenRequestException)) {
            logger.error("{} thrown.", e.getClass().toString(), e);
        }
    }
}
