package io.modelcontextprotocol.server.http.tck.async;

import io.micronaut.mcp.annotations.Resource;
import jakarta.inject.Singleton;

@Singleton
class Resources {
    @Resource(
        uri = "file:///project/src/main.rs",
        name = "main.rs",
        title = "Rust Software Application Main File",
        description = "Primary application entry point",
        mimeType = "text/x-rust")
    String mainRs() {
        return "fn main() {\n    println!(\"Hello world!\");\n}";
    }
}
