package com.cgi.eoss.ogcapi.processes.insula.model.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class JobConfigCreationRequestTest {

    private static final ObjectMapper OBJECT_MAPPER;

    private static final JobConfigCreationRequest JOB_CONFIG_CREATION_REQUEST_FROM_ALL_ATTRS;

    private static final String JOB_CONFIG_CREATION_REQUEST_FROM_ALL_ATTRS_JSON =
            "{\"service\":\"https://uri.com/jobConfigs/1\",\"inputs\":{\"in\":[\"inValue\"]}}";

    @Test
    public void testSerialize_SerializesAllJsonKeyValues_WhenAllAttributesAreSet() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(JOB_CONFIG_CREATION_REQUEST_FROM_ALL_ATTRS),
                JOB_CONFIG_CREATION_REQUEST_FROM_ALL_ATTRS_JSON,
                JSONCompareMode.STRICT);
    }

    @Test
    public void testSerialize_SerializesEmptyJson_WhenNoAttributeIsSet() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(JobConfigCreationRequest.builder().build()),
                "{}",
                JSONCompareMode.STRICT);
    }


    @Test
    public void testDeserialize_DeserializesAllAttributes_WhenAllJsonKeyValuesAreSet() throws Exception {
        assertThat(
                OBJECT_MAPPER.readValue(JOB_CONFIG_CREATION_REQUEST_FROM_ALL_ATTRS_JSON, JobConfigCreationRequest.class))
                .isEqualTo(JOB_CONFIG_CREATION_REQUEST_FROM_ALL_ATTRS);
    }

    @Test
    public void testDeserialize_IgnoresUnknownJsonKeys() throws Exception {
        JobConfigCreationRequest jobConfigCreationRequest = OBJECT_MAPPER.readValue(
                "{\"service\":\"https://uri.com/jobConfigs/1\",\"inputs\":{\"in\":[\"inValue\"]},\"unknownKey\":\"unknownValue\"}",
                JobConfigCreationRequest.class
        );
        assertThat(jobConfigCreationRequest).isEqualTo(JOB_CONFIG_CREATION_REQUEST_FROM_ALL_ATTRS);
    }

    static {
        Multimap<String, Object> inputs = ArrayListMultimap.create();
        inputs.put("in", "inValue");
        JOB_CONFIG_CREATION_REQUEST_FROM_ALL_ATTRS = JobConfigCreationRequest.builder()
                .service(URI.create("https://uri.com/jobConfigs/1"))
                .inputs(inputs)
                .build();
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModules(new GuavaModule());
    }

}
