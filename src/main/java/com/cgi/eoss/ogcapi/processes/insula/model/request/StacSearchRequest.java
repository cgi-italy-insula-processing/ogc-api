package com.cgi.eoss.ogcapi.processes.insula.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * The query parameters to be sent to perform a STAC Search on Insula.
 */

@Value
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StacSearchRequest {

    private final String catalogue;

    private final String identifier;

    private final String collection;
}
