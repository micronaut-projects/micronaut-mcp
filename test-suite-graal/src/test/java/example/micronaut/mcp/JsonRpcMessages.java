package example.micronaut.mcp;

public final class JsonRpcMessages {
    private JsonRpcMessages() {
    }

    public static final String INITIALIZE = """
             {"jsonrpc":"2.0","id":0,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{"sampling":{},"elicitation":{},"roots":{"listChanged":true}},"clientInfo":{"name":"mcp-inspector","version":"0.16.3"}}}""";

    public static final String INITIALIZED = """
        {"jsonrpc": "2.0", "method": "notifications/initialized"}""";

    public static final String PING = """
        {"jsonrpc":"2.0","method":"ping","id":"123"}""";

    public static final String PONG = """
        {"jsonrpc":"2.0","result":{},"id":"123"}""";

    public static final String TOOLS_LIST = """
        {"jsonrpc":"2.0","id":3,"method":"tools/list","params":{"_meta":{"progressToken":3}}}""";

}
