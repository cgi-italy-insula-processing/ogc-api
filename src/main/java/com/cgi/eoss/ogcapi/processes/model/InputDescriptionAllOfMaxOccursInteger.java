package com.cgi.eoss.ogcapi.processes.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InputDescriptionAllOfMaxOccursInteger implements InputDescriptionAllOfMaxOccurs {
    private Integer value;
    @JsonValue
    public Integer getValue() { return value; }
}
