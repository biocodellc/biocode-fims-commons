package biocode.fims.serializers;


import com.fasterxml.jackson.annotation.JsonView;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author RJ Ewing
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.FIELD,
        ElementType.PARAMETER // since 2.5
})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonViewOverride {
    /**
     * View that annotated element is part of. This will override and active {@link JsonView}
     * set upstream.
     */
    public Class<?> value();
}
