from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.http import HttpRequest
from micronaut.http.client import HttpClient
from micronaut.http.client.annotation import Client
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test


@Property(name="micronaut.mcp.server.transport", value="HTTP")
@Property(name="spec.name", value="CatalogToolsTest")
@MicronautTest
class CatalogToolsTest:
    client: Annotated[HttpClient, Inject, Client("/")]

    @Test
    def progress_and_log_messages_are_streamed(self) -> None:
        request = HttpRequest.POST(
            "/mcp",
            '{"jsonrpc": "2.0", "id": 1, "method": "tools/call", '
            '"params": {"name": "import_catalog", "arguments": {"pages": 2}, "_meta": {"progressToken": "import"}}}')
        result = str(self.client.toBlocking().retrieve(request.header("Accept", "application/json, text/event-stream")))
        assert "notifications/progress" in result and "Imported page 2" in result, result
        assert "notifications/message" in result and '"text":"done"' in result, result
