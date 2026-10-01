package com.cgi.eoss.ogcapi.processes.insula.model.response;

import com.cgi.eoss.ogcapi.processes.insula.model.Cwl;
import com.cgi.eoss.ogcapi.processes.insula.model.ServiceDescriptor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.hateoas.Link;

import java.util.Map;

/**
 * The response body sent when the creation of a new Service on Insula has been performed.
 */
@Value
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceResponse {

    private final Long id;

    private final String name;

    private final String description;

    private final String status;

    private final Cwl cwl;

    private final ServiceDescriptor serviceDescriptor;

    @JsonProperty("_links")
    private final Map<String, Link> links;

}
