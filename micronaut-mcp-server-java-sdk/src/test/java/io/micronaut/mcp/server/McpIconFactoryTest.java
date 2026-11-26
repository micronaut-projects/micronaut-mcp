package io.micronaut.mcp.server;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpIconFactoryTest {

    private static final String OWNER = "The icon";
    private static final String SRC = "https://example.com/sun.svg";

    @Test
    void keepsEveryDeclaredValue() {
        assertEquals(new McpSchema.Icon(SRC, "image/svg+xml", List.of("48x48", "any"), "dark"),
            McpIconFactory.icon(OWNER, SRC, "image/svg+xml", List.of("48x48", "any"), "dark"));
    }

    @Test
    void leavesOutEmptyValues() {
        assertEquals(new McpSchema.Icon(SRC, null, null, null), McpIconFactory.icon(OWNER, SRC, "", List.of(), ""));
        assertEquals(new McpSchema.Icon(SRC, null, null, null), McpIconFactory.icon(OWNER, SRC, null, List.of(), null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void aBlankSrcIsRejected(String src) {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> McpIconFactory.icon(OWNER, src, null, List.of(), null));
        assertEquals("The icon requires a src", error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com/sun.svg", "HTTPS://example.com/sun.svg", "http://localhost:8080/sun.svg",
        "https://my_host/sun.svg", "data:image/png;base64,iVBORw0KGgo=", "DATA:image/svg+xml,%3Csvg%2F%3E"})
    void httpsHttpAndDataSourcesAreAccepted(String src) {
        assertEquals(src, McpIconFactory.icon(OWNER, src, null, List.of(), null).src());
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "file:///etc/passwd", "ftp://example.com/sun.svg", "example.com/sun.svg",
        "httpx://example.com", "http:sun.svg", "https:///sun.svg", "https://exa mple.com", "data:text/html,<script>x</script>", " data:image/png;base64,AA"})
    void otherSourcesAreRejectedWithoutRepeatingThem(String src) {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> McpIconFactory.icon(OWNER, src, null, List.of(), null));
        assertEquals("The icon has a src that is not an https or http URL, or a data: URI of an image", error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://user:secret@example.com/sun.svg", "http://token@localhost/sun.svg", "https://us_er:secret@my_host/sun.svg"})
    void aSourceWithAUserNameOrPasswordIsRejectedWithoutRepeatingIt(String src) {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> McpIconFactory.icon(OWNER, src, null, List.of(), null));
        assertEquals("The icon has a src with a user name or password, which every client would receive", error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Dark", "LIGHT", "purple", " dark"})
    void aThemeOtherThanLowercaseLightOrDarkIsRejected(String theme) {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> McpIconFactory.icon(OWNER, SRC, null, List.of(), theme));
        assertEquals("The icon has the theme " + theme + ", but it must be light or dark", error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"light", "dark"})
    void lowercaseLightAndDarkAreAccepted(String theme) {
        assertEquals(theme, McpIconFactory.icon(OWNER, SRC, null, List.of(), theme).theme());
    }
}
