package biocode.fims.serializers;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

/**
 * @author rjewing
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "name")
@JsonTypeIdResolver(EntityTypeIdResolver.class)
public class NonEntityTypeIdResolverTestClass {
}
