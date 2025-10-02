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
package io.micronaut.mcp.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Method annotation to define an MCP Resource Template handler.
 * The annotated method will be invoked to read the contents of the declared resource template.
 *
 * This allows to define resource templates declaratively.
 *
 * Minimal usage:
 * <pre>
 * &#64;Singleton
 * class MyResourceTemplates {
 *   &#64;ResourceTemplate(uriTemplate = "example://hello/{locale}", name = "hello", title = "Hello", description = "Hello text", mimeType = "text/plain")
 *   String hello(String locale) {
 *     if (locale.equals("es) {
 *         return "Hola Mundo";
 *     }
 *     return "Hello World";
 *   }
 * }
 * </pre>
 *
 * Supported method parameters:
 * - parameter of type {@code io.micronaut.mcp.server.context.MicronautMcpTransportContext}
 * - parameter of type {@code io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest}
 *
 * Supported return types:
 * - {@code String}: returned as text content with the configured {@link #mimeType()}
 * - {@code io.modelcontextprotocol.spec.McpSchema.ReadResourceResult}: used as-is
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
@McpPrimitive
public @interface ResourceTemplate {
    String DEFAULT_MIME_TYPE = "text/plain";

    /**
     * Constant value for {@link #name()} indicating that the annotated element's name should be used as-is.
     */
    String ELEMENT_NAME = "<<element name>>";

    /**
     * @return Each resource must have a unique name. By default, the name is derived from the name of the annotated method.
     */
    String name() default ELEMENT_NAME;

    /**
     * @return The resource URI this handler serves (e.g. ""file:///{path}").
     */
    String uriTemplate() default "pgn://round/{round}";

    /**
     * @return A concise human-readable title of the resource.
     */
    String title() default "";

    /**
     * @return A human-readable description of the resource.
     */
    String description() default "";

    /**
     * @return The MIME type of the returned content (e.g. "text/plain", "application/json").
     */
    String mimeType() default DEFAULT_MIME_TYPE;
}
