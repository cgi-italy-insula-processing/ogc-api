package com.cgi.eoss.ogcapi.processes.serializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JobConfigInputsDeserializerTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    private JobConfigInputsDeserializer jobConfigInputsDeserializer;

    @BeforeEach
    public void init() {
        objectMapper = new ObjectMapper();
        jobConfigInputsDeserializer = new JobConfigInputsDeserializer();
    }

    @Test
    public void testDeserialize_DeserializesJobConfigInputsAsMap_WhenJsonRepresentsJobConfigInputsMap() throws Exception {
        Map<String, Object> actualJobConfigInputs = jobConfigInputsDeserializer
                .deserialize(buildParser(
                        "{\"collection\":[\"{\\\"out\\\":\\\"eopaas://outputProduct/filename\\\"}\"]," +
                                "\"in\":[\"eopaas://inputProduct/filename\"]}"
                        ),
                        objectMapper.getDeserializationContext()
                );
        Map<String, Object> expectedJobConfigInputs = Map.of(
                "collection", List.of(Map.of("out", "eopaas://outputProduct/filename")),
                "in", List.of("eopaas://inputProduct/filename"));
        assertThat(actualJobConfigInputs).isEqualTo(expectedJobConfigInputs);
    }

    @Test
    public void testDeserialize_ThrowsIllegalArgumentException_WhenJsonDoesNotRepresentJobConfigInputsMap() {
        assertThatThrownBy(() -> jobConfigInputsDeserializer
                .deserialize(buildParser("10"), objectMapper.getDeserializationContext()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to deserialize");
    }

    private JsonParser buildParser(String json) throws Exception {
        return objectMapper.getFactory().createParser(json);
    }
}
