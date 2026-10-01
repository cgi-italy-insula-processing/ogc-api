package com.cgi.eoss.ogcapi.processes.insula.model.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.hateoas.Link;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class StacSearchResponseTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final StacSearchResponse STAC_SEARCH_RESPONSE_FROM_ALL_ATTRS = StacSearchResponse.builder()
            .features(List.of("feature1", "feature2"))
            .type("type")
            .numberReturned(2)
            .numberMatched(2)
            .links(List.of(Link.of("href", "rel")))
            .build();

    private static final String STAC_SEARCH_RESPONSE_FROM_ALL_ATTRS_JSON = "{\"features\":[\"feature1\",\"feature2\"]," +
            "\"type\":\"type\",\"numberReturned\":2,\"numberMatched\":2," +
            "\"links\":[{\"href\":\"href\",\"rel\":\"rel\"}]}";

    @Test
    public void testSerialize_SerializesAllJsonKeyValues_WhenAllAttributesAreSet() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(STAC_SEARCH_RESPONSE_FROM_ALL_ATTRS),
                STAC_SEARCH_RESPONSE_FROM_ALL_ATTRS_JSON,
                JSONCompareMode.STRICT);
    }

    @Test
    public void testSerialize_SerializesEmptyJson_WhenNoAttributeIsSet() throws Exception {
        JSONAssert.assertEquals(
                OBJECT_MAPPER.writeValueAsString(StacSearchResponse.builder().build()),
                "{}",
                JSONCompareMode.STRICT);
    }

    @Test
    public void testDeserialize_DeserializesAllAttributes_WhenAllJsonKeyValuesAreSet() throws Exception {
        assertThat(
                OBJECT_MAPPER.readValue(STAC_SEARCH_RESPONSE_FROM_ALL_ATTRS_JSON, StacSearchResponse.class))
                .isEqualTo(STAC_SEARCH_RESPONSE_FROM_ALL_ATTRS);
    }

    @Test
    public void testDeserialize_IgnoresUnknownJsonKeys() throws Exception {
        StacSearchResponse stacSearchResponse = OBJECT_MAPPER.readValue(
                "{\"features\":[\"feature1\",\"feature2\"]," +
                        "\"type\":\"type\",\"numberReturned\":2,\"numberMatched\":2," +
                        "\"links\":[{\"href\":\"href\",\"rel\":\"rel\"}]," +
                        "\"unknownKey\":\"unknownValue\"}",
                StacSearchResponse.class
        );
        assertThat(stacSearchResponse).isEqualTo(STAC_SEARCH_RESPONSE_FROM_ALL_ATTRS);
    }
}
