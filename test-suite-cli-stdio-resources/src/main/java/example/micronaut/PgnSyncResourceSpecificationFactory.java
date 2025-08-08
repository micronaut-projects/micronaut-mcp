package example.micronaut;

import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static example.micronaut.FileUtils.text;

@Factory
class PgnSyncResourceSpecificationFactory {
    private static final Logger LOG = LoggerFactory.getLogger(PgnSyncResourceSpecificationFactory.class);

    @Singleton
    @EachBean(McpSchema.Resource.class)
    McpServerFeatures.SyncResourceSpecification createSyncResourceSpecification(McpSchema.Resource resource) {
        return new McpServerFeatures.SyncResourceSpecification(resource, (mcpSyncServerExchange, readResourceRequest) -> {
            String uri = readResourceRequest.uri();
            List<McpSchema.ResourceContents> contents = new ArrayList<>();
            String text = text(uri);
            contents.add(new McpSchema.TextResourceContents(uri, PgnResourceFactory.PGN_MIME_TYPE, text));
            return new McpSchema.ReadResourceResult(contents);
        });
    }
}
