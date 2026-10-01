package com.cgi.eoss.ogcapi.processes.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Wrapper class that holds a specific object of type T, defined as one of the types listed
 * in the open-api specs related definition.
 * @param <T> the type of the object wrapped by this class.
 */
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public abstract class InputValueNoObjectOneOf<T> implements InputValueNoObject {

    protected final T value;

    /**
     * Gets the wrapped object.
     * @return the wrapped object.
     */
    @JsonUnwrapped
    @JsonValue
    public Object get() { return this.value; }
}
