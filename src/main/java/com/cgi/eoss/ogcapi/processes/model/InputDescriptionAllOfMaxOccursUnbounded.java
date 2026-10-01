package com.cgi.eoss.ogcapi.processes.model;

import com.fasterxml.jackson.annotation.JsonValue;

public class InputDescriptionAllOfMaxOccursUnbounded implements InputDescriptionAllOfMaxOccurs {
    private static final String VALUE = "unbounded";
    @JsonValue
    public String getValue() { return VALUE; }
}
