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
package io.micronaut.mcp.server.registry;

import io.micronaut.core.annotation.Internal;

@Internal
final class JsonSchemaUtils {
    /**
     * @see <a href="https://json-schema.org/understanding-json-schema/reference/type">JSON Schema Type</a>
     */
    public static final String TYPE_STRING = "string";
    public static final String TYPE_NUMBER = "number";
    public static final String TYPE_OBJECT = "object";
    public static final String TYPE_ARRAY = "array";
    public static final String TYPE_BOOL = "boolean";
    public static final String TYPE_NULL = "null";

    private JsonSchemaUtils() {

    }
}
