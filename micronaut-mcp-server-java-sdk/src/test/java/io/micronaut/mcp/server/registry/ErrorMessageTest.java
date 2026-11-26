package io.micronaut.mcp.server.registry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorMessageTest {

    @Test
    void theMessageOfAFailure() {
        assertEquals("the backend is down", AbstractMcpMethodRegistry.errorMessage(new IllegalStateException("the backend is down")));
    }

    @Test
    void aFailureWithoutAMessageIsDescribedByItsSimpleName() {
        assertEquals("IllegalStateException", AbstractMcpMethodRegistry.errorMessage(new IllegalStateException()));
        assertEquals("IllegalStateException", AbstractMcpMethodRegistry.errorMessage(new IllegalStateException("  ")));
    }

    @Test
    void anAnonymousFailureWithoutAMessageIsDescribedByItsName() {
        RuntimeException anonymous = new RuntimeException() {
        };
        assertEquals(anonymous.getClass().getName(), AbstractMcpMethodRegistry.errorMessage(anonymous));
    }
}
