package io.micronaut.mcp.client.langchain4j;

import io.micronaut.context.exceptions.BeanInstantiationException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpClientConnectionsTest {

    @Test
    void theReasonIsTheRootCauseOnOneLine() {
        RuntimeException error = new BeanInstantiationException("Error instantiating bean\n\nPath Taken: ...",
            new IllegalStateException("wrapped", new IOException("Code: -32000,\r\nmessage: forged\nline")));
        assertEquals("IOException: Code: -32000, message: forged line", McpClientConnections.reason(error));
    }

    @Test
    void theReasonOfAnErrorWithoutMessageIsItsType() {
        assertEquals("IllegalStateException", McpClientConnections.reason(new IllegalStateException()));
    }

    @Test
    void theReasonIsBounded() {
        assertEquals("IllegalStateException: " + "x".repeat(177), McpClientConnections.reason(new IllegalStateException("x".repeat(500))));
    }
}
