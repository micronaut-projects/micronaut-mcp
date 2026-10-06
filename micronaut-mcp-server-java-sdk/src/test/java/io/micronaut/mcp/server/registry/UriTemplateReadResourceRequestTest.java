package io.micronaut.mcp.server.registry;

import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UriTemplateReadResourceRequestTest {
    @Test
    void arguments() {
        Map<String, Object> m = UriTemplateReadResourceRequest.arguments("pgn://round/{round}/pgn", "pgn://round/14/pgn");
        assertEquals(Map.of("round", "14"), m);
    }

    @Test
    void variablesArePercentDecoded() {
        assertEquals(Map.of("title", "A Tale of Two Cities"),
            UriTemplateReadResourceRequest.arguments("book://book/{title}", "book://book/A%20Tale%20of%20Two%20Cities"));
        assertEquals(Map.of("title", "C++ in 100% detail"),
            UriTemplateReadResourceRequest.arguments("book://book/{title}", "book://book/C%2B%2B%20in%20100%25%20detail"));
        assertEquals(Map.of("title", "Café"),
            UriTemplateReadResourceRequest.arguments("book://book/{title}", "book://book/Caf%C3%A9"));
    }

    @Test
    void malformedEscapesAreInvalidParams() {
        for (String uri : new String[] {"book://book/%zz", "book://book/%E", "book://book/title%"}) {
            McpError error = assertThrows(McpError.class, () -> UriTemplateReadResourceRequest.arguments("book://book/{title}", uri));
            assertEquals(McpSchema.ErrorCodes.INVALID_PARAMS, error.getJsonRpcError().code());
            assertEquals("Invalid percent-encoding in the value of URI template variable title", error.getMessage());
        }
    }

    @Test
    void anEncodedSlashIsRejectedInASimpleVariable() {
        McpError error = assertThrows(McpError.class,
            () -> UriTemplateReadResourceRequest.arguments("file:///{path}", "file:///..%2F..%2Fetc%2Fpasswd"));
        assertEquals(McpSchema.ErrorCodes.INVALID_PARAMS, error.getJsonRpcError().code());
        assertEquals("The value of URI template variable path must not contain an encoded /", error.getMessage());
    }

    @Test
    void reservedVariablesMayContainASlash() {
        assertEquals(Map.of("path", "docs/read me.txt"),
            UriTemplateReadResourceRequest.arguments("file:///{+path}", "file:///docs%2Fread%20me.txt"));
        assertEquals(Set.of("path", "fragment"), UriTemplateReadResourceRequest.Template.compile("file:///{+path}{#fragment}").reservedVariables());
    }

    @Test
    void requestsMatchedAgainstACompiledTemplate() {
        UriTemplateReadResourceRequest.Template template = UriTemplateReadResourceRequest.Template.compile("pgn://round/{round}/pgn");
        McpSchema.ReadResourceRequest round14 = new McpSchema.ReadResourceRequest("pgn://round/14/pgn");
        McpSchema.ReadResourceRequest round15 = new McpSchema.ReadResourceRequest("pgn://round/15/pgn");

        UriTemplateReadResourceRequest first = UriTemplateReadResourceRequest.of(template, round14);
        UriTemplateReadResourceRequest second = UriTemplateReadResourceRequest.of(template, round15);

        assertEquals(Map.of("round", "14"), first.arguments());
        assertSame(round14, first.request());
        assertEquals(Map.of("round", "15"), second.arguments());
        assertEquals(Map.of(), UriTemplateReadResourceRequest.of(template, new McpSchema.ReadResourceRequest("other://x")).arguments());
    }
}
