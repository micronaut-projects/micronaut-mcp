package io.micronaut.mcp.server.registry;

import io.micronaut.http.uri.UriMatchTemplate;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UriTemplateReadResourceRequestTest {
    @Test
    void arguments() {
        Map<String, Object> m = UriTemplateReadResourceRequest.arguments("pgn://round/{round}/pgn", "pgn://round/14/pgn");
        assertEquals(Map.of("round", "14"), m);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "book://book/A%20Tale%20of%20Two%20Cities | A Tale of Two Cities",
        "book://book/C%2B%2B%20in%20100%25%20detail | C++ in 100% detail",
        "book://book/Caf%C3%A9 | Café",
        "book://book/Café%20au%20lait | Café au lait",
        // Decoded once: an escaped % followed by hexadecimal digits is not decoded again, so %252F is not a /
        "book://book/%2520 | %20",
        "book://book/%252F | %2F"
    })
    void variablesArePercentDecoded(String uri, String title) {
        assertEquals(Map.of("title", title), UriTemplateReadResourceRequest.arguments("book://book/{title}", uri));
    }

    @Test
    void onlyTheVariablesWithAnEscapeAreDecoded() {
        assertEquals(Map.of("shelf", "classics", "title", "A Tale"),
            UriTemplateReadResourceRequest.arguments("book://{shelf}/{title}", "book://classics/A%20Tale"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"book://book/%zz", "book://book/%E", "book://book/title%",
        // Malformed UTF-8: an invalid byte, a truncated sequence and an overlong /
        "book://book/%FF", "book://book/%C3", "book://book/%C0%AF",
        // Arabic-Indic digits, which Character.digit accepts as hexadecimal
        "book://book/%\u0663\u0663"})
    void malformedEscapesAreInvalidParams(String uri) {
        McpError error = assertThrows(McpError.class, () -> UriTemplateReadResourceRequest.arguments("book://book/{title}", uri));
        assertEquals(McpSchema.ErrorCodes.INVALID_PARAMS, error.getJsonRpcError().code());
        assertEquals("Invalid percent-encoding in the value of URI template variable 'title': escapes must be %XX hexadecimal pairs encoding UTF-8",
            error.getMessage());
    }

    // A JSON string can carry an unpaired surrogate, with or without an escape next to it
    @ParameterizedTest
    @ValueSource(strings = {"book://book/\uD800", "book://book/\uDC00x", "book://book/\uD800%20"})
    void anUnpairedSurrogateIsInvalidParams(String uri) {
        McpError error = assertThrows(McpError.class, () -> UriTemplateReadResourceRequest.arguments("book://book/{title}", uri));
        assertEquals(McpSchema.ErrorCodes.INVALID_PARAMS, error.getJsonRpcError().code());
        assertEquals("The value of URI template variable 'title' contains an unpaired surrogate", error.getMessage());
    }

    @Test
    void aSurrogatePairIsBound() {
        assertEquals(Map.of("title", "\uD83D\uDE00"), UriTemplateReadResourceRequest.arguments("book://book/{title}", "book://book/\uD83D\uDE00"));
        assertEquals(Map.of("title", "\uD83D\uDE00"), UriTemplateReadResourceRequest.arguments("book://book/{title}", "book://book/%F0%9F%98%80"));
    }

    // The pre-check only rejects what UriMatchTemplate cannot match: this pins that it does not match these characters
    @ParameterizedTest
    @ValueSource(chars = {'#', '(', '!', '{', ')', '&', ';', '+'})
    void aCharacterThatASimpleVariableDoesNotMatchIsRejectedWithoutMatching(char c) {
        for (String template : new String[] {"book://book/{title}", "log://{year}-{month}-{day}"}) {
            String uri = template.startsWith("book") ? "book://book/a" + c + "b" : "log://2024-01-0" + c + "2";
            assertTrue(UriMatchTemplate.of(template).match(uri).isEmpty(), uri);
            McpError error = assertThrows(McpError.class, () -> UriTemplateReadResourceRequest.arguments(template, uri));
            assertTrue(error.getMessage().startsWith("The URI does not match the URI template " + template), error.getMessage());
        }
    }

    // UriMatchTemplate takes seconds to fail on such URIs, as it backtracks over every split point. A query lets the
    // trailing literal of the template follow the ?, which the SDK routes and UriMatchTemplate drops.
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "log://{year}-{month}-{day} | +",
        "log://{year}-{month}-{day}.log | ?.log",
        "pair://{a}-{b}.json | ?.json"
    })
    void aLongUriThatDoesNotMatchIsRejectedQuickly(String template, String suffix) {
        String uri = template.substring(0, template.indexOf("://") + 3) + "-".repeat(2000) + suffix;
        assertTimeout(Duration.ofSeconds(1), () -> assertThrows(McpError.class, () -> UriTemplateReadResourceRequest.arguments(template, uri)));
    }

    // The streaming policy matches the URI of a request against the templates of primitives that may notify, before
    // the request is dispatched, with the same guard
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "log://{year}-{month}-{day} | +",
        "log://{year}-{month}-{day}.log | ?.log",
        "pair://{a}-{b}.json | ?.json"
    })
    void aLongUriThatDoesNotMatchIsNotMatchedSlowly(String template, String suffix) {
        String uri = template.substring(0, template.indexOf("://") + 3) + "-".repeat(2000) + suffix;
        UriTemplateReadResourceRequest.Template compiled = UriTemplateReadResourceRequest.Template.compile(template);
        assertTimeout(Duration.ofSeconds(1), () -> assertFalse(UriTemplateReadResourceRequest.matches(compiled, uri)));
        assertTrue(UriTemplateReadResourceRequest.matches(UriTemplateReadResourceRequest.Template.compile("log://{year}-{month}-{day}"),
            "log://2026-11-26"));
    }

    @Test
    void aQueryIsInvalidParamsForATemplateOfSimpleVariables() {
        // UriMatchTemplate drops the query, so it would bind a
        assertTrue(UriMatchTemplate.of("book://book/{title}").match("book://book/a?b").isPresent());
        McpError error = assertThrows(McpError.class, () -> UriTemplateReadResourceRequest.arguments("book://book/{title}", "book://book/a?b"));
        assertTrue(error.getMessage().startsWith("The URI does not match the URI template book://book/{title}"), error.getMessage());
        assertEquals(Map.of("title", "a?b"), UriTemplateReadResourceRequest.arguments("book://book/{title}", "book://book/a%3Fb"));
        // A template that is not pre-checked, such as one with a reserved variable, still drops the query
        assertEquals(Map.of("title", "secret"), UriTemplateReadResourceRequest.arguments("book://book/{+title}", "book://book/secret?x"));
    }

    @Test
    void onlyTemplatesOfSimpleVariablesArePreChecked() {
        assertTrue(UriTemplateReadResourceRequest.Template.compile("book://book/{title}").simpleVariablesOnly());
        assertTrue(UriTemplateReadResourceRequest.Template.compile("log://{year}-{month}-{day}").simpleVariablesOnly());
        assertTrue(UriTemplateReadResourceRequest.Template.compile("x://{a,b}").simpleVariablesOnly());
        for (String template : new String[] {"file:///{+path}", "x://{id:[0-9]+}", "x://{id:3}", "search://books{?q}", "x://a(b)/{id}", "x://a?b={id}"}) {
            assertFalse(UriTemplateReadResourceRequest.Template.compile(template).simpleVariablesOnly(), template);
        }
        // A literal ( in the template is matched by UriMatchTemplate
        assertEquals(Map.of("id", "7"), UriTemplateReadResourceRequest.arguments("x://a(b)/{id}", "x://a(b)/7"));
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
    void anEncodedSlashIsAllowedOnlyInTheReservedVariablesOfATemplate() {
        assertEquals(Map.of("a", "p/q", "b", "r"), UriTemplateReadResourceRequest.arguments("x://{+a}/{b}", "x://p%2Fq/r"));
        McpError error = assertThrows(McpError.class, () -> UriTemplateReadResourceRequest.arguments("x://{+a}/{b}", "x://p%2Fq/r%2Fs"));
        assertEquals("The value of URI template variable 'b' must not contain an encoded / (%2F)", error.getMessage());
    }

    @Test
    void queryVariablesAreNotBound() {
        // UriMatchTemplate matches the path but binds no query ({?q}) variable, so there is nothing to decode
        assertEquals(Map.of(), UriTemplateReadResourceRequest.arguments("book://books{?q}", "book://books?q=a%2Fb"));
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

    // The SDK routes these URIs to the template, but a simple variable does not match a literal +, ;, &, !, ( or )
    @ParameterizedTest
    @ValueSource(strings = {"book://book/C++%20notes", "book://book/a;b", "book://book/a&b", "book://book/a!b", "book://book/a(b)"})
    void aUriThatDoesNotMatchTheTemplateIsInvalidParams(String uri) {
        McpError error = assertThrows(McpError.class, () -> UriTemplateReadResourceRequest.arguments("book://book/{title}", uri));
        assertEquals(McpSchema.ErrorCodes.INVALID_PARAMS, error.getJsonRpcError().code());
        assertEquals("The URI does not match the URI template book://book/{title}: percent-encode reserved characters such as ?, +, ;, &, !, ( and ) in variable values, for example + as %2B",
            error.getMessage());
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
