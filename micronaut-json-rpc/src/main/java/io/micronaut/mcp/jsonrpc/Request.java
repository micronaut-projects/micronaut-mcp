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
package io.micronaut.mcp.jsonrpc;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

/**
 * JSON-RPC Request.
 * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC</a>
 *
 * @param jsonrpc Version of the JSON-RPC protocol.
 * @param method  A String containing the name of the method to be invoked.
 * @param params  A Structured value that holds the parameter values to be used during the invocation of the method.
 * @param id      An identifier established by the Client that MUST contain a String, Number, or NULL value if included. If it is not included it is assumed to be a notification. The value SHOULD normally not be Null and Numbers SHOULD NOT contain fractional parts.
 * @param <P>     The type of the params field.
 * @param <I>     The type of the id field (String, Number, or null).
 */
@Serdeable
public record Request<P, I>(
    String jsonrpc,
    String method,
    @Nullable P params,
    @Nullable I id
) {
    public static final String VERSION_2_0 = "2.0";

    public Request(String method, P params, I id) {
        this(VERSION_2_0, method, params, id);
    }
}
