package com.cgi.eoss.ogcapi.processes.insula.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.hateoas.Link;

import java.util.Map;

/**
 * Class that holds the output file object description for an output file
 * located on Insula.
 */
@Value
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OutputFile {

    private final String filename;

    @JsonProperty("_links")
    private final Map<String, Link> links;
}