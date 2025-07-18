package io.micronaut.mcp.jsonrpc;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(startApplication = false)
class RequestValidationTest {
    @Test
    void validationConstraints(Validator validator) {
        assertTrue(validator.validate(new Request<>("ping", "123")).isEmpty());
        assertFalse(validator.validate(new Request<>(null, "123")).isEmpty(), "method cannot be null");
        assertFalse(validator.validate(new Request<>("", "123")).isEmpty(), "method cannot be blank");
        assertFalse(validator.validate(new Request<>(null, "ping", null, "123")).isEmpty(), "jsonrpc cannot be null");
        assertFalse(validator.validate(new Request<>("", "ping", null, "123")).isEmpty(), "jsonrpc cannot be blank");
        assertFalse(validator.validate(new Request<>("1.0", "ping", null, "123")).isEmpty(), "jsonrpc must be 2.0");
    }
}
