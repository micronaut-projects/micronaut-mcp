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

import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.exceptions.IntrospectionException;
import io.micronaut.core.bind.ArgumentBinderRegistry;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.json.JsonMapper;
import io.micronaut.jsonschema.JsonSchema;
import io.micronaut.jsonschema.utils.JsonSchemaClassPathResourceLoader;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.annotations.ToolArg;
import io.micronaut.mcp.server.exceptions.InputErrorMapper;
import io.micronaut.mcp.server.exceptions.McpErrorExceptionMapper;
import io.micronaut.mcp.server.json.MicronautMcpJsonMapper;
import io.micronaut.mcp.server.observability.McpServerObserver;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.function.Function;

import static io.micronaut.mcp.server.registry.JsonSchemaUtils.TYPE_ARRAY;
import static io.micronaut.mcp.server.registry.JsonSchemaUtils.TYPE_BOOL;
import static io.micronaut.mcp.server.registry.JsonSchemaUtils.TYPE_NULL;
import static io.micronaut.mcp.server.registry.JsonSchemaUtils.TYPE_NUMBER;
import static io.micronaut.mcp.server.registry.JsonSchemaUtils.TYPE_OBJECT;
import static io.micronaut.mcp.server.registry.JsonSchemaUtils.TYPE_STRING;

/**
 * The registry of {@link Tool}s.
 */
@Singleton
@Internal
public final class ToolRegistry extends AbstractMcpMethodRegistry<McpServerFeatures.SyncToolSpecification, McpServerFeatures.AsyncToolSpecification, McpStatelessServerFeatures.SyncToolSpecification, McpStatelessServerFeatures.AsyncToolSpecification> {
    public static final boolean DEFAULT_IDEMPOTENT_HINT_VALUE = false;
    public static final boolean DEFAULT_OPEN_WORLD_HINT_VALUE = true;
    public static final boolean DEFAULT_RETURN_DIRECT_VALUE = false;
    private static final Logger LOG = LoggerFactory.getLogger(ToolRegistry.class);
    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("\\p{Cntrl}");
    private static final Class<?>[] BOUND_PARAMETER_TYPES = {McpTransportContext.class, McpSchema.CallToolRequest.class};
    private static final Argument<Map<String, Object>> JSON_OBJECT_ARGUMENT = Argument.mapOf(String.class, Object.class);
    private static final String KEY_SCHEMA = "$schema";
    private static final String MEMBER_ANNOTATIONS = "annotations";
    private static final String MEMBER_READ_ONLY_HINT = "readOnlyHint";
    private static final String MEMBER_DESTRUCTIVE_HINT = "destructiveHint";
    private static final String MEMBER_IDEMPOTENT_HINT = "idempotentHint";
    private static final String MEMBER_OPEN_WORLD_HINT = "openWorldHint";
    private static final String METHOD_RETURN_DIRECT = "returnDirect";
    private static final String DEFAULT_TOOL_ANNOTATION_MEMBER_TITLE_VALUE = "";
    private static final boolean DEFAULT_MEMBER_READ_ONLY_HINT_VALUE = false;
    private static final boolean DEFAULT_MEMBER_DESTRUCTIVE_HINT_VALUE = true;
    private static final McpSchema.ToolAnnotations DEFAULT_TOOL_ANNOTATIONS = new McpSchema.ToolAnnotations(
        DEFAULT_TOOL_ANNOTATION_MEMBER_TITLE_VALUE,
        DEFAULT_MEMBER_READ_ONLY_HINT_VALUE,
        DEFAULT_MEMBER_DESTRUCTIVE_HINT_VALUE,
        DEFAULT_IDEMPOTENT_HINT_VALUE,
        DEFAULT_OPEN_WORLD_HINT_VALUE,
        DEFAULT_RETURN_DIRECT_VALUE
    );
    private final JsonSchemaClassPathResourceLoader jsonSchemaClassPathResourceLoader;
    private final McpJsonMapper mcpJsonMapper;
    private final JsonMapper jsonMapper;
    private final boolean convertStructuredContent;
    private final ArgumentBinderRegistry<McpSchema.CallToolRequest> argumentBinderRegistry;

