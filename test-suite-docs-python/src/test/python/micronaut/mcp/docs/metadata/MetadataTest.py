import json
from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.http import HttpRequest
from micronaut.http.client import HttpClient
from micronaut.http.client.annotation import Client
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test


@Property(name="micronaut.mcp.server.transport", value="HTTP")
@Property(name="spec.name", value="MetadataTest")
@MicronautTest
class MetadataTest:
    client: Annotated[HttpClient, Inject, Client("/")]

    @Test
    def icons_and_meta_of_a_tool(self) -> None:
        result = json.loads(str(self.client.toBlocking().retrieve(HttpRequest.POST(
            "/mcp", '{"jsonrpc": "2.0", "id": 1, "method": "tools/list", "params": {}}'))))
        tool = result["result"]["tools"][0]
        assert tool["name"] == "forecast"
        assert tool["icons"] == [{"src": "https://example.com/sun.svg", "mimeType": "image/svg+xml", "sizes": ["any"]}]
        assert tool["_meta"] == {"com.example/category": "weather"}

    @Test
    def annotations_of_a_resource(self) -> None:
        result = json.loads(str(self.client.toBlocking().retrieve(HttpRequest.POST(
            "/mcp", '{"jsonrpc": "2.0", "id": 1, "method": "resources/list", "params": {}}'))))
        resource = result["result"]["resources"][0]
        assert resource["size"] == 4
        assert resource["annotations"] == {"audience": ["assistant"], "priority": 0.5}
