package io.micronaut.mcp.server.stateless.sync.tools.jsonschema.localrefs;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.utils.JsonSchemaClassPathResourceLoader;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.annotations.ToolArg;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

@Requires(property = "spec.name", value = "LocalReferencesTest")
@Singleton
class LocalReferencesTools {

    @Tool(description = "Names of a node and of its descendants.")
    String names(@ToolArg Node node) {
        return node.child() == null ? node.name() : node.name() + "/" + names(node.child());
    }

    @Tool(description = "Names of a tree and of its descendants.")
    String treeNames(@ToolArg Tree tree) {
        return tree.child() == null ? tree.name() : tree.name() + "/" + treeNames(tree.child());
    }

    @Introspected
    record Tree(String name, @Nullable Tree child) {
    }

    @Introspected
    record Node(String name, @Nullable Node child) {
    }

    /**
     * Provides hand-written schemas for {@link Tree} and {@link Node}, with references local to the schema, as a
     * schema-first file may have.
     */
    @Requires(property = "spec.name", value = "LocalReferencesTest")
    @Singleton
    static class NodeSchemaLoader implements JsonSchemaClassPathResourceLoader {
        @Override
        public <T> Optional<String> jsonSchemaStringForClass(Class<T> type) {
            if (type == Tree.class) {
                return Optional.of("""
                    {
                      "$id": "classpath:META-INF/schemas/local-references-tree.schema.json",
                      "$schema": "https://json-schema.org/draft/2020-12/schema",
                      "type": "object",
                      "properties": {
                        "name": {"type": "string", "maxLength": 3},
                        "child": {"$ref": "#"}
                      },
                      "required": ["name"],
                      "additionalProperties": false
                    }""");
            }
            if (type != Node.class) {
                return Optional.empty();
            }
            return Optional.of("""
                {
                  "$id": "classpath:META-INF/schemas/local-references-node.schema.json",
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "name": {"$ref": "#/$defs/name"},
                    "child": {"$ref": "#"}
                  },
                  "required": ["name"],
                  "additionalProperties": false,
                  "$defs": {
                    "name": {"type": "string", "maxLength": 3}
                  }
                }""");
        }
    }
}
