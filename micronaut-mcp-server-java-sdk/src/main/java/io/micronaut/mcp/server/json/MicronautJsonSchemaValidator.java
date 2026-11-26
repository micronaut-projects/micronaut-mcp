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

import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.clhm.ConcurrentLinkedHashMap;
import io.micronaut.json.JsonMapper;
import io.micronaut.jsonschema.validation.JsonSchemaValidator;
import io.micronaut.jsonschema.validation.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Set;

/**
 * MCP {@link io.modelcontextprotocol.json.schema.JsonSchemaValidator} backed by Micronaut JSON Schema Validator {@link JsonSchemaValidator}.
 * The JSON of each schema Map is cached by identity, so a schema Map must not be mutated after its first validation.
 */
public class MicronautJsonSchemaValidator implements io.modelcontextprotocol.json.schema.JsonSchemaValidator {
    private static final Logger LOG = LoggerFactory.getLogger(MicronautJsonSchemaValidator.class);
    private static final int SCHEMA_CACHE_CAPACITY = 512;
    private final JsonMapper jsonMapper;
    private final JsonSchemaValidator validator;
    /**
     * JSON text of the schemas seen so far, keyed by the identity of the schema Map. The SDK passes the same
     * input and output schema Map instances on every call, so a lookup is constant time instead of serializing
     * and hashing the schema. A schema Map must not be mutated after its first validation, or it is validated against
     * stale JSON. The cache is bounded so that schemas of removed tools are eventually evicted.
     */
    private final Map<SchemaKey, String> schemaJsonCache = new ConcurrentLinkedHashMap.Builder<SchemaKey, String>()
        .maximumWeightedCapacity(SCHEMA_CACHE_CAPACITY)
        .build();

    public MicronautJsonSchemaValidator(JsonMapper jsonMapper,
                                        JsonSchemaValidator validator) {
        this.jsonMapper = jsonMapper;
        this.validator = validator;
    }

    @Override
    public ValidationResponse validate(Map<String, Object> schema, Object structuredContent) {
        if (schema == null) {
            throw new IllegalArgumentException("Schema must not be null");
        }
        if (structuredContent == null) {
            throw new IllegalArgumentException("Structured content must not be null");
        }
        // Serialized once and reused by the SDK as text content. The Micronaut JSON Schema validators (the built-in one and
        // the NetworkNT one, which replaces it when present) read a String value as JSON text rather than as a JSON string
        // literal; that contract is documented by micronaut-projects/micronaut-json-schema#425, as the validator API has no
        // dedicated JSON text overload.
        String jsonStructuredOutput;
        try {
            jsonStructuredOutput = jsonMapper.writeValueAsString(structuredContent);
        } catch (IOException | RuntimeException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Structured content cannot be serialized", e);
            }
            // The cause is meant for the developer, so it is only logged
            return ValidationResponse.asInvalid("Structured content cannot be serialized");
        }
        try {
            Set<? extends ValidationMessage> validationResult = validator.validate(jsonStructuredOutput, schemaJson(schema));
            if (CollectionUtils.isNotEmpty(validationResult)) {
                // The validator validates both tool arguments against the input schema and structured content against the
                // output schema
                return ValidationResponse
                    .asInvalid("Validation failed: content does not match the tool JSON Schema. "
                        + "Validation errors: " + validationResult);
            }
            return ValidationResponse.asValid(jsonStructuredOutput);
        } catch (IOException | RuntimeException e) {
            // A schema that cannot be serialized, read or compiled. The cause is meant for the developer, so it is only logged
            if (LOG.isErrorEnabled()) {
                LOG.error("The tool JSON Schema could not be applied", e);
            }
            return ValidationResponse.asInvalid("The tool JSON Schema could not be applied");
        }
    }

    private String schemaJson(Map<String, Object> schema) throws IOException {
        SchemaKey schemaKey = new SchemaKey(schema);
        // A plain read first, as computeIfAbsent allocates even when the JSON is already cached
        String json = schemaJsonCache.get(schemaKey);
        if (json != null) {
            return json;
        }
        try {
            return schemaJsonCache.computeIfAbsent(schemaKey, key -> {
                try {
                    return jsonMapper.writeValueAsString(key.schema());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * Identity based key for a schema Map.
     *
     * @param schema The schema
     */
    private record SchemaKey(Map<String, Object> schema) {
        @Override
        public boolean equals(Object o) {
            return o instanceof SchemaKey(var other) && other == schema;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(schema);
        }
    }
}
