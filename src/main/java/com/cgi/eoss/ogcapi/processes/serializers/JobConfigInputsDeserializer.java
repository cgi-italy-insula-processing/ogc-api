package com.cgi.eoss.ogcapi.processes.serializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Custom deserializer for JobConfig's inputs map.
 * This deserializer is used to convert JSON input into a Map<String, Object>.
 * It handles the special case where the key "collection" is present in the input.
 */
public class JobConfigInputsDeserializer extends StdDeserializer<Map<String, Object>> {

    /**
     * Default constructor for the deserializer.
     * It initializes the deserializer with a Map class type.
     */
    public JobConfigInputsDeserializer() { this(Map.class); }

    protected JobConfigInputsDeserializer(Class<?> vc) { super(vc); }

    @Override
    public Map<String, Object> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        return deserializeInputs(mapper.readTree(jsonParser), mapper);
    }

    private static Map<String, Object> deserializeInputs(Object root, ObjectMapper mapper) {
        if (!(root instanceof ObjectNode objectNode)) {
            Class<?> rootClass = root.getClass();
            throw new IllegalArgumentException("Failed to deserialize JobGetResponse object: " + root + " of type " + rootClass);
        }

        return deserializeMap(mapper.convertValue(objectNode, Map.class), mapper);
    }

    private static Map<String, Object> deserializeMap(Map<?, ?> inputs, ObjectMapper mapper) {
        return inputs.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> (String) entry.getKey(),
                        entry -> deserializeJobInputValue(
                                (String) entry.getKey(), entry.getValue(), mapper
                        )
                ));
    }

    private static Object deserializeJobInputValue(String key, Object value, ObjectMapper mapper) {
        if ("collection".equals(key)) {
            return ((List<?>) value).stream()
                    .map(input -> DeserializerUtils.deserialize(mapper, (String) input, Map.class))
                    .collect(Collectors.toList());
        }
        return value;
    }

}
