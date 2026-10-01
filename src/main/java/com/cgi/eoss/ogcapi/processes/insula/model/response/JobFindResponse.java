package com.cgi.eoss.ogcapi.processes.insula.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *     Class that represents the response object for the Find Jobs endpoint.
 * </p>
 */

@Value
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobFindResponse {

    @JsonProperty("_embedded")
    private final EmbeddedJobs embeddedJobs;

    @JsonProperty("page")
    private final Map<String, Object> page;

    @Value
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmbeddedJobs {

        private final List<JobGetResponse> jobs;

    }

}
