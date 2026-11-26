package io.micronaut.mcp.server.registry;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Executable;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.bind.ArgumentBinder;
import io.micronaut.core.bind.ArgumentBinderRegistry;
import io.micronaut.core.bind.exceptions.UnsatisfiedArgumentException;
import io.micronaut.core.convert.exceptions.ConversionErrorException;
import io.micronaut.core.type.Argument;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.common.McpTransportContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Binds the parameters of a registry method that receive neither the transport context nor the request, with the
 * semantics of {@link io.micronaut.core.bind.DefaultExecutableBinder}.
 */
@Property(name = "spec.name", value = "MethodBindingTest")
@MicronautTest(startApplication = false)
class MethodBindingTest {
    private static final Class<?>[] BOUND_PARAMETER_TYPES = {McpTransportContext.class, String.class};
    private static final McpTransportContext CONTEXT = McpTransportContext.create(Map.of());

    @Inject
    BeanContext beanContext;

    @Test
    void aParameterWithoutABinderIsUnsatisfied() {
        AbstractMcpMethodRegistry.Method<Greeter> method = method("required");
        assertThrows(UnsatisfiedArgumentException.class, () -> method.invoke(registry(null), "request", CONTEXT, "request"));
    }

    @Test
    void aRejectedValueIsAConversionError() {
        AbstractMcpMethodRegistry.Method<Greeter> method = method("required");
        ArgumentBinder<Object, String> rejecting = (context, source) -> {
            context.reject(source, new IllegalArgumentException("not a number"));
            return ArgumentBinder.BindingResult.unsatisfied();
        };
        assertThrows(ConversionErrorException.class, () -> method.invoke(registry(rejecting), "request", CONTEXT, "request"));
    }

    @Test
    void anUnboundNullableParameterIsNull() {
        AbstractMcpMethodRegistry.Method<Greeter> method = method("optional");
        assertEquals("absent", method.invoke(registry((context, source) -> ArgumentBinder.BindingResult.unsatisfied()), "request", CONTEXT, "request"));
    }

    @Test
    void theContextIsResolvedAndASingletonIsLookedUpOnce() {
        AbstractMcpMethodRegistry.Method<Greeter> method = method("contextual");
        assertEquals(true, method.invoke(registry(null), "request", CONTEXT, "request"));
        assertSame(method.bean(), method.bean());
    }

    @SuppressWarnings("unchecked")
    private AbstractMcpMethodRegistry.Method<Greeter> method(String name) {
        BeanDefinition<Greeter> definition = beanContext.getBeanDefinition(Greeter.class);
        ExecutableMethod<Greeter, Object> executable = (ExecutableMethod<Greeter, Object>) definition.getExecutableMethods().stream()
            .filter(m -> m.getMethodName().equals(name))
            .findFirst()
            .orElseThrow();
        return new AbstractMcpMethodRegistry.Method<>(beanContext, definition, executable, BOUND_PARAMETER_TYPES);
    }

    private static ArgumentBinderRegistry<String> registry(@Nullable ArgumentBinder<Object, String> binder) {
        return new ArgumentBinderRegistry<>() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Optional<ArgumentBinder<T, String>> findArgumentBinder(Argument<T> argument) {
                return Optional.ofNullable((ArgumentBinder<T, String>) (ArgumentBinder<?, String>) binder);
            }
        };
    }

    @Requires(property = "spec.name", value = "MethodBindingTest")
    @Singleton
    static class Greeter {
        @Executable
        String required(Integer number) {
            return String.valueOf(number);
        }

        @Executable
        String optional(@Nullable Integer number) {
            return number == null ? "absent" : String.valueOf(number);
        }

        @Executable
        boolean contextual(Object context) {
            return context instanceof McpTransportContext;
        }
    }
}
