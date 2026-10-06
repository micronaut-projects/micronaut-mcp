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

import tools.jackson.core.JacksonException;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.clhm.ConcurrentLinkedHashMap;
import io.micronaut.json.JsonMapper;
import io.micronaut.jsonschema.validation.JsonSchemaValidator;
import io.micronaut.jsonschema.validation.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * MCP {@link io.modelcontextprotocol.json.schema.JsonSchemaValidator} backed by Micronaut JSON Schema Validator {@link JsonSchemaValidator}.
 *
 */
public class MicronautJsonSchemaValidator implements io.modelcontextprotocol.json.schema.JsonSchemaValidator {
    private static final Logger LOG = LoggerFactory.getLogger(MicronautJsonSchemaValidator.class);
    private static final int SCHEMA_CACHE_CAPACITY = 512;
    private final JsonMapper jsonMapper;
    private final JsonSchemaValidator validator;
    /**
     * JSON text of the schemas seen so far, keyed by the identity of the schema Map. The SDK passes the same
     * input and output schema Map instances on every call, so a lookup is constant time instead of serializing
     * and hashing the schema. The cache is bounded so that schemas of removed tools are eventually evicted.
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
        try {
            // Serialized once and reused by the SDK as text content. DefaultJsonSchemaValidator reads a String value
            // as JSON text rather than as a JSON string literal; that contract is documented by
            // micronaut-projects/micronaut-json-schema#425, as the validator API has no dedicated JSON text overload.
            String jsonStructuredOutput = jsonMapper.writeValueAsString(structuredContent);
            Set<? extends ValidationMessage> validationResult = validator.validate(jsonStructuredOutput, schemaJson(schema));
            if (CollectionUtils.isNotEmpty(validationResult)) {
                return ValidationResponse
                    .asInvalid("Validation failed: structuredContent does not match tool outputSchema. "
                        + "Validation errors: " + validationResult);
            }
            return ValidationResponse.asValid(jsonStructuredOutput);
        } catch (JacksonException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Error parsing schema: {}", e);
            }
            return ValidationResponse.asInvalid("Error parsing tool JSON Schema: " + e.getMessage());
        } catch (IOException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Unexpected error: {}", e);
            }
            return ValidationResponse.asInvalid("Unexpected validation error: " + e.getMessage());
        }
    }

    private String schemaJson(Map<String, Object> schema) throws IOException {
        SchemaKey key = new SchemaKey(schema);
        String json = schemaJsonCache.get(key);
        if (json == null) {
            json = jsonMapper.writeValueAsString(schema);
            schemaJsonCache.put(key, json);
        }
        return json;
    }

    /**
     * Identity based key for a schema Map.
     *
     * @param schema The schema
     */
    private record SchemaKey(Map<String, Object> schema) {
        @Override
        public boolean equals(Object o) {
            return o instanceof SchemaKey other && other.schema == schema;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(schema);
        }
    }
}
