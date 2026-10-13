/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.mcp.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * An entry of the {@code _meta} of an MCP tool, prompt, resource or resource template, declared on the annotated method.
 *
 * <pre>
 * &#64;Tool
 * &#64;Meta(key = "com.example/category", value = "weather")
 * String weather(String city) { ... }
 * </pre>
 *
 * @see <a href="https://modelcontextprotocol.io/specification/2025-11-25/basic/index#_meta">_meta</a>
 * @since 2.2.0
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
@Repeatable(MetaEntries.class)
public @interface Meta {

    /**
     * @return The key, optionally prefixed by a reverse DNS name and a slash. Prefixes containing {@code modelcontextprotocol} or {@code mcp} are reserved.
     */
    String key();

    /**
     * @return The value
     */
    String value();
}
