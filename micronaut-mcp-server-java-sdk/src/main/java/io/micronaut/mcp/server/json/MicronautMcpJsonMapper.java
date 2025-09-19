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
package io.micronaut.mcp.server.json;

import io.micronaut.core.type.Argument;
import io.micronaut.json.JsonMapper;
import io.micronaut.json.tree.JsonNode;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;

import java.io.IOException;

/**
 * An implementation of {@link McpJsonMapper} that uses Micronaut's {@link JsonMapper} for JSON serialization and deserialization.
 */
public class MicronautMcpJsonMapper implements McpJsonMapper {
    private final JsonMapper jsonMapper;

    public MicronautMcpJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public <T> T readValue(String content, Class<T> type) throws IOException {
        return jsonMapper.readValue(content, type);
    }

    @Override
    public <T> T readValue(byte[] content, Class<T> type) throws IOException {
        return jsonMapper.readValue(content, type);
    }

    @Override
    public <T> T readValue(String content, TypeRef<T> type) throws IOException {
        return jsonMapper.readValue(content, toArgument(type));
    }

    @Override
    public <T> T readValue(byte[] content, TypeRef<T> type) throws IOException {
        return jsonMapper.readValue(content, toArgument(type));
    }

    @Override
    public <T> T convertValue(Object fromValue, Class<T> type) {
        try {
            JsonNode jsonNode = jsonMapper.writeValueToTree(fromValue);
            return jsonMapper.readValueFromTree(jsonNode, type);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error converting value", e);
        }
    }

    @Override
    public <T> T convertValue(Object fromValue, TypeRef<T> type) {
        try {
            JsonNode jsonNode = jsonMapper.writeValueToTree(fromValue);
            return jsonMapper.readValueFromTree(jsonNode, toArgument(type));
        } catch (IOException e) {
            throw new IllegalArgumentException("Error converting value", e);
        }
    }

    @Override
    public String writeValueAsString(Object value) throws IOException {
        return jsonMapper.writeValueAsString(value);
    }

    @Override
    public byte[] writeValueAsBytes(Object value) throws IOException {
        return jsonMapper.writeValueAsBytes(value);
    }

    @SuppressWarnings("unchecked")
    private static <T> Argument<T> toArgument(TypeRef<T> type) {
        return (Argument<T>) Argument.of(type.getType());
    }
}
