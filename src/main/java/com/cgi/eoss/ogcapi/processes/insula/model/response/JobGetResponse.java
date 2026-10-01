package com.cgi.eoss.ogcapi.processes.insula.model.response;

import com.cgi.eoss.ogcapi.processes.insula.model.OutputFile;
import com.cgi.eoss.ogcapi.processes.serializers.JobConfigInputsDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.Link;
import lombok.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Value
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobGetResponse implements JobResponse {

    private final Long id;

    private final Status status;

    private final Phase phase;

    private final LocalDateTime endTime;

    private final LocalDateTime startTime;

    private final OffsetDateTime created;

    private final OffsetDateTime lastUpdated;

    private final String serviceName;

    private final Long serviceId;

    private final Map<String, List<String>> outputs;

    private final List<OutputFile> outputFiles;

    private final String extId;

    private final JobConfig config;

    private final Map<Status, Integer> subJobStatusCounts;

    @JsonProperty("_links")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private final Map<String, List<Link>> links;

    @Value
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JobConfig {

        @JsonDeserialize(using = JobConfigInputsDeserializer.class)
        private final Map<String, Object> inputs;

    }

    /**
     * Gets the output files' unique identifier given its output ID.
     * @param outputId the output ID.
     * @return the output files' identifier.
     */
    public String getOutputFilesIdentifier(String outputId) {
        String extId = this.getExtId();
        return extId + "_" + outputId;
    }

    /**
     * Gets the collection ID given its output ID.
     * @param outputId the output ID.
     * @return the collection ID.
     */
    public String getCollectionId(String outputId) {
        Map<String, Object> configInputs = this.getConfig().getInputs();
        List<Map<String, Object>> collections = (List<Map<String, Object>>) configInputs.get("collection");
        return (String) collections.stream()
                .filter(collectionsAsMap -> collectionsAsMap.containsKey(outputId))
                .findAny()
                .map(collection -> collection.get(outputId))
                .orElse(null);
    }

}
