package com.cgi.eoss.ogcapi.processes.insula.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * Represents the response of Insula /services endpoint.
 * This class contains an embedded list of services.
 */

@Value
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetServicesResponse {

    @JsonProperty("_embedded")
    private final GetServicesResponse.EmbeddedServices embeddedServices;

    /**
     * Class representing a list of embedded services in Get Service response.
     */
    @Value
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmbeddedServices {

        private final List<ServiceResponse> services;

    }
}
