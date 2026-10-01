package com.cgi.eoss.ogcapi.processes.insula.model.request;

import com.cgi.eoss.ogcapi.processes.insula.model.Cwl;
import com.cgi.eoss.ogcapi.processes.insula.model.request.ServiceCreationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.assertj.core.api.Assertions.assertThat;

public class ServiceCreationRequestTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final ServiceCreationRequest SERVICE_CREATION_REQUEST_FROM_ALL_ATTRS = new ServiceCreationRequest(Cwl.builder()
            .url("reference").document("document").build());

    private static final String SERVICE_CREATION_REQUEST_FROM_ALL_ATTRS_JSON = "{\"cwl\": {\"url\": \"reference\", \"document\": \"document\"}}";

    @Test
    public void testSerialize_SerializesAllJsonKeyValues_WhenAllAttributesAreSet() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(SERVICE_CREATION_REQUEST_FROM_ALL_ATTRS),
                SERVICE_CREATION_REQUEST_FROM_ALL_ATTRS_JSON,
                JSONCompareMode.STRICT);
    }

    @Test
    public void testSerialize_SerializesEmptyJson_WhenNoAttributeIsSet() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(ServiceCreationRequest.builder().build()),
                "{}",
                JSONCompareMode.STRICT);
    }


    @Test
    public void testDeserialize_DeserializesAllAttributes_WhenAllJsonKeyValuesAreSet() throws Exception {
        assertThat(
                OBJECT_MAPPER.readValue(SERVICE_CREATION_REQUEST_FROM_ALL_ATTRS_JSON, ServiceCreationRequest.class))
                .isEqualTo(SERVICE_CREATION_REQUEST_FROM_ALL_ATTRS);
    }

    @Test
    public void testDeserialize_IgnoresUnknownJsonKeys() throws Exception {
        ServiceCreationRequest serviceCreationRequest = OBJECT_MAPPER.readValue(
                "{\"cwl\": {\"url\": \"reference\", \"document\": \"document\"}, \"unknownKey\": \"unknownValue\"}",
                ServiceCreationRequest.class
        );
        assertThat(serviceCreationRequest).isEqualTo(SERVICE_CREATION_REQUEST_FROM_ALL_ATTRS);
    }

}
