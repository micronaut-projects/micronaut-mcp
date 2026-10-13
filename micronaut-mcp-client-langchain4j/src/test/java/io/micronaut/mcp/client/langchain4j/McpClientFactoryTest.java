package io.micronaut.mcp.client.langchain4j;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.mcp.conf.client.McpClientHttpConfiguration;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.client.http.micronaut.url", value = "http://localhost:8080/mcp")
@Property(name = "micronaut.mcp.client.http.micronaut.http-client", value = "MICRONAUT")
@MicronautTest(startApplication = false)
class McpClientFactoryTest {
    @Inject
    BeanContext beanContext;

    @Test
    void theConnectionsOfTheMicronautHttpClientNeedTheMicronautHttpClient() {
        McpClientHttpConfiguration configuration = beanContext.getBean(McpClientHttpConfiguration.class, Qualifiers.byName("micronaut"));
        McpClientFactory factory = new McpClientFactory();
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> factory.createMcpClient(configuration, beanContext, null));
        assertTrue(error.getMessage().contains("micronaut-http-client"), error.getMessage());
    }
}
