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

import io.micronaut.json.JsonMapper;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.json.schema.JsonSchemaValidatorSupplier;

/**
 * An {@link JsonSchemaValidatorSupplier} that provides a {@link MicronautJsonSchemaValidator}.
 */
public class MicronautJsonSchemaValidatorSupplier implements JsonSchemaValidatorSupplier {

    @Override
    public JsonSchemaValidator get() {
        return new MicronautJsonSchemaValidator(JsonMapper.createDefault(), null);
    }
}
