package com.cgi.eoss.ogcapi.processes.insula.model.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.hateoas.Link;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class JobLaunchResponseTest {

    private static final ObjectMapper OBJECT_MAPPER;

    private static final JobLaunchResponse JOB_LAUNCH_RESPONSE_FROM_ALL_ATTRS;

    private static final String JOB_LAUNCH_RESPONSE_FROM_ALL_ATTRS_JSON = "{\"id\":10,\"created\":\"2025-01-01T10:15:20Z\"," +
            "\"startTime\":\"2025-01-01T10:15:25\",\"endTime\":\"2025-02-01T10:15:20\",\"lastUpdated\":" +
            "\"2025-02-01T10:15:25Z\",\"phase\":\"PROCESSING\",\"status\":\"RUNNING\"," +
            "\"_links\":{\"self\":{\"href\":\"http://localhost/jobs/10\",\"rel\":\"self\"}}}";

    @Test
    public void testSerialize_SerializesAllJsonKeyValues_WhenAllAttributesAreSet() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(JOB_LAUNCH_RESPONSE_FROM_ALL_ATTRS),
                JOB_LAUNCH_RESPONSE_FROM_ALL_ATTRS_JSON,
                JSONCompareMode.STRICT);
    }

    @Test
    public void testSerialize_SerializesEmptyJson_WhenNoAttributeIsSet() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(JobLaunchResponse.builder().build()),
                "{}",
                JSONCompareMode.STRICT);
    }


    @Test
    public void testDeserialize_DeserializesAllAttributes_WhenAllJsonKeyValuesAreSet() throws Exception {
        assertThat(
                OBJECT_MAPPER.readValue(JOB_LAUNCH_RESPONSE_FROM_ALL_ATTRS_JSON, JobLaunchResponse.class))
                .isEqualTo(JOB_LAUNCH_RESPONSE_FROM_ALL_ATTRS);
    }

    @Test
    public void testDeserialize_IgnoresUnknownJsonKeys() throws Exception {
        JobLaunchResponse jobLaunchResponse = OBJECT_MAPPER.readValue(
                "{\"id\":10, \"created\":\"2025-01-01T10:15:20Z\"," +
                        "\"startTime\":\"2025-01-01T10:15:25\",\"endTime\":\"2025-02-01T10:15:20\",\"lastUpdated\":" +
                        "\"2025-02-01T10:15:25Z\",\"phase\":\"PROCESSING\",\"status\":\"RUNNING\"," +
                        "\"_links\":{\"self\":{\"href\":\"http://localhost/jobs/10\",\"rel\":\"self\"}}," +
                        "\"unknownKey\":\"unknownValue\"}",
                JobLaunchResponse.class
        );
        assertThat(jobLaunchResponse).isEqualTo(JOB_LAUNCH_RESPONSE_FROM_ALL_ATTRS);
    }

    static {
        JOB_LAUNCH_RESPONSE_FROM_ALL_ATTRS = JobLaunchResponse.builder()
                .id(10L)
                .created(OffsetDateTime.of(2025, 1, 1, 10, 15, 20, 0, ZoneOffset.UTC))
                .startTime(LocalDateTime.of(2025, 1, 1, 10, 15, 25, 0))
                .endTime(LocalDateTime.of(2025, 2, 1, 10, 15, 20, 0))
                .lastUpdated(OffsetDateTime.of(2025, 2, 1, 10, 15, 25, 0, ZoneOffset.UTC))
                .phase(JobLaunchResponse.Phase.PROCESSING)
                .status(JobLaunchResponse.Status.RUNNING)
                .links(Map.of("self", Link.of("http://localhost/jobs/10", "self")))
                .build();
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModules(new JavaTimeModule());
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    }

}
