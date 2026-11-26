/*
 * Copyright 2017-2025 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.mcp.server.stateless;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Order;
import io.micronaut.core.io.buffer.ByteBuffer;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.Headers;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.body.MessageBodyReader;
import io.micronaut.http.codec.CodecException;
import io.micronaut.json.JsonMapper;
import io.micronaut.json.body.JsonMessageHandler;
import io.micronaut.json.tree.JsonNode;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The JSON-RPC envelope read by the HTTP endpoint.
 *
 * @param value the parsed JSON
 */
@Internal
record McpRequestBody(JsonNode value) {

    @Singleton
    @Consumes(MediaType.APPLICATION_JSON)
    @Order(JsonMessageHandler.ORDER - 1)
    static final class Reader implements MessageBodyReader<McpRequestBody> {
        private final Argument<?> valueType;
        private final JsonMessageHandler<Object> jsonReader;

        Reader(JsonMapper mapper) {
            // Jackson's Micronaut JsonNode deserializer uses the default stream configuration.
            valueType = mapper.getStreamConfig().useBigDecimalForFloats()
                ? Argument.OBJECT_ARGUMENT
                : Argument.of(JsonNode.class);
            jsonReader = new JsonMessageHandler<>(mapper.createSpecific(valueType));
        }

        @Override
        public @Nullable McpRequestBody read(Argument<McpRequestBody> type, @Nullable MediaType mediaType,
                                            Headers headers, ByteBuffer<?> buffer) throws CodecException {
            return wrap(jsonReader.read(valueType(), mediaType, headers, buffer));
        }

        @Override
        public @Nullable McpRequestBody read(Argument<McpRequestBody> type, @Nullable MediaType mediaType,
                                            Headers headers, InputStream stream) throws CodecException {
            return wrap(jsonReader.read(valueType(), mediaType, headers, stream));
        }

        @SuppressWarnings("unchecked")
        private Argument<Object> valueType() {
            return (Argument<Object>) valueType;
        }

        private static @Nullable McpRequestBody wrap(@Nullable Object value) {
            return value == null ? null : new McpRequestBody(value instanceof JsonNode node ? node : toTree(value));
        }

        private static JsonNode toTree(@Nullable Object value) {
            if (value instanceof Map<?, ?> map) {
                Map<String, JsonNode> fields = new LinkedHashMap<>(map.size());
                map.forEach((key, field) -> fields.put((String) key, toTree(field)));
                return JsonNode.createObjectNode(fields);
            }
            if (value instanceof Object[] array) {
                return JsonNode.createArrayNode(Arrays.stream(array).map(Reader::toTree).toList());
            }
            if (value instanceof List<?> list) {
                return JsonNode.createArrayNode(list.stream().map(Reader::toTree).toList());
            }
            return JsonNode.from(value);
        }
    }
}
