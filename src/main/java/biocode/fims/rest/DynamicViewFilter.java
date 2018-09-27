package biocode.fims.rest;

import biocode.fims.rest.responses.DynamicViewResponse;
import com.fasterxml.jackson.annotation.JsonView;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;

/**
 * Interceptor to dynamically set the jackson view for serialization
 *
 * @author rjewing
 */

public class DynamicViewFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext context) throws IOException {
        if (context.getEntity() instanceof DynamicViewResponse) {
            DynamicViewResponse response = (DynamicViewResponse) context.getEntity();

            if (response.getEntity() == null) {
                context.setStatus(204);
                return;
            }

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
            Annotation[] annotations = context.getEntityAnnotations();
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
            }
            context.setEntity(response.getEntity(), annotations, context.getMediaType());
        }

    }
}
