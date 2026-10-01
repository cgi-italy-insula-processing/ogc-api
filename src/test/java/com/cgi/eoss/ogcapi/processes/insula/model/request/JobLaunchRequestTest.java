package com.cgi.eoss.ogcapi.processes.insula.model.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.assertj.core.api.Assertions.assertThat;

public class JobLaunchRequestTest {

    private static final ObjectMapper OBJECT_MAPPER;

    private static final JobLaunchRequest JOB_LAUNCH_REQUEST_FROM_ALL_ATTRS;

    private static final String JOB_LAUNCH_REQUEST_FROM_ALL_ATTRS_JSON = "{\"jobConfigId\":3030}";

    @Test
    public void testSerialize_SerializesAllJsonKeyValues_WhenAllAttributesAreSet() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(JOB_LAUNCH_REQUEST_FROM_ALL_ATTRS),
                JOB_LAUNCH_REQUEST_FROM_ALL_ATTRS_JSON,
                JSONCompareMode.STRICT);
    }

    @Test
    public void testSerialize_SerializesEmptyJson_WhenNoAttributeIsSet() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(JobLaunchRequest.builder().build()),
                "{}",
                JSONCompareMode.STRICT);
    }


    @Test
    public void testDeserialize_DeserializesAllAttributes_WhenAllJsonKeyValuesAreSet() throws Exception {
        assertThat(
                OBJECT_MAPPER.readValue(JOB_LAUNCH_REQUEST_FROM_ALL_ATTRS_JSON, JobLaunchRequest.class))
                .isEqualTo(JOB_LAUNCH_REQUEST_FROM_ALL_ATTRS);
    }

    @Test
    public void testDeserialize_IgnoresUnknownJsonKeys() throws Exception {
        JobLaunchRequest jobConfigCreationRequest = OBJECT_MAPPER.readValue(
                "{\"jobConfigId\":\"3030\",\"unknownKey\":\"unknownValue\"}",
                JobLaunchRequest.class
        );
        assertThat(jobConfigCreationRequest).isEqualTo(JOB_LAUNCH_REQUEST_FROM_ALL_ATTRS);
    }

    static {
        JOB_LAUNCH_REQUEST_FROM_ALL_ATTRS = JobLaunchRequest.builder()
                .jobConfigId(3030L)
                .build();
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModules(new GuavaModule());
    }

}
