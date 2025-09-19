/*
 * Copyright 2017-2024 original authors
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
package io.micronaut.mcp.server.json;

import com.networknt.schema.SpecVersion;
import com.networknt.schema.AbsoluteIri;
import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.ExecutionContextCustomizer;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.resource.InputStreamSource;
import com.networknt.schema.resource.SchemaLoader;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.json.JsonMapper;
import io.micronaut.jsonschema.utils.JsonSchemaClassPathResourceLoader;
import io.micronaut.jsonschema.utils.JsonSchemaConfiguration;
import io.micronaut.jsonschema.validation.JsonSchemaValidator;
import io.micronaut.jsonschema.validation.JsonSchemaValidatorConfiguration;
import io.micronaut.jsonschema.validation.ValidationMessage;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import io.micronaut.jsonschema.validation.ValidationMessageAdapter;

@Replaces(JsonSchemaValidator.class)
@Singleton
@Internal
final class JsonSchemaValidatorReplacement implements JsonSchemaValidator {
    private static final Logger LOG = LoggerFactory.getLogger(JsonSchemaValidatorReplacement.class);
    private static final String CLASSPATH_PREFIX = "classpath:";

    private static final ExecutionContextCustomizer CONTEXT_CUSTOMIZER = (executionContext, validationContext) -> {
        // By default, since Draft 2019-09 the format keyword only generates annotations and not assertions
        validationContext.getConfig().setFormatAssertionsEnabled(true);
    };
    private static final String SLASH = "/";
    private static final String META_INF = "META-INF";

    private final Map<Class<?>, JsonSchema> jsonSchemaCache = new ConcurrentHashMap<>();
    private final JsonSchemaValidatorConfiguration config;
    private final ResourceLoader resourceLoader;
    private final JsonMapper jsonMapper;
    private final SchemaValidatorsConfig schemaValidatorsConfig;
    private final JsonSchemaClassPathResourceLoader jsonSchemaClassPathResourceLoader;
    private final JsonSchemaConfiguration jsonSchemaConfiguration;

    JsonSchemaValidatorReplacement(
        JsonSchemaValidatorConfiguration config,
        ResourceLoader resourceLoader,
        JsonMapper jsonMapper,
        SchemaValidatorsConfig schemaValidatorsConfig,
        JsonSchemaClassPathResourceLoader jsonSchemaClassPathResourceLoader,
        JsonSchemaConfiguration jsonSchemaConfiguration
    ) {
        this.config = config;
        this.resourceLoader = resourceLoader;
        this.jsonMapper = jsonMapper;
        this.schemaValidatorsConfig = schemaValidatorsConfig;
        this.jsonSchemaClassPathResourceLoader = jsonSchemaClassPathResourceLoader;
        this.jsonSchemaConfiguration = jsonSchemaConfiguration;
    }

    @Override
    public <T> Set<? extends ValidationMessage> validate(@NonNull String json, @NonNull Class<T> type) {
        JsonSchema schema = jsonSchemaCache.computeIfAbsent(type, this::jsonSchema);
        return validate(schema, json);
    }

    @NonNull
    public Set<? extends ValidationMessage> validate(@NonNull Object value, @NonNull Map<String, Object> schemaMap) throws IOException {
        JsonSchema schema = jsonSchema(schemaMap);
        String json = value instanceof String s ? s : jsonMapper.writeValueAsString(value);
        return validate(schema, json);
    }

    @Override
    @NonNull
    public <T> Set<? extends ValidationMessage> validate(@NonNull Object value, @NonNull Class<T> type) throws IOException {
        JsonSchema schema = jsonSchemaCache.computeIfAbsent(type, this::jsonSchema);
        String json = jsonMapper.writeValueAsString(value);
        return validate(schema, json);
    }

    private <T> JsonSchema jsonSchema(@NonNull Class<T> type) {
        String jsonSchema = jsonSchemaClassPathResourceLoader.jsonSchemaStringForClass(type).orElse(null);
        if (jsonSchema == null) {
            throw new IllegalArgumentException("No schema found for type: " + type);
        }
        return jsonSchema(jsonSchema);
    }

    @NonNull
    private JsonSchema jsonSchema(@NonNull Map<String, Object> jsonSchema) {
        try {
            String jsonSchemaString = jsonMapper.writeValueAsString(jsonSchema);
            return jsonSchema(jsonSchemaString);
        } catch (IOException e) {
            throw new IllegalArgumentException("could not serialize JSON Schema from: " + jsonSchema);
        }
    }

    @NonNull
    private JsonSchema jsonSchema(@NonNull String jsonSchema) {
        JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder -> {
            builder.schemaLoaders(b -> b.add(new JsonSchemaValidatorReplacement.ResourceSchemaLoader()));
        });
        return jsonSchemaFactory.getSchema(jsonSchema, schemaValidatorsConfig);
    }

    private static Set<? extends ValidationMessage> validate(JsonSchema schema, String json) {
        return schema.validate(json, InputFormat.JSON, CONTEXT_CUSTOMIZER)
            .stream()
            .map(ValidationMessageAdapter::new)
            .collect(Collectors.toSet());
    }

    private class ResourceSchemaLoader implements SchemaLoader {
        @Override
        public InputStreamSource getSchema(AbsoluteIri absoluteIri) {
            String path = URI.create(absoluteIri.toString()).toString();
            if (path.startsWith(config.baseUri())) {
                path = path.substring(config.baseUri().length());
            }
            String classpathFolder = META_INF + SLASH + jsonSchemaConfiguration.getOutputLocation() + SLASH;
            String filePath = Path.of(classpathFolder + path).normalize().toString();
            if (!filePath.startsWith(classpathFolder)) {
                throw new IllegalArgumentException("Schema for URI " + absoluteIri + " is not inside the required folder " + config.classpathFolder() + " at path: " + path);
            }
            return () -> resourceLoader.getResourceAsStream(CLASSPATH_PREFIX + filePath)
                .orElseThrow(() -> new IllegalArgumentException("No schema found for uri: " + absoluteIri + " at path: " + filePath));
        }
    }

}

