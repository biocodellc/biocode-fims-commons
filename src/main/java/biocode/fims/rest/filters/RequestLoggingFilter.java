package biocode.fims.rest.filters;

import org.glassfish.jersey.message.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Simple filter to log requests to FIMS services. Much of this is taken from org.glassfish.jersey.logging.ServerLoggingFilter.
 * <p>
 * This filter was written because we didn't need such verbose logging that the jersey LoggingFeature provided, and we
 * only wanted to log requests.
 *
 * @author rjewing
 */
@Priority(Integer.MIN_VALUE)
public class RequestLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final String LOGGING_ID_PROPERTY = "RequestLoggingFilter.request.id";

    private static final int MAX_ENTITY_SIZE = 8 * 1024;

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static final MediaType TEXT_MEDIA_TYPE = new MediaType("text", "*");
    private static final Set<MediaType> READABLE_APP_MEDIA_TYPES = new HashSet<MediaType>() {{
        add(TEXT_MEDIA_TYPE);
        add(MediaType.APPLICATION_ATOM_XML_TYPE);
        add(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        add(MediaType.APPLICATION_JSON_TYPE);
        add(MediaType.APPLICATION_SVG_XML_TYPE);
        add(MediaType.APPLICATION_XHTML_XML_TYPE);
        add(MediaType.APPLICATION_XML_TYPE);
    }};

    private final static ConcurrentHashMap<Long, StringBuilder> toLog = new ConcurrentHashMap();

    final AtomicLong _id = new AtomicLong(0);

    @Context
    private HttpServletRequest servletRequest;


    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final long id = _id.incrementAndGet();
        requestContext.setProperty(LOGGING_ID_PROPERTY, id);

        final StringBuilder b = new StringBuilder();
        b.append(servletRequest.getRemoteAddr()).append(" ");

        b.append(requestContext.getMethod())
                .append(" ")
                .append(requestContext.getUriInfo().getRequestUri().toASCIIString())
                .append(" ");

        logHeader(b, "Referer", requestContext);
        logHeader(b, "User-Agent", requestContext);
        logHeader(b, "Fims-App", requestContext);

        if (requestContext.hasEntity() && isReadable(requestContext.getMediaType())) {
            requestContext.setEntityStream(
                    logInboundEntity(b, requestContext.getEntityStream(), MessageUtils.getCharset(requestContext.getMediaType())));
        }

        toLog.put(id, b);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        final Object requestId = requestContext.getProperty(LOGGING_ID_PROPERTY);

        if (requestId != null) {
            StringBuilder logBuilder = toLog.remove(requestId);
            logBuilder.append("\"Status: ");
            logBuilder.append(responseContext.getStatus());
            logBuilder.append("\"\n");

            logger.info(logBuilder.toString());
        }

    }

    private void logHeader(StringBuilder b, String header, ContainerRequestContext context) {
        String val = context.getHeaderString(header);

        if (val == null) {
            val = "-";
        }

        b.append("\"").append(val).append("\" ");
    }

    /**
     * Returns {@code true} if specified {@link MediaType} is considered textual.
     * <p>
     * See {@link #READABLE_APP_MEDIA_TYPES}.
     *
     * @param mediaType the media type of the entity
     * @return {@code true} if specified {@link MediaType} is considered textual.
     */
    private static boolean isReadable(MediaType mediaType) {
        if (mediaType != null) {
            for (MediaType readableMediaType : READABLE_APP_MEDIA_TYPES) {
                if (readableMediaType.isCompatible(mediaType)) {
                    return true;
                }
            }
        }
        return false;
    }

    private InputStream logInboundEntity(final StringBuilder b, InputStream stream, final Charset charset) throws IOException {
        if (!stream.markSupported()) {
            stream = new BufferedInputStream(stream);
        }
        stream.mark(MAX_ENTITY_SIZE + 1);
        final byte[] entity = new byte[MAX_ENTITY_SIZE + 1];
        final int entitySize = stream.read(entity);
        b.append("\"Entity: ");

        String e = new String(entity, 0, Math.min(entitySize, MAX_ENTITY_SIZE), charset);
        if (e.toLowerCase().contains("password")) {
            b.append("entity contains password, not logging");
        } else {
            b.append(e);
            if (entitySize > MAX_ENTITY_SIZE) {
                b.append("...more...");
            }
        }
        b.append("\" ");
        stream.reset();
        return stream;
    }
}
