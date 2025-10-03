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
 * Method annotation to define an MCP Prompt Completion.
 * <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/utilities/completion#requesting-completions">Requesting Completions</a>
 * Micronaut registers Completion singletons for beans with executable methods (e.g. methods in a class annotated with `@Singleton`) annotated with {@link PromptCompletion}.
 *
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
@McpPrimitive
public @interface PromptCompletion {

    /**
     * Constant value for {@link #name()} indicating that the annotated element's name should be used as-is.
     */
    String ELEMENT_NAME = "<<element name>>";

    /**
     * @return The prompt name this completion refers to.
     */
    String name() default ELEMENT_NAME;
}
