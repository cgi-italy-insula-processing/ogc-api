package com.cgi.eoss.ogcapi.processes.insula.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Class that holds the detailed service configuration for a Service that has been
 * created on Insula.
 */
@Value
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceDescriptor {

    private final String title;

    private final String version;

    private final List<InputOutputParameter> dataInputs;

    private final List<InputOutputParameter> dataOutputs;

    /**
     * Class that holds the details of an input or output parameter that belongs to a Service.
     */
    @Value
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InputOutputParameter {

        private final String id;

        private final String description;

        private final String title;

        private final Integer minOccurs;

        private final Integer maxOccurs;

        private final Map<String, String> defaultAttrs;

        private final Map<String, String> platformMetadata;
    }

}
