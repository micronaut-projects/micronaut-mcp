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

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.type.Argument;
import io.micronaut.serde.Decoder;
import io.micronaut.serde.Deserializer;
import io.micronaut.serde.Encoder;
import io.micronaut.serde.Serializer;
import io.micronaut.serde.util.NullableDeserializer;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

import java.io.IOException;

@Singleton
@Internal
final class McpSchemaRoleSerde implements Serializer<McpSchema.Role>, NullableDeserializer<McpSchema.Role> {

    @Override
    public void serialize(Encoder encoder,
                          Serializer.EncoderContext context,
                          Argument<? extends McpSchema.Role> type,
                          McpSchema.Role value) throws IOException {
        if (value == null) {
            encoder.encodeNull();
            return;
        }
        encoder.encodeString(switch (value) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
        });
    }

    @Override
    public McpSchema.Role deserializeNonNull(Decoder decoder,
                                             Deserializer.DecoderContext context,
                                             Argument<? super McpSchema.Role> type) throws IOException {
        return switch (decoder.decodeString()) {
            case "user", "USER" -> McpSchema.Role.USER;
            case "assistant", "ASSISTANT" -> McpSchema.Role.ASSISTANT;
            default -> throw decoder.createDeserializationException("Invalid role value", type);
        };
    }
}
