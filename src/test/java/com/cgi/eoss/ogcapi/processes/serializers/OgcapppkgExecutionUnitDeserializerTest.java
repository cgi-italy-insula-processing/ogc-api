package com.cgi.eoss.ogcapi.processes.serializers;

import com.cgi.eoss.ogcapi.processes.model.Link;
import com.cgi.eoss.ogcapi.processes.model.OgcapppkgExecutionUnit;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OgcapppkgExecutionUnitDeserializerTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    private OgcapppkgExecutionUnitDeserializer ogcapppkgExecutionUnitDeserializer;

    @BeforeEach
    public void init() {
        objectMapper = new ObjectMapper();
        ogcapppkgExecutionUnitDeserializer = new OgcapppkgExecutionUnitDeserializer();
    }

    @Test
    public void testDeserialize_DeserializesOgcapppkgExecutionUnitAsLink_WhenJsonRepresentsLink() throws Exception {
        String expectedHref = "https://reference.ogc/cwl";
        OgcapppkgExecutionUnit actualExecutionUnit = ogcapppkgExecutionUnitDeserializer
                .deserialize(buildParser("{\"href\": \""+expectedHref+"\"}"), objectMapper.getDeserializationContext());
        OgcapppkgExecutionUnit expectedExecutionUnit = new Link(expectedHref);
        assertThat(actualExecutionUnit).isEqualTo(expectedExecutionUnit);
    }

    @Test
    public void testDeserialize_ThrowsIllegalArgumentException_WhenJsonDoesNotRepresentLink() {
        assertThatThrownBy(() ->ogcapppkgExecutionUnitDeserializer
                .deserialize(buildParser("{\"unknownKey\": \"unknownValue\"}"), objectMapper.getDeserializationContext()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to deserialize");
    }

    private JsonParser buildParser(String json) throws Exception {
        return objectMapper.getFactory().createParser(json);
    }
}
