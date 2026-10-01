package com.cgi.eoss.ogcapi.processes.model.inputs;

import com.cgi.eoss.ogcapi.processes.model.Bbox;
import com.cgi.eoss.ogcapi.processes.model.InputValueNoObjectOneOf;

/**
 * Class representing an input value of type Bbox.
 */
public class InputValueNoObjectBbox extends InputValueNoObjectOneOf<Bbox> {

    /**
     * Creates an instance of this class with the given value.
     * @param value the Bbox object.
     */
    public InputValueNoObjectBbox(Bbox value) { super(value); }
}
