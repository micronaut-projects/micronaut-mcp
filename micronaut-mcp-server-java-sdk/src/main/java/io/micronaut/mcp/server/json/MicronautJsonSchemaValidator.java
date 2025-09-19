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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.micronaut.core.util.CollectionUtils;
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
    private final JsonMapper jsonMapper;
    private final JsonSchemaValidator validator;

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
            Set<? extends ValidationMessage> validationResult = validator.validate(structuredContent, schema);
            if (CollectionUtils.isNotEmpty(validationResult)) {
                return ValidationResponse
                    .asInvalid("Validation failed: structuredContent does not match tool outputSchema. "
                        + "Validation errors: " + validationResult);
            }
            String jsonStructuredOutput = jsonMapper.writeValueAsString(structuredContent);

            return ValidationResponse.asValid(jsonStructuredOutput.toString());
        } catch (JsonProcessingException e) {
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
}
