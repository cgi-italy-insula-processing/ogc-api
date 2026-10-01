package com.cgi.eoss.ogcapi.processes.model.inputs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InputValueNoObjectDoubleTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final InputValueNoObjectDouble INPUT_VALUE_NO_OBJECT_DOUBLE = new InputValueNoObjectDouble(1.5);

    private static final String INPUT_VALUE_NO_OBJECT_DOUBLE_JSON = "1.5";

    @Test
    public void testGet_ReturnsDoubleValue() {
        assertThat(INPUT_VALUE_NO_OBJECT_DOUBLE.get()).isEqualTo(1.5);
    }

    @Test
    public void testSerialize_SerializesObjectWithGetKeyAndDoubleValue() throws Exception {
        assertThat(OBJECT_MAPPER.writeValueAsString(INPUT_VALUE_NO_OBJECT_DOUBLE)).isEqualTo(INPUT_VALUE_NO_OBJECT_DOUBLE_JSON);
    }
}
