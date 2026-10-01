package com.cgi.eoss.ogcapi.processes.serializers;

import com.cgi.eoss.ogcapi.processes.model.Link;
import com.cgi.eoss.ogcapi.processes.model.OgcapppkgExecutionUnit;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

import static com.cgi.eoss.ogcapi.processes.serializers.DeserializerUtils.isLink;

/**
 * Custom Json Deserializer for classes implementing {@link OgcapppkgExecutionUnit}.
 */
public class OgcapppkgExecutionUnitDeserializer extends StdDeserializer<OgcapppkgExecutionUnit> {

    public OgcapppkgExecutionUnitDeserializer() { this(OgcapppkgExecutionUnit.class); }

    protected OgcapppkgExecutionUnitDeserializer(Class<?> vc) { super(vc); }

    @Override
    public OgcapppkgExecutionUnit deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        ObjectNode root = mapper.readTree(jsonParser);

        String objectAsString = root.toString();
        if (isLink(root)) {
            return DeserializerUtils.deserialize(mapper, objectAsString, Link.class);
        }
        throw new IllegalArgumentException("Failed to deserialize: " + objectAsString);
    }
}
