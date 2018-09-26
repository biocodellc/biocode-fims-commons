package biocode.fims.rest;

import biocode.fims.rest.responses.DynamicViewResponse;
import com.fasterxml.jackson.annotation.JsonView;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;

/**
 * Interceptor to dynamically set the jackson view for serialization
 *
 * @author rjewing
 */

public class DynamicViewWriterInterceptor implements WriterInterceptor {

    @Override
    public void aroundWriteTo(WriterInterceptorContext context)
            throws IOException, WebApplicationException {
        if (context.getEntity() instanceof DynamicViewResponse) {
            DynamicViewResponse response = (DynamicViewResponse) context.getEntity();

            Annotation view = new JsonView() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return JsonView.class;
                }

                @Override
                public Class<?>[] value() {
                    return new Class[]{response.getView()};
                }
            };

            boolean setView = false;
            Annotation[] annotations = context.getAnnotations();
            for (int i = 0; i < annotations.length; i++) {
                Annotation annotation = annotations[i];

                if (annotation instanceof JsonView) {
                    // dynamically replace the JsonView annotation
                    annotations[i] = view;
                    setView = true;
                    break;
                }
            }

            if (!setView) {
                annotations = Arrays.copyOf(annotations, annotations.length + 1);
                annotations[annotations.length - 1] = view;
                context.setAnnotations(annotations);
            }

            context.setEntity(response.getEntity());
            context.setGenericType(response.getEntity().getClass());
        }
        context.proceed();
    }
}
