from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.http import HttpRequest
from micronaut.http.client import HttpClient
from micronaut.http.client.annotation import Client
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test


@Property(name="micronaut.mcp.server.transport", value="HTTP")
@Property(name="spec.name", value="WhoAmIToolsTest")
@MicronautTest
class WhoAmIToolsTest:
    client: Annotated[HttpClient, Inject, Client("/")]

    @Test
    def without_an_authenticated_user(self) -> None:
        result = str(self.client.toBlocking().retrieve(HttpRequest.POST(
            "/mcp", '{"jsonrpc": "2.0", "id": 1, "method": "tools/call", "params": {"name": "whoami", "arguments": {}}}')))
        assert '"text":"anonymous"' in result, result
