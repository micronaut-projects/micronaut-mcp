package io.micronaut.mcp.server.context;

import io.micronaut.http.HttpRequest;
import io.modelcontextprotocol.common.McpTransportContext;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DefaultMcpTransportContextExtractorTest {

    @Test
    void theExtractedContextResolvesWithTheResolversOnlyWhenAsked() {
        HttpRequestMcpTransportContextTest.CountingHostResolver hostResolver =
            new HttpRequestMcpTransportContextTest.CountingHostResolver("http://localhost");
        HttpRequestMcpTransportContextTest.CountingLocaleResolver localeResolver =
            new HttpRequestMcpTransportContextTest.CountingLocaleResolver(Locale.FRENCH);
        DefaultMcpTransportContextExtractor extractor = new DefaultMcpTransportContextExtractor(hostResolver, localeResolver);

        McpTransportContext extracted = extractor.extract(HttpRequest.GET("/mcp"));

        MicronautMcpTransportContext context = assertInstanceOf(MicronautMcpTransportContext.class, extracted);
        assertEquals(0, hostResolver.resolutions.get());
        assertEquals(0, localeResolver.resolutions.get());
        assertEquals("http://localhost", context.host());
        assertEquals(Locale.FRENCH, context.locale());
        assertEquals(1, hostResolver.resolutions.get());
        assertEquals(1, localeResolver.resolutions.get());
    }
}
