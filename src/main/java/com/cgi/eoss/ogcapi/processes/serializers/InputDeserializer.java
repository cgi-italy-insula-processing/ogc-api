package com.cgi.eoss.ogcapi.processes.serializers;

import com.cgi.eoss.ogcapi.processes.model.*;
import com.cgi.eoss.ogcapi.processes.model.inputs.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.*;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Custom Json Deserializer for classes implementing {@link Input}.
 */
@Log4j2
public class InputDeserializer extends StdDeserializer<Input> {

    public InputDeserializer() { this(Input.class); }

    protected InputDeserializer(Class<?> vc) { super(vc); }

    @Override
    public Input deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        return deserializeInput(mapper.readTree(jsonParser), mapper);
    }

    private static Input deserializeInput(Object root, ObjectMapper mapper) {
        LOG.debug("Deserializing {}", root);

        if (root instanceof JsonNode node) {
            return deserializeNode(node, mapper);
        }

        Class<?> rootClass = root.getClass();
        LOG.error("Failed to deserialize input {} of type {}", root, rootClass);
        throw new IllegalArgumentException("Failed to deserialize input object: " + root + " of type " + rootClass);
    }

    private static Input deserializeNode(JsonNode node, ObjectMapper mapper) {
        JsonNodeType nodeType = node.getNodeType();
        switch (nodeType) {
            case STRING -> {
                return new InputValueNoObjectString(node.textValue());
            }
            case BOOLEAN -> {
                return new InputValueNoObjectBoolean(node.booleanValue());
            }
            case NUMBER -> {
                return deserializeNumericNode((NumericNode) node);
            }
            case ARRAY -> {
                return deserializeArrayNode((ArrayNode) node, mapper);
            }
            case OBJECT -> {
                return deserializeObjectNode((ObjectNode) node, mapper);
            }
            default -> throw new IllegalArgumentException("Unrecognized input type: " + nodeType);
        }
    }

    private static Input deserializeNumericNode(NumericNode numericNode) {
        if (numericNode.isInt()) {
            return new InputValueNoObjectInteger(numericNode.intValue());
        }
        if (numericNode.isDouble()) {
            return new InputValueNoObjectDouble(numericNode.doubleValue());
        }
        throw new IllegalArgumentException("Unsupported numeric input type: " + numericNode.numberType());
    }

    private static Input deserializeArrayNode(ArrayNode arrayNode, ObjectMapper mapper) {
        assertHomogeneousItemTypes(arrayNode);
        return new InputValueNoObjectArray<>(deserializeArrayItems(arrayNode, mapper));
    }

    private static Input deserializeObjectNode(ObjectNode objectNode, ObjectMapper mapper) {
        if (DeserializerUtils.isLink(objectNode)) {
            return DeserializerUtils.deserialize(mapper, objectNode.toString(), Link.class);
        }
        if (DeserializerUtils.isBbox(objectNode)) {
            return new InputValueNoObjectBbox(DeserializerUtils.deserialize(mapper, objectNode.toString(), Bbox.class));
        }
        throw new IllegalArgumentException("Failed to deserialize input object: " + objectNode + " of type " + objectNode.getClass());
    }

    private static void assertHomogeneousItemTypes(ArrayNode arrayNode) {
        int numberOfItemTypes = StreamSupport.stream(arrayNode.spliterator(), false)
                .map(JsonNode::getClass).collect(Collectors.toSet()).size();
        if (numberOfItemTypes > 1) {
            LOG.error("Input array is not homogeneous: found {} different item types.", numberOfItemTypes);
            throw new IllegalArgumentException("Input array is not homogeneous: found " + numberOfItemTypes + " different item types.");
        }
    }

    private static List<Input> deserializeArrayItems(ArrayNode arrayNode, ObjectMapper mapper) {
        return StreamSupport.stream(arrayNode.spliterator(), false)
                .map(item -> deserializeInput(item, mapper))
                .toList();
    }


}
