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
package io.micronaut.mcp.conf;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Internal;

@ConfigurationProperties(ResourcesConfiguration.PREFIX)
@Internal
final class ResourcesConfigurationProperties implements ResourcesConfiguration {
    private boolean listChanged = DEFAULT_LIST_CHANGED;
    private boolean subscribe = DEFAULT_SUBSCRIBE;

    @Override
    public boolean isSubscribe() {
        return subscribe;
    }

    /**
     *
     * @param subscribe whether the client can subscribe to be notified of changes to individual resources. Default value `false`.
     */
    public void setSubscribe(boolean subscribe) {
        this.subscribe = subscribe;
    }

    @Override
    public boolean isListChanged() {
        return listChanged;
    }

    /**
     *
     * @param listChanged whether the server will emit notifications when the list of available resources changes. Default value `false`.
     */
    public void setListChanged(boolean listChanged) {
        this.listChanged = listChanged;
    }
}
