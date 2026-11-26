package io.micronaut.mcp.server.registry;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Executable;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Prototype;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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
    void theTransportContextIsResolvedFromItsBoundIndex() {
        AbstractMcpMethodRegistry.Method<Greeter> method = method("contextual");
        assertEquals(true, method.invoke(registry(null), "request", CONTEXT, "request"));
    }

    @Test
    void aMissingBoundValueIsBoundByTheArgumentBinder() {
        AbstractMcpMethodRegistry.Method<Greeter> method = method("contextual");
        ArgumentBinder<Object, String> binder = (context, source) -> () -> Optional.of(CONTEXT);
        assertEquals(true, method.invoke(registry(binder), "request", null, "request"));
    }

    @Test
    void argumentBindersAreResolvedOnceForEveryParameter() {
        AbstractMcpMethodRegistry.Method<Greeter> method = method("sum");
        AtomicInteger lookups = new AtomicInteger();
        ArgumentBinder<Object, String> binder = (context, source) -> () -> Optional.of(1);
        ArgumentBinderRegistry<String> registry = new ArgumentBinderRegistry<>() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Optional<ArgumentBinder<T, String>> findArgumentBinder(Argument<T> argument) {
                lookups.incrementAndGet();
                return Optional.of((ArgumentBinder<T, String>) (ArgumentBinder<?, String>) binder);
            }
        };
        assertEquals(2, method.invoke(registry, "request", CONTEXT, "request"));
        assertEquals(2, method.invoke(registry, "request", CONTEXT, "request"));
        assertEquals(2, lookups.get());
    }

    @Test
    void aMissingBinderIsLookedUpAgainOnTheNextInvocation() {
        AbstractMcpMethodRegistry.Method<Greeter> method = method("required");
        AtomicReference<ArgumentBinder<Object, String>> binder = new AtomicReference<>();
        ArgumentBinderRegistry<String> registry = mutableRegistry(binder);
        assertThrows(UnsatisfiedArgumentException.class, () -> method.invoke(registry, "request", CONTEXT, "request"));
        // for example a binder for a JSON schema type, registered when the specifications are built
        binder.set((context, source) -> () -> Optional.of(5));
        assertEquals("5", method.invoke(registry, "request", CONTEXT, "request"));
    }

    @Test
    void aBoundValueThatIsNotAnInstanceOfTheParameterTypeIsBoundByTheArgumentBinder() {
        Class<?>[] boundParameterTypes = {McpTransportContext.class, CharSequence.class};
        AbstractMcpMethodRegistry.Method<Greeter> method = method(Greeter.class, "echo", boundParameterTypes);
        ArgumentBinder<Object, String> binder = (context, source) -> () -> Optional.of("from the binder");
        // a String parameter receives a bound CharSequence value only when the value is a String
        assertEquals("bound", method.invoke(registry(binder), "request", CONTEXT, "bound"));
        assertEquals("from the binder", method.invoke(registry(binder), "request", CONTEXT, new StringBuilder("builder")));
    }

    @Test
    void aPrototypeIsLookedUpForEveryInvocation() {
        AbstractMcpMethodRegistry.Method<Ticket> method = method(Ticket.class, "number", BOUND_PARAMETER_TYPES);
        assertNotSame(method.bean(), method.bean());
    }

    @Test
    void aSingletonIsLookedUpOnceUntilItIsDestroyed() {
        AbstractMcpMethodRegistry.Method<Greeter> method = method("contextual");
        Greeter first = method.bean();
        assertSame(first, method.bean());
        // the registries forget the singleton when the bean context destroys it, so the replacement is invoked
        beanContext.destroyBean(Greeter.class);
        method.forget(beanContext.getBeanDefinition(Greeter.class));
        Greeter second = method.bean();
        assertNotSame(first, second);
        assertSame(beanContext.getBean(Greeter.class), second);
    }

    @Test
    void theFirstBoundParameterTypeIsTheTransportContext() {
        BeanDefinition<Greeter> definition = beanContext.getBeanDefinition(Greeter.class);
        ExecutableMethod<Greeter, Object> executable = executable(definition, "contextual");
        Class<?>[] requestFirst = {String.class, McpTransportContext.class};
        assertThrows(IllegalArgumentException.class,
            () -> new AbstractMcpMethodRegistry.Method<>(beanContext, definition, executable, requestFirst, false));
    }

    @Test
    void aParameterOfAnApplicationInterfaceReceivesACustomContextThatImplementsIt() {
        AbstractMcpMethodRegistry.Method<Greeter> method = method("tenant");
        assertEquals("acme", method.invoke(registry(null), "request", new TenantContext("acme"), "request"));
        // another context is no tenant, so the parameter is left to its binder
        assertThrows(UnsatisfiedArgumentException.class, () -> method.invoke(registry(null), "request", CONTEXT, "request"));
    }

    private AbstractMcpMethodRegistry.Method<Greeter> method(String name) {
        return method(Greeter.class, name, BOUND_PARAMETER_TYPES);
    }

    private <B> AbstractMcpMethodRegistry.Method<B> method(Class<B> beanType, String name, Class<?>[] boundParameterTypes) {
        BeanDefinition<B> definition = beanContext.getBeanDefinition(beanType);
        return new AbstractMcpMethodRegistry.Method<>(beanContext, definition, executable(definition, name), boundParameterTypes, false);
    }

    @SuppressWarnings("unchecked")
    private static <B> ExecutableMethod<B, Object> executable(BeanDefinition<B> definition, String name) {
        return (ExecutableMethod<B, Object>) definition.getExecutableMethods().stream()
            .filter(m -> m.getMethodName().equals(name))
            .findFirst()
            .orElseThrow();
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

    private static ArgumentBinderRegistry<String> mutableRegistry(AtomicReference<ArgumentBinder<Object, String>> binder) {
        return new ArgumentBinderRegistry<>() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Optional<ArgumentBinder<T, String>> findArgumentBinder(Argument<T> argument) {
                return Optional.ofNullable((ArgumentBinder<T, String>) (ArgumentBinder<?, String>) binder.get());
            }
        };
    }

    @Requires(property = "spec.name", value = "MethodBindingTest")
    @Prototype
    static class Ticket {
        @Executable
        int number() {
            return 1;
        }
    }

    interface Tenant {
        String tenantId();
    }

    record TenantContext(String tenantId) implements McpTransportContext, Tenant {
        @Override
        public @Nullable Object get(String key) {
            return null;
        }
    }

    @Requires(property = "spec.name", value = "MethodBindingTest")
    @Singleton
    static class Greeter {
        @Executable
        String echo(String text) {
            return text;
        }

        @Executable
        String required(Integer number) {
            return String.valueOf(number);
        }

        @Executable
        String optional(@Nullable Integer number) {
            return number == null ? "absent" : String.valueOf(number);
        }

        @Executable
        int sum(Integer first, Integer second) {
            return first + second;
        }

        @Executable
        boolean contextual(Object context) {
            return context instanceof McpTransportContext;
        }

        @Executable
        String tenant(Tenant tenant) {
            return tenant.tenantId();
        }
    }
}
