package com.cgi.eoss.ogcapi.processes.insula.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.assertj.core.api.Assertions.assertThat;

public class CwlTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Cwl CWL_FROM_ALL_ATTRS = Cwl.builder().url("reference").document("document").build();

    private static final String CWL_FROM_ALL_ATTRS_JSON = "{\"url\": \"reference\", \"document\": \"document\"}";

    @Test
    public void testSerialize_SerializesAllJsonKeyValues_WhenAllAttributesAreSet() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(CWL_FROM_ALL_ATTRS),
                CWL_FROM_ALL_ATTRS_JSON,
                JSONCompareMode.STRICT);
    }

    @Test
    public void testSerialize_SerializesAllJsonKeyValues_WhenDocumentAttributeIsNotSet() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(Cwl.builder().url("reference").build()),
                "{\"url\": \"reference\"}",
                JSONCompareMode.STRICT);
    }

    @Test
    public void testSerialize_SerializesEmptyJson_WhenNoAttributeIsSet() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(Cwl.builder().build()),
                "{}",
                JSONCompareMode.STRICT);
    }


    @Test
    public void testDeserialize_DeserializesAllAttributes_WhenAllJsonKeyValuesAreSet() throws Exception {
        assertThat(
                OBJECT_MAPPER.readValue(CWL_FROM_ALL_ATTRS_JSON, Cwl.class))
                .isEqualTo(CWL_FROM_ALL_ATTRS);
    }

    @Test
    public void testDeserialize_DeserializesAllAttributes_WhenDocumentAttributeIsNotSet() throws Exception {
        assertThat(
                OBJECT_MAPPER.readValue("{\"url\": \"reference\"}", Cwl.class))
                .isEqualTo(Cwl.builder().url("reference").build());
    }

    @Test
    public void testDeserialize_IgnoresUnknownJsonKeys() throws Exception {
        Cwl cwl = OBJECT_MAPPER.readValue(
                "{\"url\": \"reference\", \"document\": \"document\", \"unknownKey\": \"unknownValue\"}",
                Cwl.class
        );
        assertThat(cwl).isEqualTo(CWL_FROM_ALL_ATTRS);
    }
}
