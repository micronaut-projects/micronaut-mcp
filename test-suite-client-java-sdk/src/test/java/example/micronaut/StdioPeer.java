package example.micronaut;

import io.micronaut.context.ApplicationContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;

public final class StdioPeer {
    public static void main(String[] args) {
        var protocolOutput = System.out;
        System.setOut(System.err);
        var context = ApplicationContext.run();
        var mapper = context.getBean(McpJsonMapper.class);
        var transport = new StdioServerTransportProvider(mapper, System.in, protocolOutput);
        McpServer.sync(transport)
            .jsonMapper(mapper)
            .jsonSchemaValidator(context.getBean(JsonSchemaValidator.class))
            .serverInfo("standalone-stdio", "1")
            .build();
    }
}
