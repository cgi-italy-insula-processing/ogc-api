package com.cgi.eoss.ogcapi.processes.serializers;

import com.cgi.eoss.ogcapi.processes.model.Bbox;
import com.cgi.eoss.ogcapi.processes.model.Link;
import com.cgi.eoss.ogcapi.processes.model.inputs.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class InputDeserializerTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final InputDeserializer inputDeserializer = new InputDeserializer();

    @Test
    public void testDeserialize_DeserializesInputAsInputValueNoObjectInteger_WhenJsonRepresentsInteger() throws Exception {
        InputValueNoObjectInteger inputValueNoObjectInteger = (InputValueNoObjectInteger) inputDeserializer
                .deserialize(buildParser("10"), objectMapper.getDeserializationContext());
        assertThat(inputValueNoObjectInteger).isEqualTo(new InputValueNoObjectInteger(10));
    }

    @Test
    public void testDeserialize_DeserializesInputAsInputValueNoObjectDouble_WhenJsonRepresentsDouble() throws Exception {
        InputValueNoObjectDouble inputValueNoObjectDouble = (InputValueNoObjectDouble) inputDeserializer
                .deserialize(buildParser("1.0"), objectMapper.getDeserializationContext());
        assertThat(inputValueNoObjectDouble).isEqualTo(new InputValueNoObjectDouble(1.0));
    }

    @Test
    public void testDeserialize_DeserializesInputAsInputValueNoObjectBoolean_WhenJsonRepresentsBoolean() throws Exception {
        InputValueNoObjectBoolean inputValueNoObjectBoolean = (InputValueNoObjectBoolean) inputDeserializer
                .deserialize(buildParser("true"), objectMapper.getDeserializationContext());
        assertThat(inputValueNoObjectBoolean).isEqualTo(new InputValueNoObjectBoolean(true));
    }

    @Test
    public void testDeserialize_DeserializesInputAsInputValueNoObjectString_WhenJsonRepresentsString() throws Exception {
        InputValueNoObjectString inputValueNoObjectString = (InputValueNoObjectString) inputDeserializer
                .deserialize(buildParser("\"string\""), objectMapper.getDeserializationContext());
        assertThat(inputValueNoObjectString).isEqualTo(new InputValueNoObjectString("string"));
    }

    @Test
    public void testDeserialize_DeserializesInputAsInputValueNoObjectBbox_WhenJsonRepresentsBbox() throws Exception {
        InputValueNoObjectBbox inputValueNoObjectBbox = (InputValueNoObjectBbox) inputDeserializer
                .deserialize(buildParser("{\"bbox\":[10],\"crs\":\"http://www.opengis.net/def/crs/OGC/1.3/CRS84\"}"),
                        objectMapper.getDeserializationContext());
        assertThat(inputValueNoObjectBbox).isEqualTo(new InputValueNoObjectBbox(new Bbox(List.of(new BigDecimal(10)))));
    }

    @Test
    public void testDeserialize_DeserializesInputAsInputValueNoObjectBbox_WhenJsonRepresentsLink() throws Exception {
        Link link = (Link) inputDeserializer
                .deserialize(buildParser("{\"href\":\"hrefValue\"}"), objectMapper.getDeserializationContext());
        assertThat(link).isEqualTo(new Link("hrefValue"));
    }

    @Test
    public void testDeserialize_DeserializesInputAsInputValueNoObjectArray_WhenJsonRepresentsArray() throws Exception {
        InputValueNoObjectArray inputValueNoObjectArray = (InputValueNoObjectArray) inputDeserializer
                .deserialize(buildParser("[10,20,30]"), objectMapper.getDeserializationContext());
        assertThat(inputValueNoObjectArray).isEqualTo(new InputValueNoObjectArray<>(List.of(new InputValueNoObjectInteger(10),
                new InputValueNoObjectInteger(20), new InputValueNoObjectInteger(30))));
    }

    @Test
    public void testDeserialize_ThrowsIllegalArgumentException_WhenJsonRepresentsArrayWithHeterogeneousItemTypes() {
        assertThatThrownBy(() -> inputDeserializer
                .deserialize(buildParser("[10,\"string\",1.5]"), objectMapper.getDeserializationContext()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Input array is not homogeneous");
    }

    @Test
    public void testDeserialize_ThrowsIllegalArgumentException_WhenJsonRepresentsUnsupportedNumericType() {
        assertThatThrownBy(() -> inputDeserializer
                .deserialize(buildParser("982134839798321390034432432"), objectMapper.getDeserializationContext()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported numeric input type: BIG_INTEGER");
    }

    @Test
    public void testDeserialize_ThrowsIllegalArgumentException_WhenJsonRepresentsUnsupportedInputObjectType() {
        String unknownObjectJson = "{\"unknownKey\":\"unknownValue\"}";
        assertThatThrownBy(() -> inputDeserializer
                .deserialize(buildParser(unknownObjectJson), objectMapper.getDeserializationContext()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to deserialize input object: " + unknownObjectJson);
    }

    private JsonParser buildParser(String json) throws Exception {
        return objectMapper.getFactory().createParser(json);
    }
}