    ToolRegistry(JsonSchemaClassPathResourceLoader jsonSchemaClassPathResourceLoader,
                 List<McpErrorExceptionMapper<? extends Throwable>> exceptionMappers,
                 McpJsonMapper mcpJsonMapper,
                 JsonMapper jsonMapper,
                 ArgumentBinderRegistry<McpSchema.CallToolRequest> argumentBinderRegistry,
                 BeanContext beanContext) {
        super(exceptionMappers, beanContext);
        this.jsonSchemaClassPathResourceLoader = jsonSchemaClassPathResourceLoader;
        this.mcpJsonMapper = mcpJsonMapper;
        this.jsonMapper = jsonMapper;
        // When the SDK uses a JSON mapper other than Micronaut's (e.g. the SDK's own Jackson mapper replacing the bean), the
        // result is converted to a Map with the Micronaut mapper, so that the structured content on the wire matches the JSON
        // that is validated and sent as text content, and no reflective serialization of user types is needed in a native image
        this.convertStructuredContent = !(mcpJsonMapper instanceof MicronautMcpJsonMapper);
        this.argumentBinderRegistry = argumentBinderRegistry;
    }

    @Override
    protected Class<?>[] boundParameterTypes() {
        return BOUND_PARAMETER_TYPES;
    }

    @Override
    public List<McpServerFeatures.SyncToolSpecification> getSyncSpecs() {
        return streamMethods()
            .map(toolMethod -> {
                McpSchema.Tool tool = tool(toolMethod);
                return McpServerFeatures.SyncToolSpecification.builder()
                    .tool(tool)
                    .callHandler(provideSyncCallHandler(toolMethod, tool))
                    .build();
            })
            .toList();
    }

    @Override
    public List<McpServerFeatures.AsyncToolSpecification> getAsyncSpecs() {
        return streamMethods()
            .map(toolMethod -> {
                McpSchema.Tool tool = tool(toolMethod);
                return McpServerFeatures.AsyncToolSpecification.builder()
                    .tool(tool)
                    .callHandler(provideReactiveCallHandler(toolMethod, tool))
                    .build();
            })
            .toList();
    }

    @Override
    public List<McpStatelessServerFeatures.SyncToolSpecification> getStatelessSyncSpecs() {
        return streamMethods()
            .map(toolMethod -> {
                McpSchema.Tool tool = tool(toolMethod);
                return McpStatelessServerFeatures.SyncToolSpecification.builder()
                    .tool(tool)
                    .callHandler(provideSyncCallHandler(toolMethod, tool))
                    .build();
            })
            .toList();
    }

    @Override
    public List<McpStatelessServerFeatures.AsyncToolSpecification> getStatelessAsyncSpecs() {
        return streamMethods()
            .map(toolMethod -> {
                McpSchema.Tool tool = tool(toolMethod);
                return McpStatelessServerFeatures.AsyncToolSpecification.builder()
                    .tool(tool)
                    .callHandler(provideReactiveCallHandler(toolMethod, tool))
                    .build();
            })
            .toList();
    }

    private static boolean hasOutputSchema(McpSchema.Tool tool) {
        return CollectionUtils.isNotEmpty(tool.outputSchema());
    }

    private <B, C> BiFunction<C, McpSchema.CallToolRequest, McpSchema.CallToolResult> provideSyncCallHandler(Method<B> toolMethod,
                                                                                                             McpSchema.Tool tool) {
        boolean structuredOutput = hasOutputSchema(tool);
        return (mcpTransportContext, callToolRequest)
            -> callToolToResult(toolMethod, structuredOutput, mcpTransportContext, callToolRequest);
    }

    private <B, C> BiFunction<C, McpSchema.CallToolRequest, Mono<McpSchema.CallToolResult>> provideReactiveCallHandler(Method<B> toolMethod,
                                                                                                                       McpSchema.Tool tool) {
        boolean structuredOutput = hasOutputSchema(tool);
        return (mcpTransportContext, callToolRequest)
            -> reactiveCallToolToResult(toolMethod, structuredOutput, mcpTransportContext, callToolRequest);
    }

