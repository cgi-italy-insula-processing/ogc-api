package com.cgi.eoss.ogcapi.processes.insula.model.response;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.assertj.core.api.Assertions.assertThat;

public class JobConfigCreationResponseTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final JobConfigCreationResponse JOB_CONFIG_CREATION_RESPONSE_FROM_ALL_ATTRS = JobConfigCreationResponse.builder()
            .id(3030L)
            .build();

    private static final String JOB_CONFIG_CREATION_RESPONSE_FROM_ALL_ATTRS_JSON = "{\"id\":3030}";

    @Test
    public void testSerialize_SerializesAllJsonKeyValues_WhenAllAttributesAreSet() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(JOB_CONFIG_CREATION_RESPONSE_FROM_ALL_ATTRS),
                JOB_CONFIG_CREATION_RESPONSE_FROM_ALL_ATTRS_JSON,
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
                OBJECT_MAPPER.readValue(JOB_CONFIG_CREATION_RESPONSE_FROM_ALL_ATTRS_JSON, JobConfigCreationResponse.class))
                .isEqualTo(JOB_CONFIG_CREATION_RESPONSE_FROM_ALL_ATTRS);
    }

    @Test
    public void testDeserialize_IgnoresUnknownJsonKeys() throws Exception {
        JobConfigCreationResponse jobConfigCreationResponse = OBJECT_MAPPER.readValue(
                "{\"id\":3030,\"unknownKey\":\"unknownValue\"}",
                JobConfigCreationResponse.class
        );
        assertThat(jobConfigCreationResponse).isEqualTo(JOB_CONFIG_CREATION_RESPONSE_FROM_ALL_ATTRS);
    }
}
