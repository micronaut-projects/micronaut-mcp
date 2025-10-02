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
package io.micronaut.mcp.primitives.prompts;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Introspected;

/**
 * An MCP Prompt Argument.
 */
@Internal
@Introspected
public class PromptArgument {
    private String name;
    private String title;
    private String description;
    private boolean required = true;

    /**
     *
     * @return Prompt argument Name
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @param name Prompt Argument Name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return Whether the prompt argument is required
     */
    public boolean isRequired() {
        return required;
    }

    /**
     *
     * @param required Whether the prompt argument is required
     */
    public void setRequired(boolean required) {
        this.required = required;
    }

    /**
     *
     * @return Prompt argument description
     */
    public String getDescription() {
        return description;
    }

    /**
     *
     * @param description Prompt argument description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     *
     * @return Prompt argument title
     */
    public String getTitle() {
        return title;
    }

    /**
     *
     * @param title Prompt argument title
     */
    public void setTitle(String title) {
        this.title = title;
    }
}
