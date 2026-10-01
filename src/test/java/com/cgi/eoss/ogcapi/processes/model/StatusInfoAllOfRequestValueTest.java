package com.cgi.eoss.ogcapi.processes.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.lang.Exception;
import static org.assertj.core.api.Assertions.assertThat;

public class StatusInfoAllOfRequestValueTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final StatusInfoAllOfRequestValue STATUS_INFO_ALL_OF_REQUEST_VALUE_LINK = new StatusInfoAllOfRequestValue(
            new Link("hrefValue"));

    private static final String STATUS_INFO_ALL_OF_REQUEST_VALUE_LINK_JSON =
            "{\"href\": \"hrefValue\",\"rel\": null,\"type\": null,\"hreflang\": null,\"title\": null}";

    @Test
    public void testGet_ReturnsObjectValue() {
        assertThat(STATUS_INFO_ALL_OF_REQUEST_VALUE_LINK.getValue()).isEqualTo(new Link("hrefValue"));
    }

    @Test
    public void testSerialize_SerializesObjectAsLink_WhenValueIsOfLinkType() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(STATUS_INFO_ALL_OF_REQUEST_VALUE_LINK),
                STATUS_INFO_ALL_OF_REQUEST_VALUE_LINK_JSON,
                JSONCompareMode.STRICT);
    }

    @Test
    public void testSerialize_SerializesObjectAsStringValue_WhenValueIsOfStringType() throws Exception {
        assertThat(OBJECT_MAPPER.writeValueAsString(new StatusInfoAllOfRequestValue("stringValue")))
                .isEqualTo("\"stringValue\"");
    }

}
