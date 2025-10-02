package io.micronaut.mcp.server.stateless.sync.resources;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.naming.Named;

@Requires(property = "spec.name", value = "StatelessSyncResourceListTest")
@EachProperty("pgn")
class PgnFile implements Named {
    private final String name;
    private String path;
    private Integer round;

    PgnFile(@Parameter String name) {
        this.name = name;
    }

    public Integer getRound() {
        return round;
    }

    public void setRound(Integer round) {
        this.round = round;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
