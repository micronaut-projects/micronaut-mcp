package io.micronaut.mcp.server.registry;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.bind.ArgumentBinder;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.type.Argument;
import io.micronaut.json.JsonMapper;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Converts the already parsed arguments of a tool call into the type of a {@code @JsonSchema} parameter.
 */
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "JsonMapperTypedCallToRequestArgumentBinderTest")
@MicronautTest(startApplication = false)
class JsonMapperTypedCallToRequestArgumentBinderTest {
    @Inject
    JsonMapper jsonMapper;

    @Test
    void nestedObjectsAndListsAreConverted() {
        Map<String, Object> arguments = Map.of(
            "customer", Map.of("name", "Ada"),
            "lines", List.of(Map.of("product", "pen", "quantity", 2), Map.of("product", "ink", "quantity", 1)),
            "note", "gift");
        assertEquals(new Order(new Customer("Ada"), List.of(new Line("pen", 2), new Line("ink", 1)), "gift"), bind(arguments).get());
    }

    @Test
    void missingPropertiesAreNullAndUnknownPropertiesAreIgnored() {
        Map<String, Object> arguments = Map.of("customer", Map.of("name", "Ada"), "lines", List.of(), "unknown", true);
        assertEquals(new Order(new Customer("Ada"), List.of(), null), bind(arguments).get());
    }

    @Test
    void aValueOfTheWrongTypeIsAConversionErrorNotAnException() {
        Map<String, Object> arguments = Map.of("customer", Map.of("name", "Ada"),
            "lines", List.of(Map.of("product", "pen", "quantity", "two")));
        ArgumentBinder.BindingResult<Order> result = bind(arguments);
        assertFalse(result.isPresentAndSatisfied());
        assertEquals(1, result.getConversionErrors().size());
    }

    @Test
    void theArgumentTypeIsTheParameterType() {
        assertEquals(Argument.of(Order.class), new JsonMapperTypedCallToRequestArgumentBinder<>(Order.class, jsonMapper).argumentType());
        assertTrue(bind(Map.of("customer", Map.of("name", "Ada"), "lines", List.of())).isPresentAndSatisfied());
    }

    private ArgumentBinder.BindingResult<Order> bind(Map<String, Object> arguments) {
        JsonMapperTypedCallToRequestArgumentBinder<Order> binder = new JsonMapperTypedCallToRequestArgumentBinder<>(Order.class, jsonMapper);
        return binder.bind(ConversionContext.of(Argument.of(Order.class, "order")), new McpSchema.CallToolRequest("order", arguments));
    }

    @Serdeable
    record Order(Customer customer, List<Line> lines, @Nullable String note) {
    }

    @Serdeable
    record Customer(String name) {
    }

    @Serdeable
    record Line(String product, int quantity) {
    }
}
