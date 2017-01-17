package biocode.fims.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;

import java.io.IOException;

/**
 * Custom serializer that will override the upstream @JsonView if the {@link JsonViewOverride} annotation is present
 * for a property. This is useful for returning summary serializations of child objects when returning the detailed
 * view of a parent object.
 * @author RJ Ewing
 */
public class JsonViewOverrideSerializer extends JsonSerializer implements ContextualSerializer, ResolvableSerializer {
    private final JsonSerializer beanSerializer;
    private final Class overrideView;

    public JsonViewOverrideSerializer(JsonSerializer<?> serializer) {
        this(serializer, null);
    }

    public JsonViewOverrideSerializer(JsonSerializer<?> serializer, Class<?> overrideView) {
        this.beanSerializer = serializer;
        this.overrideView = overrideView;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        SerializerProvider effectiveProvider = provider;

        if (overrideView != provider.getActiveView()) {
            effectiveProvider = overrideProvider(overrideView, gen, provider);
        }

        beanSerializer.serialize(value, gen, effectiveProvider);
    }

    private SerializerProvider overrideProvider(Class<?> overrideView, JsonGenerator gen, SerializerProvider provider) {
        return ((DefaultSerializerProvider) provider).createInstance(provider.getConfig().withView(overrideView), ((ObjectMapper) gen.getCodec()).getSerializerFactory());
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        JsonSerializer<?> serializer = this.beanSerializer;

        if (serializer instanceof ContextualSerializer) {
            serializer = ((ContextualSerializer) serializer).createContextual(prov, property);
        }

        if (property != null) {
            JsonViewOverride jsonViewOverride = property.getAnnotation(JsonViewOverride.class);

            if (jsonViewOverride != null) {
                Class<?> jsonView = jsonViewOverride.value();
                serializer = new JsonViewOverrideSerializer(serializer, jsonView);
            }
        }

        return serializer;
    }

    @Override
    public void resolve(SerializerProvider provider) throws JsonMappingException {
        if ((beanSerializer != null)
                && (beanSerializer instanceof ResolvableSerializer)) {
            ((ResolvableSerializer) beanSerializer).resolve(provider);
        }

    }
}
