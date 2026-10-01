package com.cgi.eoss.ogcapi.processes.model.inputs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InputValueNoObjectIntegerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final InputValueNoObjectInteger INPUT_VALUE_NO_OBJECT_INTEGER = new InputValueNoObjectInteger(35);

    private static final String INPUT_VALUE_NO_OBJECT_INTEGER_JSON = "35";

    @Test
    public void testGet_ReturnsIntegerValue() {
        assertThat(INPUT_VALUE_NO_OBJECT_INTEGER.get()).isEqualTo(35);
    }

    @Test
    public void testSerialize_SerializesObjectWithGetKeyAndDoubleValue() throws Exception {
        assertThat(OBJECT_MAPPER.writeValueAsString(INPUT_VALUE_NO_OBJECT_INTEGER)).isEqualTo(INPUT_VALUE_NO_OBJECT_INTEGER_JSON);
    }
}
