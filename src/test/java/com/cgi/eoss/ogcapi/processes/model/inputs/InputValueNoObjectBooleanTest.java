package com.cgi.eoss.ogcapi.processes.model.inputs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InputValueNoObjectBooleanTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final InputValueNoObjectBoolean INPUT_VALUE_NO_OBJECT_BOOLEAN = new InputValueNoObjectBoolean(true);

    private static final String INPUT_VALUE_NO_OBJECT_BOOLEAN_JSON = "true";

    @Test
    public void testGet_ReturnsBooleanValue() {
        assertThat(INPUT_VALUE_NO_OBJECT_BOOLEAN.get()).isEqualTo(true);
    }

    @Test
    public void testSerialize_SerializesObjectWithGetKeyAndBooleanValue() throws Exception {
        assertThat(OBJECT_MAPPER.writeValueAsString(INPUT_VALUE_NO_OBJECT_BOOLEAN)).isEqualTo(INPUT_VALUE_NO_OBJECT_BOOLEAN_JSON);
    }
}
