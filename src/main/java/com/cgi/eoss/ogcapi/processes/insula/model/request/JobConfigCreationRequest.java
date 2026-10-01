package com.cgi.eoss.ogcapi.processes.insula.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Multimap;
import lombok.*;

import java.net.URI;

/**
 * The request body to be sent to perform the creation of a Job Config on Insula.
 */
@Value
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobConfigCreationRequest {

    private final URI service;

    private final Multimap<String, Object> inputs;

}
