package com.cgi.eoss.ogcapi.processes.insula.model.response;

import com.cgi.eoss.ogcapi.processes.model.InlineOrRefData;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.hateoas.Link;

import java.util.List;

/**
 * Class that represents the response of a STAC search request on Insula.
 * <p>
 *      This class is used to deserialize the response from a STAC search request.
 * </p>
 */

@Value
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StacSearchResponse implements InlineOrRefData {

    private final List<Object> features;

    private final String type;

    private final Integer numberReturned;

    private final Integer numberMatched;

    private final List<Link> links;
}
