package com.cgi.eoss.ogcapi.processes.insula.model.request;

import com.cgi.eoss.ogcapi.processes.insula.model.Cwl;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * The request body to be sent to perform the creation of a Service on Insula.
 */
@Value
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceCreationRequest {

    private final Cwl cwl;
}
