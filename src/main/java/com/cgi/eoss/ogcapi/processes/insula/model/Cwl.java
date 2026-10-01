package com.cgi.eoss.ogcapi.processes.insula.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * Immutable record representing a Common Workflow Language object.
 * @param url the URL to the external reference where the CWL is stored.
 * @param document the textual representation of the CWL.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Cwl(String url, String document) { }
