package com.cgi.eoss.ogcapi.processes.insula.model.response;

import com.cgi.eoss.ogcapi.processes.insula.model.Cwl;
import com.cgi.eoss.ogcapi.processes.insula.model.ServiceDescriptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.hateoas.Link;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ServiceResponseTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final ServiceResponse SERVICE_CREATION_RESPONSE_FROM_ALL_ATTRS = ServiceResponse.builder()
            .id(1L).name("name").description("description")
            .cwl(Cwl.builder().url("reference").document("document").build())
            .serviceDescriptor(ServiceDescriptor.builder().title("title").version("version").build())
            .links(Map.of("self", Link.of("http://localhost/services/10", "self")))
            .build();

    private static final String SERVICE_CREATION_RESPONSE_FROM_ALL_ATTRS_JSON = "{\"id\": 1, \"name\": \"name\", \"description\": \"description\"," +
            "\"cwl\": {\"url\": \"reference\", \"document\": \"document\"}, " +
            "\"serviceDescriptor\": {\"title\": \"title\", \"version\": \"version\"}," +
            "\"_links\":{\"self\":{\"href\":\"http://localhost/services/10\",\"rel\":\"self\"}}}";

    @Test
    public void testSerialize_SerializesAllJsonKeyValues_WhenAllAttributesAreSet() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(SERVICE_CREATION_RESPONSE_FROM_ALL_ATTRS),
                SERVICE_CREATION_RESPONSE_FROM_ALL_ATTRS_JSON,
                JSONCompareMode.STRICT);
    }

    @Test
    public void testSerialize_SerializesEmptyJson_WhenNoAttributeIsSet() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(ServiceResponse.builder().build()),
                "{}",
                JSONCompareMode.STRICT);
    }

    @Test
    public void testDeserialize_DeserializesAllAttributes_WhenAllJsonKeyValuesAreSet() throws Exception {
        assertThat(
                OBJECT_MAPPER.readValue(SERVICE_CREATION_RESPONSE_FROM_ALL_ATTRS_JSON, ServiceResponse.class))
                .isEqualTo(SERVICE_CREATION_RESPONSE_FROM_ALL_ATTRS);
    }

    @Test
    public void testDeserialize_IgnoresUnknownJsonKeys() throws Exception {
        ServiceResponse serviceResponse = OBJECT_MAPPER.readValue(
                "{\"id\": \"1\", \"name\": \"name\", \"description\": \"description\"," +
                        "\"cwl\": {\"url\": \"reference\", \"document\": \"document\"}, " +
                        "\"serviceDescriptor\": {\"title\": \"title\", \"version\": \"version\"}, " +
                        "\"_links\":{\"self\":{\"href\":\"http://localhost/services/10\",\"rel\":\"self\"}}, " +
                        "\"unknownKey\": \"unknownValue\"}",
                ServiceResponse.class
        );
        assertThat(serviceResponse).isEqualTo(SERVICE_CREATION_RESPONSE_FROM_ALL_ATTRS);
    }
}