    private <B> Mono<McpSchema.CallToolResult> reactiveCallToolToResult(Method<B> m,
                                                                        boolean structuredOutput,
                                                                        Object mcpTransportContext,
                                                                        McpSchema.CallToolRequest callToolRequest) {
        return observedAsync(McpSchema.METHOD_TOOLS_CALL, callToolRequest.name(), () -> m.callAsync(() -> m.invoke(argumentBinderRegistry, callToolRequest, mcpTransportContext, callToolRequest),
            result -> toCallToolResult(m, structuredOutput, result), error -> toolExecutionError(m, error)), ToolRegistry::errorType);
    }

    private <B> McpSchema.CallToolResult callToolToResult(Method<B> m,
                                                          boolean structuredOutput,
                                                          Object mcpTransportContext,
                                                          McpSchema.CallToolRequest callToolRequest) {
        return observed(McpSchema.METHOD_TOOLS_CALL, callToolRequest.name(), () -> m.call(() -> m.invoke(argumentBinderRegistry, callToolRequest, mcpTransportContext, callToolRequest),
            mcpTransportContext, result -> toCallToolResult(m, structuredOutput, result), error -> toolExecutionError(m, error)), ToolRegistry::errorType);
    }

    private static @Nullable String errorType(McpSchema.CallToolResult result) {
        return Boolean.TRUE.equals(result.isError()) ? McpServerObserver.TOOL_ERROR : null;
    }

    /**
     * A tool that fails, including when its arguments cannot be bound or are not valid, produces a tool execution error,
     * which the model can see and correct, rather than a protocol error. A tool that throws an {@link McpError}, or an
     * exception that an application {@link McpErrorExceptionMapper} maps, chooses a protocol error.
     *
     * @see <a href="https://modelcontextprotocol.io/specification/2025-11-25/server/tools#error-handling">Tool error handling</a>
     */
    private <B> McpSchema.CallToolResult toolExecutionError(Method<B> m, Throwable error) {
        if (error instanceof McpError mcpError) {
            throw mcpError;
        }
        McpErrorExceptionMapper<? extends Throwable> mapper = getExceptionMapper(error.getClass());
        String name = toolName(m.method());
        String message;
        if (mapper instanceof InputErrorMapper<?> inputErrorMapper) {
            // Only the message is needed, so no McpError, with its stack trace, is built
            String mapped = inputErrorMessage(inputErrorMapper, error);
            // An exception without a message, such as a SerdeException without a cause, has no message to map
            message = mapped != null && !mapped.isBlank() ? mapped : toolErrorMessage(name, error);
            // Classified by the type of the exception, which may also be thrown by the tool body
            if (LOG.isDebugEnabled()) {
                LOG.debug("Tool {} failed with an input error: {}", name, loggable(message), error);
            }
        } else if (mapper != null) {
            // The SDK does not log the protocol errors it sends
            if (LOG.isDebugEnabled()) {
                LOG.debug("Tool {} failed with a mapped protocol error", name, error);
            }
            throw mapException(mapper, error);
        } else {
            message = toolErrorMessage(name, error);
            if (LOG.isWarnEnabled()) {
                LOG.warn("Tool {} failed: {}", name, loggable(message), error);
            }
        }
        return McpSchema.CallToolResult.builder().addTextContent(message).isError(true).build();
    }

