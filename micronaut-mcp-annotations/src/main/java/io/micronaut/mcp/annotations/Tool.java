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

import io.micronaut.context.annotation.Executable;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Method annotation to define an MCP Tool.
 * <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/tools">Tools</a>
 * Micronaut registers Tools singletons for beans with executable methods (e.g. methods in a class annotated with `@Singleton`) annotated with {@link Tool}.
 *
 * Forked from: https://github.com/quarkiverse/quarkus-mcp-server/blob/main/core/runtime/src/main/java/io/quarkiverse/mcp/server/Tool.java
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
@McpPrimitive
public @interface Tool {
    /**
     * Constant value for {@link #name()} indicating that the annotated element's name should be used as-is.
     */
    String ELEMENT_NAME = "<<element name>>";

    /**
     * Each tool must have a unique name. By default, the name is derived from the name of the annotated method.
     * @return {@value #ELEMENT_NAME}
     */
    String name() default ELEMENT_NAME;

    /**
     * @return A human-readable description of the tool. A hint to the model.
     */
    String description() default "";
}
