package com.cgi.eoss.ogcapi.processes.insula.model.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class JobFindResponseTest {

    private static final ObjectMapper OBJECT_MAPPER;

    private static final JobFindResponse JOB_FIND_RESPONSE_FROM_ALL_ATTRS;

    private static final String JOB_FIND_RESPONSE_FROM_ALL_ATTRS_JSON = "{\"_embedded\":{\"jobs\":[" +
            "{\"id\":10,\"created\":\"2025-01-01T10:15:20Z\"," +
            "\"startTime\":\"2025-01-01T10:15:25\",\"endTime\":\"2025-02-01T10:15:20\",\"lastUpdated\":" +
            "\"2025-02-01T10:15:25Z\",\"phase\":\"PROCESSING\",\"status\":\"RUNNING\",\"serviceName\":\"serviceName\"}]}}";

    @Test
    public void testSerialize_SerializesAllJsonKeyValues_WhenAllAttributesAreSet() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(JOB_FIND_RESPONSE_FROM_ALL_ATTRS),
                JOB_FIND_RESPONSE_FROM_ALL_ATTRS_JSON,
                JSONCompareMode.STRICT);
    }

    @Test
    public void testSerialize_SerializesEmptyJson_WhenNoAttributeIsSet() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(JobFindResponse.builder().build()),
                "{}",
                JSONCompareMode.STRICT);
    }


    @Test
    public void testDeserialize_DeserializesAllAttributes_WhenAllJsonKeyValuesAreSet() throws Exception {
        assertThat(
                OBJECT_MAPPER.readValue(JOB_FIND_RESPONSE_FROM_ALL_ATTRS_JSON, JobFindResponse.class))
                .isEqualTo(JOB_FIND_RESPONSE_FROM_ALL_ATTRS);
    }

    @Test
    public void testDeserialize_IgnoresUnknownJsonKeys() throws Exception {
        JobFindResponse jobFindResponse = OBJECT_MAPPER.readValue(
                "{\"_embedded\":{\"jobs\":[{\"id\":10, \"created\":\"2025-01-01T10:15:20Z\"," +
                        "\"startTime\":\"2025-01-01T10:15:25\",\"endTime\":\"2025-02-01T10:15:20\",\"lastUpdated\":" +
                        "\"2025-02-01T10:15:25Z\",\"phase\":\"PROCESSING\",\"status\":\"RUNNING\",\"serviceName\":\"serviceName\"" +
                        "}]}, \"unknownKey\":\"unknownValue\"}",
                JobFindResponse.class
        );
        assertThat(jobFindResponse).isEqualTo(JOB_FIND_RESPONSE_FROM_ALL_ATTRS);
    }

    static {
        JOB_FIND_RESPONSE_FROM_ALL_ATTRS = JobFindResponse.builder()
                        .embeddedJobs(JobFindResponse.EmbeddedJobs.builder()
                                .jobs(List.of(JobGetResponse.builder()
                                        .id(10L)
                                        .created(OffsetDateTime.of(2025, 1, 1, 10, 15, 20, 0, ZoneOffset.UTC))
                                        .startTime(LocalDateTime.of(2025, 1, 1, 10, 15, 25, 0))
                                        .endTime(LocalDateTime.of(2025, 2, 1, 10, 15, 20, 0))
                                        .lastUpdated(OffsetDateTime.of(2025, 2, 1, 10, 15, 25, 0, ZoneOffset.UTC))
                                        .phase(JobLaunchResponse.Phase.PROCESSING)
                                        .status(JobLaunchResponse.Status.RUNNING)
                                        .serviceName("serviceName")
                                        .build()))
                                .build())
                .build();
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModules(new JavaTimeModule());
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
