package io.micronaut.mcp.docs.resources;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.naming.Named;

/**
 * Configuration of a PGN file: {@code pgn.<name>.path} and {@code pgn.<name>.round}.
 */
@Requires(property = "spec.name", value = "ResourcesFactoryTest")
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
