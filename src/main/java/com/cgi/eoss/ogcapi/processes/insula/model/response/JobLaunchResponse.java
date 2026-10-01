package com.cgi.eoss.ogcapi.processes.insula.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.hateoas.Link;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;

@Value
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PROTECTED)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobLaunchResponse implements JobResponse {

    private final Long id;

    private final Status status;

    private final Phase phase;

    private final LocalDateTime endTime;

    private final LocalDateTime startTime;

    private final OffsetDateTime created;

    private final OffsetDateTime lastUpdated;

    @JsonProperty("_links")
    private final Map<String, Link> links;
}
