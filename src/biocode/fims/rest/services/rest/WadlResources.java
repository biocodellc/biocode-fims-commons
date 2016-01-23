package biocode.fims.rest.services.rest;

import biocode.fims.rest.FimsService;
import org.glassfish.jersey.server.wadl.WadlApplicationContext;
import org.glassfish.jersey.server.wadl.internal.ApplicationDescription;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Describes the REST service in a human readable manner
 */
@Produces({"application/vnd.sun.wadl+xml", "application/xml"})
@Singleton
@Path("fims.wadl")
public final class WadlResources extends FimsService {


    private WadlApplicationContext wadlContext;

    private com.sun.research.ws.wadl.Application application;

    private byte[] wadlXmlRepresentation;

    public WadlResources(@Context WadlApplicationContext wadlContext) {
        this.wadlContext = wadlContext;
    }

    @GET
    public synchronized Response getWadl(@Context UriInfo uriInfo) {

        String styleSheetUrl = appRoot + "wadl.xsl";
        ApplicationDescription ae = wadlContext.getApplication(uriInfo, true);
        this.application = ae.getApplication();

        try {

            Marshaller marshaller = wadlContext.getJAXBContext()
                    .createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Writer writer = new OutputStreamWriter(os);

            writer.write("<?xml version='1.0'?>");
            writer.write("\n");
            writer.write("<?xml-stylesheet type=\"text/xsl\" href=\"" + styleSheetUrl + "\" ?>");
            writer.write("\n");

            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            marshaller.marshal(application, writer);
            writer.flush();
            writer.close();
            wadlXmlRepresentation = os.toByteArray();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(application).build();
        }

        return Response.ok(new ByteArrayInputStream(wadlXmlRepresentation))
                .build();
    }
}
