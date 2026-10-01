package com.cgi.eoss.ogcapi.processes.insula.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ServiceDescriptorTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final ServiceDescriptor SERVICE_DESCRIPTOR_FROM_ALL_ATTRS = ServiceDescriptor.builder()
            .title("title").version("version")
            .dataInputs(List.of(ServiceDescriptor.InputOutputParameter.builder()
                            .id("inputId")
                            .description("description")
                            .defaultAttrs(ImmutableMap.of("dataType", "string"))
                            .platformMetadata(ImmutableMap.of("preventUrlDownload", "false", "format", "CATALOGUE", "type", "STAC"))
                            .title("inputTitle")
                            .minOccurs(0)
                            .maxOccurs(100)
                    .build()))
            .dataOutputs(List.of(ServiceDescriptor.InputOutputParameter.builder()
                            .id("outputId")
                            .description("description")
                            .defaultAttrs(ImmutableMap.of("dataType", "string"))
                            .platformMetadata(ImmutableMap.of("format", "URL"))
                            .title("outputTitle")
                            .minOccurs(1)
                            .maxOccurs(2)
                    .build()))
            .build();

    private static final String SERVICE_DESCRIPTOR_FROM_ALL_ATTRS_JSON = "{\"title\": \"title\", " +
            "\"version\": \"version\", " +
            "\"dataInputs\": [{\"id\": \"inputId\", \"description\": \"description\", \"title\": \"inputTitle\", \"minOccurs\": 0, \"maxOccurs\": 100, " +
            "\"defaultAttrs\": {\"dataType\": \"string\"}, \"platformMetadata\": {\"preventUrlDownload\": \"false\", \"format\": \"CATALOGUE\", \"type\": \"STAC\"}}], " +
            "\"dataOutputs\": [{\"id\": \"outputId\", \"description\": \"description\", \"title\": \"outputTitle\", \"minOccurs\": 1, \"maxOccurs\": 2, " +
            "\"defaultAttrs\": {\"dataType\": \"string\"}, \"platformMetadata\": {\"format\": \"URL\"}}]}";

    @Test
    public void testSerialize_SerializesAllJsonKeyValues_WhenAllAttributesAreSet() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(SERVICE_DESCRIPTOR_FROM_ALL_ATTRS),
                SERVICE_DESCRIPTOR_FROM_ALL_ATTRS_JSON,
                JSONCompareMode.STRICT);
    }

    @Test
    public void testSerialize_SerializesEmptyJson_WhenNoAttributeIsSet() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(ServiceDescriptor.builder().build()),
                "{}",
                JSONCompareMode.STRICT);
    }

    @Test
    public void testDeserialize_DeserializesAllAttributes_WhenAllJsonKeyValuesAreSet() throws Exception {
        assertThat(
                OBJECT_MAPPER.readValue(SERVICE_DESCRIPTOR_FROM_ALL_ATTRS_JSON, ServiceDescriptor.class))
                .isEqualTo(SERVICE_DESCRIPTOR_FROM_ALL_ATTRS);
    }

    @Test
    public void testDeserialize_IgnoresUnknownJsonKeys() throws Exception {
        ServiceDescriptor serviceDescriptor = OBJECT_MAPPER.readValue(
                "{\"title\": \"title\", " +
                        "\"version\": \"version\", " +
                        "\"dataInputs\": [{\"id\": \"inputId\", \"description\": \"description\", \"title\": \"inputTitle\", \"minOccurs\": 0, \"maxOccurs\": 100, " +
                        "\"defaultAttrs\": {\"dataType\": \"string\"}, \"platformMetadata\": {\"preventUrlDownload\": \"false\", \"format\": \"CATALOGUE\", \"type\": \"STAC\"}}], " +
                        "\"dataOutputs\": [{\"id\": \"outputId\", \"description\": \"description\", \"title\": \"outputTitle\", \"minOccurs\": 1, \"maxOccurs\": 2, " +
                        "\"defaultAttrs\": {\"dataType\": \"string\"}, \"platformMetadata\": {\"format\": \"URL\"}}], " +
                        "\"unknownKey\":\"unknownValue\"}",
                ServiceDescriptor.class
        );
        assertThat(serviceDescriptor).isEqualTo(SERVICE_DESCRIPTOR_FROM_ALL_ATTRS);
    }

}
