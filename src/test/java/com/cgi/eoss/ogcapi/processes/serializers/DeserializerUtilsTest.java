package com.cgi.eoss.ogcapi.processes.serializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DeserializerUtilsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testIsLink_ReturnsTrue_WhenObjectIsLink() throws Exception {
        ObjectNode objectNode;
        try (JsonParser jsonParser = buildParser("{\"href\": \"href\", \"rel\": \"rel\"}")) {
            objectNode = objectMapper.readTree(jsonParser);
        }
        assertThat(DeserializerUtils.isLink(objectNode)).isTrue();
    }

    @Test
    public void testIsLink_ReturnsFalse_WhenObjectIsNotLink() throws Exception {
        ObjectNode objectNode;
        try (JsonParser jsonParser = buildParser("{\"someKey\": \"someValue\"}")) {
            objectNode = objectMapper.readTree(jsonParser);
        }
        assertThat(DeserializerUtils.isLink(objectNode)).isFalse();
    }

    @Test
    public void testIsBbox_ReturnsTrue_WhenObjectIsBbox() throws Exception {
        ObjectNode objectNode;
        try (JsonParser jsonParser = buildParser("{\"bbox\": [10,20,30,40], \"crs\": \"someLink\"}")) {
            objectNode = objectMapper.readTree(jsonParser);
        }
        assertThat(DeserializerUtils.isBbox(objectNode)).isTrue();
    }

    @Test
    public void testIsBbox_ReturnsTrue_WhenObjectIsNotBbox() throws Exception {
        ObjectNode objectNode;
        try (JsonParser jsonParser = buildParser("{\"someKey\": \"someValue\"}")) {
            objectNode = objectMapper.readTree(jsonParser);
        }
        assertThat(DeserializerUtils.isBbox(objectNode)).isFalse();
    }

    @Test
    public void testDeserialize_DeserializesObject() {
        assertThat(DeserializerUtils.deserialize(objectMapper, "true", Boolean.class)).isEqualTo(Boolean.TRUE);
    }

    @Test
    public void testDeserialize_ThrowsUncheckedIOException_WhenObjectCannotBeDeserialized() {
        assertThatThrownBy(() ->
                DeserializerUtils.deserialize(objectMapper, "true", Integer.class))
                .isInstanceOf(UncheckedIOException.class);
    }

    private JsonParser buildParser(String json) throws Exception {
        return objectMapper.getFactory().createParser(json);
    }
}
