package com.cgi.eoss.ogcapi.processes.insula.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.hateoas.Link;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class OutputFileTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final OutputFile OUTPUT_FILE_FROM_ALL_ATTRS = OutputFile.builder()
            .filename("filename.tiff")
            .links(Map.of("out", Link.of("http://localhost/self", "self") ))
            .build();

    private static final String OUTPUT_FILE_FROM_ALL_ATTRS_JSON = "{\"filename\": \"filename.tiff\", " +
            "\"_links\": {\"out\": {\"href\": \"http://localhost/self\", \"rel\": \"self\"}}}";

    @Test
    public void testSerialize_SerializesAllJsonKeyValues_WhenAllAttributesAreSet() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(OUTPUT_FILE_FROM_ALL_ATTRS),
                OUTPUT_FILE_FROM_ALL_ATTRS_JSON,
                JSONCompareMode.STRICT);
    }

    @Test
    public void testSerialize_SerializesEmptyJson_WhenNoAttributeIsSet() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(OutputFile.builder().build()),
                "{}",
                JSONCompareMode.STRICT);
    }


    @Test
    public void testDeserialize_DeserializesAllAttributes_WhenAllJsonKeyValuesAreSet() throws Exception {
        assertThat(
                OBJECT_MAPPER.readValue(OUTPUT_FILE_FROM_ALL_ATTRS_JSON, OutputFile.class))
                .isEqualTo(OUTPUT_FILE_FROM_ALL_ATTRS);
    }

    @Test
    public void testDeserialize_IgnoresUnknownJsonKeys() throws Exception {
        OutputFile outputFile = OBJECT_MAPPER.readValue(
                "{\"filename\": \"filename.tiff\", " +
                        "\"_links\": {\"out\": {\"href\": \"http://localhost/self\", \"rel\": \"self\"}}, \"unknownKey\": \"unknownValue\"}",
                OutputFile.class
        );
        assertThat(outputFile).isEqualTo(OUTPUT_FILE_FROM_ALL_ATTRS);
    }

}
