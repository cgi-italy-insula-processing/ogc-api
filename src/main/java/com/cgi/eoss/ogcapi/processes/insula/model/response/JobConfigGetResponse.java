package com.cgi.eoss.ogcapi.processes.insula.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobConfigGetResponse {

    @JsonProperty("_embedded")
    private Embedded embedded;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Embedded {
        private Service service;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Service {
        private String dockerTag;
    }
}
