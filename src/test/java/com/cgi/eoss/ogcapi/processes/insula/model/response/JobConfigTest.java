package com.cgi.eoss.ogcapi.processes.insula.model.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class JobConfigTest {

    private static final ObjectMapper OBJECT_MAPPER;

    private static final JobGetResponse.JobConfig JOB_CONFIG_FROM_ALL_ATTRS_DESERIALIZED;

    private static final JobGetResponse.JobConfig JOB_CONFIG_FROM_ALL_ATTRS_SERIALIZED;

    private static final String JOB_CONFIG_FROM_ALL_ATTRS_JSON =
            "{\"inputs\":{" +
            "\"in\":[\"eopaas://outputProduct/filename\"]," +
            "\"collection\":[\"{\\\"out\\\":\\\"eopaas://outputProduct/a2389zji\\\"}\"]" +
            "}}";

    @Test
    public void testSerialize_SerializesAllJsonKeyValues_WhenAllAttributesAreSet() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(JOB_CONFIG_FROM_ALL_ATTRS_SERIALIZED),
                JOB_CONFIG_FROM_ALL_ATTRS_JSON,
                JSONCompareMode.STRICT);
    }

    @Test
    public void testSerialize_SerializesEmptyJson_WhenNoAttributeIsSet() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(JobGetResponse.builder().build()),
                "{}",
                JSONCompareMode.STRICT);
    }


    @Test
    public void testDeserialize_DeserializesAllAttributes_WhenAllJsonKeyValuesAreSet() throws Exception {
        assertThat(
                OBJECT_MAPPER.readValue(JOB_CONFIG_FROM_ALL_ATTRS_JSON, JobGetResponse.JobConfig.class))
                .isEqualTo(JOB_CONFIG_FROM_ALL_ATTRS_DESERIALIZED);
    }

    @Test
    public void testDeserialize_IgnoresUnknownJsonKeys() throws Exception {
        JobGetResponse.JobConfig jobConfig = OBJECT_MAPPER.readValue(
                "{\"unknownKey\":\"unknownValue\"," +
                        "\"inputs\":{" +
                        "\"in\":[\"eopaas://outputProduct/filename\"]," +
                        "\"collection\":[\"{\\\"out\\\":\\\"eopaas://outputProduct/a2389zji\\\"}\"]" +
                        "}}",
                JobGetResponse.JobConfig.class
        );
        assertThat(jobConfig).isEqualTo(JOB_CONFIG_FROM_ALL_ATTRS_DESERIALIZED);
    }

    static {
        JOB_CONFIG_FROM_ALL_ATTRS_DESERIALIZED = JobGetResponse.JobConfig.builder()
                        .inputs(Map.of("in",
                                List.of("eopaas://outputProduct/filename"),
                                "collection",
                                List.of(Map.of("out", "eopaas://outputProduct/a2389zji"))))
                        .build();
        JOB_CONFIG_FROM_ALL_ATTRS_SERIALIZED = JobGetResponse.JobConfig.builder()
                .inputs(Map.of("in",
                        List.of("eopaas://outputProduct/filename"),
                        "collection",
                        List.of("{\"out\":\"eopaas://outputProduct/a2389zji\"}")))
                .build();
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModules(new JavaTimeModule());
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