    /**
     * A message that may contain the arguments of a call, made safe to log: control characters, such as line breaks,
     * are replaced, so that a caller cannot forge log lines (CWE-117). The text sent to the model is unchanged.
     */
    private static String loggable(@Nullable String message) {
        return message == null ? "null" : CONTROL_CHARACTERS.matcher(message).replaceAll("_");
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> String inputErrorMessage(InputErrorMapper<?> mapper, T error) {
        return ((InputErrorMapper<T>) mapper).message(error);
    }

    /**
     * The text of the tool execution error of a failure without a mapper: its message, or, when it has none, which tool
     * failed and the class of the failure, so that the model is not left with a bare class name.
     */
    private static String toolErrorMessage(String name, Throwable error) {
        String message = error.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        return "Tool " + name + " failed (" + errorMessage(error) + ")";
    }

    private <B> McpSchema.CallToolResult toCallToolResult(Method<B> m, boolean structuredOutput, @Nullable Object result) {
        if (result instanceof McpSchema.CallToolResult callToolResult) {
            return callToolResult;
        }
        try {
            return serialize(m, structuredOutput, result);
        } catch (IOException e) {
            // Reported like an output that does not match the output schema: a tool execution error with the reason
            String name = toolName(m.method());
            String message = "The result of tool " + name + " could not be serialized: " + e.getMessage();
            if (LOG.isWarnEnabled()) {
                LOG.warn("The result of tool {} could not be serialized: {}", name, loggable(e.getMessage()), e);
            }
            return McpSchema.CallToolResult.builder().addTextContent(message).isError(true).build();
        }
    }

    private <B> McpSchema.CallToolResult serialize(Method<B> m, boolean structuredOutput, @Nullable Object result) throws IOException {
        if (structuredOutput) {
            return structuredToolResult(m.method(), result);
        }
        String text = "";
        if (result instanceof CharSequence || result instanceof Enum<?>) {
            text = result.toString();
        } else if (result != null) {
            text = jsonMapper.writeValueAsString(result);
        }
        return McpSchema.CallToolResult.builder().addTextContent(text).isError(false).build();
    }

    private McpSchema.CallToolResult structuredToolResult(ExecutableMethod<?, ?> method, @Nullable Object result) throws IOException {
        if (result == null) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Tool {} returned null but declares an output schema; returning an error result to the client", toolName(method));
            }
            return toolError("Tool " + toolName(method) + " produced no result");
        }
        // The SDK validates the object against the output schema and adds its JSON as text content. A result that cannot be
        // converted is reported by the caller like a text result that cannot be serialized
        Object structuredContent = convertStructuredContent
            ? jsonMapper.readValueFromTree(jsonMapper.writeValueToTree(result), JSON_OBJECT_ARGUMENT)
            : result;
        return McpSchema.CallToolResult.builder()
            .structuredContent(structuredContent)
            .isError(false)
            .build();
    }

    private static McpSchema.CallToolResult toolError(String reason) {
        return McpSchema.CallToolResult.builder()
            .addTextContent(reason + ". This is a server-side error; retrying with the same arguments will not help.")
            .isError(true)
            .build();
    }

    private <B> McpSchema.Tool tool(Method<B> toolMethod) {
        ExecutableMethod<B, Object> method = toolMethod.method();
        McpSchema.Tool.Builder toolBuilder = McpSchema.Tool.builder()
            .name(toolName(method));
        toolTitle(method).ifPresent(toolBuilder::title);
        toolDescription(method).ifPresent(toolBuilder::description);
        toolAnnotations(method).ifPresent(toolBuilder::annotations);
        Optional<Argument<?>> jsonSchemaArgumentOptional = jsonSchemaArgument(method);
        String jsonSchema = jsonSchemaArgumentOptional.flatMap(this::jsonSchema).orElse(null);
        if (jsonSchema != null) {
            toolBuilder.inputSchema(mcpJsonMapper, jsonSchema);
            jsonSchemaArgumentOptional.ifPresent(argument ->
                argumentBinderRegistry.addArgumentBinder(new JsonMapperTypedCallToRequestArgumentBinder(argument.getType(), jsonMapper)));
        } else {
            toolBuilder.inputSchema(inputSchema(toolMethod, ToolRegistry::toolArgumentName, ToolRegistry::toolArgDescription));
        }
        toolOutputSchema(method).ifPresent(schema -> toolBuilder.outputSchema(mcpJsonMapper, schema));
        toolBuilder.icons(icons(method));
        toolBuilder.meta(meta(method));
        return toolBuilder.build();
    }

    private Optional<String> toolOutputSchema(ExecutableMethod<?, ?> method) {
        return jsonSchemaClassPathResourceLoader.jsonSchemaStringForClass(resultArgument(method).getType());
    }

    private static String toolArgumentName(Argument<?> argument) {
        return argument.findAnnotation(ToolArg.class)
            .flatMap(annValue -> annValue.stringValue("name"))
            .filter(name -> !name.equals(ToolArg.ELEMENT_NAME))
            .orElseGet(argument::getName);
    }

    private static Optional<String> toolTitle(ExecutableMethod<?, ?> method) {
        return method.stringValue(Tool.class, MEMBER_TITLE);
    }

    private static Optional<McpSchema.ToolAnnotations> toolAnnotations(ExecutableMethod<?, ?> method) {
        return method.findAnnotation(Tool.class)
            .flatMap(toolAnnotationValue -> toolAnnotationValue.getAnnotation(MEMBER_ANNOTATIONS))
            .map(annotationAnnotationValue -> new McpSchema.ToolAnnotations(
                annotationAnnotationValue.stringValue(MEMBER_TITLE).orElse(DEFAULT_TOOL_ANNOTATION_MEMBER_TITLE_VALUE),
                    annotationAnnotationValue.booleanValue(MEMBER_READ_ONLY_HINT).orElse(DEFAULT_MEMBER_READ_ONLY_HINT_VALUE),
                    annotationAnnotationValue.booleanValue(MEMBER_DESTRUCTIVE_HINT).orElse(DEFAULT_MEMBER_DESTRUCTIVE_HINT_VALUE),
                    annotationAnnotationValue.booleanValue(MEMBER_IDEMPOTENT_HINT).orElse(DEFAULT_IDEMPOTENT_HINT_VALUE),
                    annotationAnnotationValue.booleanValue(MEMBER_OPEN_WORLD_HINT).orElse(DEFAULT_OPEN_WORLD_HINT_VALUE),
                    annotationAnnotationValue.booleanValue(METHOD_RETURN_DIRECT).orElse(DEFAULT_RETURN_DIRECT_VALUE)
            )).filter(toolAnnotations -> !toolAnnotations.equals(DEFAULT_TOOL_ANNOTATIONS));
    }

    private static Optional<String> toolDescription(ExecutableMethod<?, ?> method) {
        return method.stringValue(Tool.class, MEMBER_DESCRIPTION);
    }

    @Nullable
    private static String toolArgDescription(Argument<?> argument) {
        return argument.getAnnotationMetadata().stringValue(ToolArg.class, MEMBER_DESCRIPTION)
            .filter(desc -> !desc.isEmpty())
            .orElse(null);
    }

    @Override
    protected String primitiveName(ExecutableMethod<?, ?> method) {
        return toolName(method);
    }

    private static String toolName(ExecutableMethod<?, ?> method) {
        String name = method.stringValue(Tool.class, "name").orElse(Tool.ELEMENT_NAME);
        if (name.equals(Tool.ELEMENT_NAME)) {
            return method.getName();
        }
        return name;
    }

    @NonNull
    private Optional<Argument<?>> jsonSchemaArgument(@NonNull ExecutableMethod<?, ?> method) {
        Argument<?>[] arguments = method.getArguments();
        for (Argument<?> arg : arguments) {
            if (arg.getAnnotationMetadata().hasAnnotation(ToolArg.class)) {
                // A named property of the input, whatever its type
                continue;
            }
            try {
                BeanIntrospection<?> introspection = BeanIntrospection.getIntrospection(arg.getType());
                if (introspection.findAnnotation(JsonSchema.class).isPresent()) {
                    return Optional.of(arg);
                }
            } catch (IntrospectionException ex) {

            }
            if (arg.findAnnotation(JsonSchema.class).isPresent()) {
                return Optional.of(arg);
            }
        }
        return Optional.empty();
    }

    private static Object argumentJsonSchema(Argument<?> argument,
                                             Function<Argument<?>, String> argumentDescription) {
        String description = argumentDescription.apply(argument);

        if (description != null) {
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put(KEY_TYPE, argumentType(argument));
            if (StringUtils.isNotEmpty(description)) {
                schema.put(MEMBER_DESCRIPTION, description);
            }
            return schema;
        }

        return new McpSchema.JsonSchema(argumentType(argument), null, null, null, null, null);
    }

    /**
     * The schema of a named property of the tool input: the JSON Schema of its type when there is one, so that the client
     * sees its fields, else a schema with the JSON type of the argument.
     */
    private Object propertyJsonSchema(Argument<?> argument, Function<Argument<?>, String> argumentDescription) {
        if (!TYPE_OBJECT.equals(argumentType(argument))) {
            // Only an object can have a JSON Schema of its own, and looking one up is costly for any other type
            return argumentJsonSchema(argument, argumentDescription);
        }
        return jsonSchema(argument)
            .<Object>map(json -> embeddedJsonSchema(json, argumentDescription.apply(argument)))
            .orElseGet(() -> argumentJsonSchema(argument, argumentDescription));
    }

    private Map<String, Object> embeddedJsonSchema(String json, @Nullable String description) {
        Map<String, Object> schema;
        try {
            schema = new LinkedHashMap<>(jsonMapper.readValue(json, JSON_OBJECT_ARGUMENT));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // The identifier is kept: it makes the embedded schema a schema resource of its own, so that the references and
        // anchors inside it resolve within it and not against the tool input schema. The dialect is the one of the tool
        // input schema.
        schema.remove(KEY_SCHEMA);
        if (StringUtils.isNotEmpty(description)) {
            schema.put(MEMBER_DESCRIPTION, description);
        }
        return schema;
    }

    @NonNull
    private Optional<String> jsonSchema(Argument<?> argument) {
        Class<?> argumentClass = argument.getType();
        return jsonSchemaClassPathResourceLoader.jsonSchemaStringForClass(argumentClass);
    }

    private <B> McpSchema.JsonSchema inputSchema(Method<B> toolMethod,
                                                 Function<Argument<?>, String> propertyNameFunction,
                                                 Function<Argument<?>, String> argumentDescription) {
        Argument<?>[] arguments = toolMethod.method().getArguments();
        Map<String, Object> properties = CollectionUtils.newHashMap(arguments.length);
        List<String> requiredProperties = new ArrayList<>(arguments.length);
        for (int i = 0; i < arguments.length; i++) {
            if (toolMethod.isBound(i)) {
                continue;
            }
            Argument<?> argument = arguments[i];
            String propertyName = propertyNameFunction.apply(argument);
            properties.put(propertyName, propertyJsonSchema(argument, argumentDescription));
            if (isArgumentRequired(argument)) {
                requiredProperties.add(propertyName);
            }
        }
        return new McpSchema.JsonSchema(TYPE_OBJECT, properties, requiredProperties, null, null, null);
    }

    private static String argumentType(Argument<?> argument) {
        Class<?> type = wrapPrimitive(argument.getType());
        if (type == null || Void.class.equals(type)) {
            return TYPE_NULL;
        }
        if (Boolean.class.equals(type)) {
            return TYPE_BOOL;
        }
        if (CharSequence.class.isAssignableFrom(type)
            || Character.class.equals(type)
            || type.isEnum()) {
            return TYPE_STRING;
        }
        if (Number.class.isAssignableFrom(type)) {
            return TYPE_NUMBER;
        }
        if (type.isArray() || Collection.class.isAssignableFrom(type)) {
            return TYPE_ARRAY;
        }
        return TYPE_OBJECT;
    }

    private static Class<?> wrapPrimitive(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        return switch (type.getName()) {
            case "boolean" -> Boolean.class;
            case "byte"    -> Byte.class;
            case "short"   -> Short.class;
            case "int"     -> Integer.class;
            case "long"    -> Long.class;
            case "float"   -> Float.class;
            case "double"  -> Double.class;
            case "char"    -> Character.class;
            default        -> type;
        };
    }

    private static boolean isArgumentRequired(Argument<?> argument) {
        return !argument.isNullable();
    }
}
