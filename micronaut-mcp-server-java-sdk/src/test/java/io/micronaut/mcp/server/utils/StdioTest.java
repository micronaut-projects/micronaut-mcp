package io.micronaut.mcp.server.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StdioTest {

    @Test
    @Timeout(5)
    void incompleteResponseTimesOutWhileTheWriterIsStillOpen() throws IOException {
        try (Stdio stdio = new Stdio()) {
            stdio.serverStdout.write('x');
            stdio.serverStdout.write(0xC3); // First byte of a two-byte UTF-8 character
            assertThrows(AssertionError.class, () -> stdio.readResponse(Duration.ofMillis(50)));
        }
    }

    @Test
    void readsOneUtf8ResponseAtATime() throws IOException, InterruptedException {
        try (Stdio stdio = new Stdio()) {
            stdio.serverStdout.write("café\r\n日本語\n".getBytes(StandardCharsets.UTF_8));
            Duration timeout = Duration.ofSeconds(1);
            assertEquals("café", stdio.readResponse(timeout));
            assertEquals("日本語", stdio.readResponse(timeout));
        }
    }
}
