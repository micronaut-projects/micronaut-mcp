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
package io.micronaut.mcp.primitives.utils;

import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to perform string interpolation with maps.
 */
@Internal
public final class StringInterpolator {
    private static final Pattern INTERPOLATION_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    private StringInterpolator() {
    }

    /**
     * If you supplied a template such as `Name: ${firstName} ${lastName}` and map of values such as `[firstName: Sergio, lastName: Amo] it returns `Name: Sergio Amo`.
     * @param template A String template
     * @param values a Map of values
     * @return a itnerpolated string
     */
    @NonNull
    public static String interpolate(@NonNull String template, @NonNull Map<String, Object> values) {
        Matcher matcher = INTERPOLATION_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object defaultValue = matcher.group(0);
            Object value = values.getOrDefault(key, defaultValue);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value.toString()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
