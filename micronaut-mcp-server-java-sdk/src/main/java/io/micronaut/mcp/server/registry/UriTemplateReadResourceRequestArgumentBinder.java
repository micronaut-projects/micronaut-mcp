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

import io.micronaut.context.annotation.DefaultImplementation;
import io.micronaut.core.annotation.Indexed;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.bind.ArgumentBinder;

/**
 * Marker interface for a {@link ArgumentBinder} whose source type is a {@link UriTemplateReadResourceRequest}.
 * @param <T> The argument type
 */
@Indexed(UriTemplateReadResourceRequestArgumentBinder.class)
@DefaultImplementation(DefaultUriTemplateReadResourceRequestArgumentBinder.class)
@Internal
sealed interface UriTemplateReadResourceRequestArgumentBinder<T> extends ArgumentBinder<T, UriTemplateReadResourceRequest>
    permits DefaultUriTemplateReadResourceRequestArgumentBinder {
}
