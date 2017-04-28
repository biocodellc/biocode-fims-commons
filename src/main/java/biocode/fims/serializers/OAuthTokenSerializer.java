package biocode.fims.serializers;

import biocode.fims.models.OAuthToken;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Custom JSON serializer for {@link OAuthToken}. This is to include the static variables in the serialization
 */
public class OAuthTokenSerializer extends JsonSerializer<OAuthToken> {
    @Override
    public void serialize(OAuthToken token, JsonGenerator jgen, SerializerProvider provider)
        throws IOException {
        jgen.writeStartObject();
        jgen.writeStringField("access_token", token.getToken());
        jgen.writeStringField("refresh_token", token.getRefreshToken());
        jgen.writeStringField("token_type", token.TOKEN_TYPE);
        jgen.writeStringField("expires_in", String.valueOf(token.EXPIRES_IN));
        if (token.getState() != null) {
            jgen.writeStringField("state", token.getState());
        }
        jgen.writeEndObject();
    }
}
