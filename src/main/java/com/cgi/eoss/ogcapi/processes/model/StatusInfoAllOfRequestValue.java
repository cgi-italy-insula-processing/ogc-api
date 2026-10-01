package com.cgi.eoss.ogcapi.processes.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

/**
 * Wrapper class that holds a specific object, defined as one of the types listed
 * in the open-api specs related definition.
 */
@AllArgsConstructor
public class StatusInfoAllOfRequestValue implements StatusInfoAllOfRequest {

    private Object value;

    /**
     * Gets the wrapped object.
     * @return the wrapped object.
     */
    @JsonValue
    public Object getValue() { return value; }
}
