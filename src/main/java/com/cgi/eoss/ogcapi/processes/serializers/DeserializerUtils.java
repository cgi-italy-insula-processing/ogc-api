package com.cgi.eoss.ogcapi.processes.serializers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.UncheckedIOException;

public class DeserializerUtils {

    /**
     * Checks whether the given input represents a {@link com.cgi.eoss.ogcapi.processes.model.Bbox} object.
     * @param root the input object.
     * @return true if the object represents a Bbox, false otherwise.
     */
    public static boolean isBbox(ObjectNode root) { return root.has("bbox"); }

    /**
     * Checks whether the given input represents a {@link com.cgi.eoss.ogcapi.processes.model.Link} object.
     * @param root the input object.
     * @return true if the object represents a Link, false otherwise.
     */
    public static boolean isLink(ObjectNode root) { return root.has("href"); }

    /**
     * Deserializes the given jsonString with the given mapper into an object of
     * the given clazz type.
     * @param mapper the Mapper that is leveraged to deserialize the object.
     * @param jsonString the object to be deserialized represented as a JSON string.
     * @param clazz the class of the object to be deserialized.
     * @return the deserialized object,
     * @param <T> the type of the object to be deserialized.
     */
    public static <T> T deserialize(ObjectMapper mapper, String jsonString, Class<T> clazz) {
        try {
            return mapper.readValue(jsonString, clazz);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

}
