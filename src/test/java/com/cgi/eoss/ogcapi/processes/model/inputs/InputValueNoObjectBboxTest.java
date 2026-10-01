package com.cgi.eoss.ogcapi.processes.model.inputs;

import com.cgi.eoss.ogcapi.processes.model.Bbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class InputValueNoObjectBboxTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final InputValueNoObjectBbox INPUT_VALUE_NO_OBJECT_BBOX = new InputValueNoObjectBbox(new Bbox(List.of(
            BigDecimal.valueOf(10), BigDecimal.valueOf(20), BigDecimal.valueOf(30), BigDecimal.valueOf(40))));

    private static final String INPUT_VALUE_NO_OBJECT_BBOX_JSON = "{\"bbox\":[10, 20, 30, 40],\"crs\":\"http://www.opengis.net/def/crs/OGC/1.3/CRS84\"}";

    @Test
    public void testGet_ReturnsBboxValue() {
        assertThat(INPUT_VALUE_NO_OBJECT_BBOX.get()).isEqualTo(new Bbox(List.of(
                BigDecimal.valueOf(10), BigDecimal.valueOf(20), BigDecimal.valueOf(30), BigDecimal.valueOf(40))));
    }

    @Test
    public void testSerialize_SerializesObjectAsBboxObject() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(INPUT_VALUE_NO_OBJECT_BBOX),
                INPUT_VALUE_NO_OBJECT_BBOX_JSON,
                JSONCompareMode.STRICT);
    }
}
