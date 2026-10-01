package com.cgi.eoss.ogcapi.processes.insula.model.response;

import com.cgi.eoss.ogcapi.processes.insula.model.OutputFile;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JobGetResponseTest {

    private static final ObjectMapper OBJECT_MAPPER;

    private static final JobGetResponse JOB_GET_RESPONSE_FROM_ALL_ATTRS;

    private static final String JOB_GET_RESPONSE_FROM_ALL_ATTRS_JSON = "{" +
            "\"id\":10," +
            "\"created\":\"2025-01-01T10:15:20Z\"," +
            "\"startTime\":\"2025-01-01T10:15:25\"," +
            "\"endTime\":\"2025-02-01T10:15:20\"," +
            "\"lastUpdated\":\"2025-02-01T10:15:25Z\"," +
            "\"phase\":\"PROCESSING\"," +
            "\"status\":\"RUNNING\"," +
            "\"serviceName\":\"serviceName\"," +
            "\"outputs\":{\"out\":[\"eopaas://outputProduct/filename\"]}," +
            "\"outputFiles\":[{\"filename\":\"filename\",\"_links\":{\"download\":{\"href\":\"http://localhost/download\",\"rel\":\"self\"}}}]," +
            "\"config\":{\"inputs\":{\"in\":[\"eopaas://outputProduct/filename\"]}}," +
            "\"extId\":\"c7d6fedd-5e2d-4a31-b359-95c69e16ca92\"," +
            "\"subJobStatusCounts\":{\"RUNNING\":1, \"COMPLETED\":2}" +
            "}";

    @Test
    public void testSerialize_SerializesAllJsonKeyValues_WhenAllAttributesAreSet() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(JOB_GET_RESPONSE_FROM_ALL_ATTRS),
                JOB_GET_RESPONSE_FROM_ALL_ATTRS_JSON,
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
                OBJECT_MAPPER.readValue(JOB_GET_RESPONSE_FROM_ALL_ATTRS_JSON, JobGetResponse.class))
                .isEqualTo(JOB_GET_RESPONSE_FROM_ALL_ATTRS);
    }

    @Test
    public void testDeserialize_IgnoresUnknownJsonKeys() throws Exception {
        JobGetResponse jobGetResponse = OBJECT_MAPPER.readValue(
                "{\"unknownKey\":\"unknownValue\"," +
                        "\"id\":10," +
                        "\"created\":\"2025-01-01T10:15:20Z\"," +
                        "\"startTime\":\"2025-01-01T10:15:25\"," +
                        "\"endTime\":\"2025-02-01T10:15:20\"," +
                        "\"lastUpdated\":\"2025-02-01T10:15:25Z\"," +
                        "\"phase\":\"PROCESSING\"," +
                        "\"status\":\"RUNNING\"," +
                        "\"serviceName\":\"serviceName\"," +
                        "\"outputs\":{\"out\":[\"eopaas://outputProduct/filename\"]}," +
                        "\"outputFiles\":[{\"filename\":\"filename\",\"_links\":{\"download\":{\"href\":\"http://localhost/download\",\"rel\":\"self\"}}}]," +
                        "\"config\":{\"inputs\":{\"in\":[\"eopaas://outputProduct/filename\"]}}," +
                        "\"extId\":\"c7d6fedd-5e2d-4a31-b359-95c69e16ca92\"," +
                        "\"subJobStatusCounts\":{\"RUNNING\":1, \"COMPLETED\":2}" +
                        "}",
                JobGetResponse.class
        );
        assertThat(jobGetResponse).isEqualTo(JOB_GET_RESPONSE_FROM_ALL_ATTRS);
    }

    @Test
    public void testGetOutputFilenames_ReturnsOutputFilesIdentifierList() {
        String outputId = "outputId";
        String extId = "extId";

        JobGetResponse jobGetResponse = JobGetResponse.builder()
                .outputs(Map.of(outputId, List.of("tenant://catalogue//jobId//outputFileNameOne", "tenant://catalogue//jobId//outputFileNameTwo")))
                .extId(extId)
                .build();

        assertThat(jobGetResponse.getOutputFilesIdentifier(outputId)).isEqualTo(extId + "_" + outputId);
    }

    @Test
    public void testGetCollectionId_ReturnsCollectionId() {
        String outputId = "outputId";

        JobGetResponse jobGetResponse = JobGetResponse.builder()
                .config(JobGetResponse.JobConfig.builder().inputs(
                                Map.of("collection",
                                        List.of(Map.of("outputId", "collectionId"))))
                        .build())
                .build();

        assertThat(jobGetResponse.getCollectionId(outputId)).isEqualTo("collectionId");
    }

    @Test
    public void testGetCollectionId_ReturnsNull_WhenNoMatchingCollectionIdIsFound() {
        String outputId = "outputId";

        JobGetResponse jobGetResponse = JobGetResponse.builder()
                .config(JobGetResponse.JobConfig.builder().inputs(
                                Map.of("collection",
                                        List.of(Map.of("someOtherOutputId", "collectionId"))))
                        .build())
                .build();

        assertThat(jobGetResponse.getCollectionId(outputId)).isNull();
    }

    @Test
    public void testGetCollectionId_ThrowsClassCastException_WhenCollectionIsNotAMap() {
        String outputId = "outputId";

        JobGetResponse jobGetResponse = JobGetResponse.builder()
                .config(JobGetResponse.JobConfig.builder().inputs(
                                Map.of("collection", List.of("notAMap")))
                        .build())
                .build();

        assertThatThrownBy(() -> jobGetResponse.getCollectionId(outputId))
                .isInstanceOf(ClassCastException.class)
                .hasMessageContaining("class java.lang.String cannot be cast to class java.util.Map ");
    }

    static {
        JOB_GET_RESPONSE_FROM_ALL_ATTRS = JobGetResponse.builder()
                .id(10L)
                .created(OffsetDateTime.of(2025, 1, 1, 10, 15, 20, 0, ZoneOffset.UTC))
                .startTime(LocalDateTime.of(2025, 1, 1, 10, 15, 25, 0))
                .endTime(LocalDateTime.of(2025, 2, 1, 10, 15, 20, 0))
                .lastUpdated(OffsetDateTime.of(2025, 2, 1, 10, 15, 25, 0, ZoneOffset.UTC))
                .phase(JobLaunchResponse.Phase.PROCESSING)
                .status(JobLaunchResponse.Status.RUNNING)
                .serviceName("serviceName")
                .outputs(Map.of("out", List.of("eopaas://outputProduct/filename")))
                .outputFiles(List.of(
                        OutputFile.builder()
                                .filename("filename")
                                .links(Map.of("download", Link.of("http://localhost/download")))
                                .build()
                ))
                .extId("c7d6fedd-5e2d-4a31-b359-95c69e16ca92")
                .config(JobGetResponse.JobConfig.builder()
                        .inputs(Map.of("in",
                                List.of("eopaas://outputProduct/filename")))
                        .build())
                .subJobStatusCounts(Map.of(JobResponse.Status.RUNNING, 1, JobResponse.Status.COMPLETED, 2))
                .build();
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModules(new JavaTimeModule());
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

}
