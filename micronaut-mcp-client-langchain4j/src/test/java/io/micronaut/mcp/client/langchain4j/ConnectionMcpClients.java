package io.micronaut.mcp.client.langchain4j;

import io.micronaut.context.BeanContext;
import io.micronaut.inject.qualifiers.Qualifiers;

/**
 * Registers the shared client of a connection, for tests outside this package.
 */
public final class ConnectionMcpClients {
    private ConnectionMcpClients() {
    }

    /**
     * Registers the shared client of a connection, which resolves the {@code McpClient} bean of that name.
     *
     * @param beanContext The bean context
     * @param name The name of the connection
     */
    public static void register(BeanContext beanContext, String name) {
        beanContext.registerSingleton(ConnectionMcpClient.class, new ConnectionMcpClient(name, beanContext), Qualifiers.byName(name));
    }
}
