package com.cgi.eoss.ogcapi.processes.model.inputs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InputValueNoObjectStringTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final InputValueNoObjectString INPUT_VALUE_NO_OBJECT_STRING = new InputValueNoObjectString("string");

    private static final String INPUT_VALUE_NO_OBJECT_STRING_JSON = "\"string\"";

    @Test
    public void testGet_ReturnsStringValue() {
        assertThat(INPUT_VALUE_NO_OBJECT_STRING.get()).isEqualTo("string");
    }


    @Test
    public void testSerialize_SerializesObjectWithGetKeyAndStringValue() throws Exception {
        assertThat(OBJECT_MAPPER.writeValueAsString(INPUT_VALUE_NO_OBJECT_STRING)).isEqualTo(INPUT_VALUE_NO_OBJECT_STRING_JSON);
    }
}
