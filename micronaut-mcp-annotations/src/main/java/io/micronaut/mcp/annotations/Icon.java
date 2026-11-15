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
 * An icon of an MCP tool, prompt, resource or resource template, declared on the annotated method.
 *
 * <pre>
 * &#64;Tool
 * &#64;Icon(src = "https://example.com/weather.svg", mimeType = "image/svg+xml")
 * String weather(String city) { ... }
 * </pre>
 *
 * @see <a href="https://modelcontextprotocol.io/specification/2025-11-25/basic/index#icons">Icons</a>
 * @since 2.2.0
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
@Repeatable(Icons.class)
public @interface Icon {

    /**
     * @return The URI of the icon: an HTTPS URL or a {@code data:} URI.
     */
    String src();

    /**
     * @return The MIME type of the icon, if it cannot be inferred from the URI.
     */
    String mimeType() default "";

    /**
     * @return The sizes the icon is available in, such as {@code 48x48}, or {@code any} for a scalable format.
     */
    String[] sizes() default {};

    /**
     * @return The theme the icon is designed for, {@code light} or {@code dark}. Empty for any theme.
     */
    String theme() default "";
}
