package io.micronaut.mcp.server.registry;

import java.util.List;

public interface McpPrimitiveRegistry<Sync, Async, StatelessSync, StatelessAsync> {

    List<Sync> getSyncSpecs();

    List<Async> getAsyncSpecs();

    List<StatelessSync> getStatelessSyncSpecs();

    List<StatelessAsync> getStatelessAsyncSpecs();

}
