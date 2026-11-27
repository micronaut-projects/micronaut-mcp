package io.micronaut.mcp.client.langchain4j.http;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.mcp.conf.client.McpClientHttpConfiguration;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.client.http.micronaut.url", value = "http://localhost:8080/mcp")
@Property(name = "micronaut.mcp.client.http.micronaut.http-client", value = "MICRONAUT")
@MicronautTest(startApplication = false)
class McpClientFactoryMicronautHttpClientTest {
    @Inject
    BeanContext beanContext;

    @Test
    void theConnectionsOfTheMicronautHttpClientNeedTheMicronautHttpClient() {
        McpClientHttpConfiguration configuration = beanContext.getBean(McpClientHttpConfiguration.class, Qualifiers.byName("micronaut"));
        StreamableHttpMcpTransportFactory factory = new StreamableHttpMcpTransportFactory(List.of());
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> factory.createHttpMcpTransport(configuration, beanContext, null));
        assertTrue(error.getMessage().contains("micronaut-http-client"), error.getMessage());
    }
}
