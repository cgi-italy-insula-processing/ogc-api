package com.cgi.eoss.ogcapi.processes.model.inputs;

import com.cgi.eoss.ogcapi.processes.model.Input;
import com.cgi.eoss.ogcapi.processes.model.InputValueNoObjectOneOf;

import java.util.List;

/**
 * A class representing an input value that is an array of objects of type T.
 * @param <T> the type of the objects in the array.
 */
public class InputValueNoObjectArray <T extends Input> extends InputValueNoObjectOneOf<List<T>> {

    /**
     * Creates an instance of this class with the given value.
     * @param value a list of objects of type T.
     */
    public InputValueNoObjectArray(List<T> value) { super(value); }

    @Override
    public Object get() { return value.stream().map(this::unwrapItem).toList(); }

    private Object unwrapItem(Object o) {
        if (o instanceof InputValueNoObjectOneOf<?> inputValueNoObjectOneOf) {
            return inputValueNoObjectOneOf.get();
        }
        return o;
    }
}
