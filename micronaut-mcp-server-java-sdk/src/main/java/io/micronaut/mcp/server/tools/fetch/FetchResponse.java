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
package io.micronaut.mcp.server.tools.fetch;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.jsonschema.JsonSchema;

import java.util.Map;
import java.util.Objects;

/**
 * @param id a unique ID for the document or search result item
 * @param title a string title for the search result item
 * @param text The full text of the document or item
 * @param url a URL to the document or search result item. Useful for citing specific resources in research.
 * @param metadata an optional key/value pairing of data about the result
 */
@Introspected
@JsonSchema
public record FetchResponse(
    @NonNull String id,
    @NonNull String title,
    @NonNull String text,
    @NonNull String url,
    @Nullable Map<String, Object> metadata
) {
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fetch Response Builder.
     */
    public static final class Builder {
        private String id;
        private String title;
        private String text;
        private String url;
        private Map<String, Object> metadata;

        private Builder() {

        }

        @NonNull
        public Builder id(@NonNull String id) {
            this.id = id;
            return this;
        }

        @NonNull
        public Builder title(@NonNull String title) {
            this.title = title;
            return this;
        }

        @NonNull
        public Builder text(@NonNull String text) {
            this.text = text;
            return this;
        }

        @NonNull
        public Builder url(@NonNull String url) {
            this.url = url;
            return this;
        }

        @NonNull
        public Builder metadata(@NonNull Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        @NonNull
        public FetchResponse build() {
            return new FetchResponse(
                Objects.requireNonNull(id),
                Objects.requireNonNull(title),
                Objects.requireNonNull(text),
                Objects.requireNonNull(url),
                metadata
            );
        }
    }
}
