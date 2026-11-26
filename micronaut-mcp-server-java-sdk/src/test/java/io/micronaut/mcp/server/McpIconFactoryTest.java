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
    @ValueSource(strings = {"https://example.com/sun.svg", "HTTPS://example.com/sun.svg", "http://localhost:8080/sun.svg", "data:image/png;base64,iVBORw0KGgo="})
    void httpsHttpAndDataSourcesAreAccepted(String src) {
        assertEquals(src, McpIconFactory.icon(OWNER, src, null, List.of(), null).src());
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "file:///etc/passwd", "ftp://example.com/sun.svg", "example.com/sun.svg", "httpx://example.com"})
    void otherSchemesAreRejected(String src) {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> McpIconFactory.icon(OWNER, src, null, List.of(), null));
        assertEquals("The icon has the src " + src + ", but it must be an https or http URL, or a data: URI", error.getMessage());
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
