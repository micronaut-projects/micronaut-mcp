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
        assertEquals(Map.of("title", "Café au lait"),
            UriTemplateReadResourceRequest.arguments("book://book/{title}", "book://book/Café%20au%20lait"));
        assertEquals(Map.of("shelf", "classics", "title", "A Tale"),
            UriTemplateReadResourceRequest.arguments("book://{shelf}/{title}", "book://classics/A%20Tale"));
    }

    @Test
    void malformedEscapesAreInvalidParams() {
        for (String uri : new String[] {"book://book/%zz", "book://book/%E", "book://book/title%",
            // Malformed UTF-8: an invalid byte, a truncated sequence and an overlong /
            "book://book/%FF", "book://book/%C3", "book://book/%C0%AF",
            // Arabic-Indic digits, which Character.digit accepts as hexadecimal
            "book://book/%\u0663\u0663",
            // A lone surrogate, which a JSON string can carry, next to an escape
            "book://book/\uD800%20"}) {
            McpError error = assertThrows(McpError.class, () -> UriTemplateReadResourceRequest.arguments("book://book/{title}", uri));
            assertEquals(McpSchema.ErrorCodes.INVALID_PARAMS, error.getJsonRpcError().code());
            assertEquals("Invalid percent-encoding in the value of URI template variable 'title': escapes must be %XX hexadecimal pairs encoding UTF-8",
                error.getMessage());
        }
    }

    @Test
    void anEncodedSlashIsRejectedInASimpleVariable() {
        McpError error = assertThrows(McpError.class,
            () -> UriTemplateReadResourceRequest.arguments("file:///{path}", "file:///..%2F..%2Fetc%2Fpasswd"));
        assertEquals(McpSchema.ErrorCodes.INVALID_PARAMS, error.getJsonRpcError().code());
        assertEquals("The value of URI template variable 'path' must not contain an encoded / (%2F)", error.getMessage());
    }

    @Test
    void reservedVariablesMayContainASlash() {
        assertEquals(Map.of("path", "docs/read me.txt"),
            UriTemplateReadResourceRequest.arguments("file:///{+path}", "file:///docs%2Fread%20me.txt"));
        // UriMatchTemplate does not match fragment ({#var}) expressions, so they are not reserved
        assertEquals(Set.of("path"), UriTemplateReadResourceRequest.Template.compile("file:///{+path}{#fragment}").reservedVariables());
    }

    @Test
    void reservedVariablesDropPrefixAndExplodeModifiers() {
        assertEquals(Set.of("a", "b", "c"), UriTemplateReadResourceRequest.Template.compile("x://{+a:3,b*}/{+c}").reservedVariables());
    }

    @Test
    void aLiteralPlusIsNotDecodedAsASpace() {
        assertEquals(Map.of("path", "C++ notes.txt"),
            UriTemplateReadResourceRequest.arguments("file:///{+path}", "file:///C++%20notes.txt"));
    }

    @Test
    void aUriThatDoesNotMatchTheTemplateIsInvalidParams() {
        // The SDK routes these URIs to the template, but a simple variable does not match a literal +, ;, &, !, ( or )
        for (String uri : new String[] {"book://book/C++%20notes", "book://book/a;b", "book://book/a&b", "book://book/a!b", "book://book/a(b)"}) {
            McpError error = assertThrows(McpError.class, () -> UriTemplateReadResourceRequest.arguments("book://book/{title}", uri));
            assertEquals(McpSchema.ErrorCodes.INVALID_PARAMS, error.getJsonRpcError().code());
            assertEquals("The URI does not match the URI template book://book/{title}: percent-encode reserved characters such as +, ;, &, !, ( and ) in variable values, for example + as %2B",
                error.getMessage());
        }
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
        McpSchema.ReadResourceRequest other = new McpSchema.ReadResourceRequest("other://x");
        assertThrows(McpError.class, () -> UriTemplateReadResourceRequest.of(template, other));
    }
}
