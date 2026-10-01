package com.cgi.eoss.ogcapi.processes.model.inputs;

import com.cgi.eoss.ogcapi.processes.model.Bbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class InputValueNoObjectArrayTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final InputValueNoObjectArray<InputValueNoObjectInteger> INPUT_VALUE_NO_OBJECT_ARRAY =
            new InputValueNoObjectArray<>(List.of(new InputValueNoObjectInteger(10),
                    new InputValueNoObjectInteger(20), new InputValueNoObjectInteger(30)));

    private static final String INPUT_VALUE_NO_OBJECT_ARRAY_JSON = "[10,20,30]";

    @Test
    public void testGet_ReturnsListOfIntegers_WhenArrayContainsIntegerTypeElements() {
        InputValueNoObjectArray<InputValueNoObjectInteger> integerInputValueNoObjectArray = new InputValueNoObjectArray<>(
                List.of(new InputValueNoObjectInteger(10),
                        new InputValueNoObjectInteger(20),
                        new InputValueNoObjectInteger(30)));
        assertThat(integerInputValueNoObjectArray.get()).isEqualTo(List.of(10,20,30));
    }

    @Test
    public void testGet_ReturnsListOfDoubles_WhenArrayContainsDoubleTypeElements() {
        InputValueNoObjectArray<InputValueNoObjectDouble> integerInputValueNoObjectArray = new InputValueNoObjectArray<>(
                List.of(new InputValueNoObjectDouble(1.0),
                        new InputValueNoObjectDouble(2.0),
                        new InputValueNoObjectDouble(3.0)));
        assertThat(integerInputValueNoObjectArray.get()).isEqualTo(List.of(1.0,2.0,3.0));
    }

    @Test
    public void testGet_ReturnsListOfStrings_WhenArrayContainsStringTypeElements() {
        InputValueNoObjectArray<InputValueNoObjectString> integerInputValueNoObjectArray = new InputValueNoObjectArray<>(
                List.of(new InputValueNoObjectString("one"),
                        new InputValueNoObjectString("two"),
                        new InputValueNoObjectString("three")));
        assertThat(integerInputValueNoObjectArray.get()).isEqualTo(List.of("one", "two", "three"));
    }

    @Test
    public void testGet_ReturnsListOfBooleans_WhenArrayContainsBooleanTypeElements() {
        InputValueNoObjectArray<InputValueNoObjectBoolean> integerInputValueNoObjectArray = new InputValueNoObjectArray<>(
                List.of(new InputValueNoObjectBoolean(true),
                        new InputValueNoObjectBoolean(true),
                        new InputValueNoObjectBoolean(false)));
        assertThat(integerInputValueNoObjectArray.get()).isEqualTo(List.of(true, true, false));
    }

    @Test
    public void testGet_ReturnsListOfBboxes_WhenArrayContainsBboxTypeElements() {
        InputValueNoObjectArray<InputValueNoObjectBbox> integerInputValueNoObjectArray = new InputValueNoObjectArray<>(
                List.of(new InputValueNoObjectBbox(new Bbox(List.of(new BigDecimal(10)))),
                        new InputValueNoObjectBbox(new Bbox(List.of(new BigDecimal(20)))),
                        new InputValueNoObjectBbox(new Bbox(List.of(new BigDecimal(30))))));
        assertThat(integerInputValueNoObjectArray.get()).isEqualTo(List.of(new Bbox(List.of(new BigDecimal(10))),
                new Bbox(List.of(new BigDecimal(20))), new Bbox(List.of(new BigDecimal(30)))));
    }

    @Test
    public void testSerialize_SerializesObjectWithGetKeyAndArrayOfItems() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(INPUT_VALUE_NO_OBJECT_ARRAY),
                INPUT_VALUE_NO_OBJECT_ARRAY_JSON,
                JSONCompareMode.STRICT);
    }
}
