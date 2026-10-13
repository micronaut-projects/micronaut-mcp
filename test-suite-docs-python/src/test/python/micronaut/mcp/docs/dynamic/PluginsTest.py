from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.http import HttpRequest
from micronaut.http.client import HttpClient
from micronaut.http.client.annotation import Client
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from .Plugins import Plugins


@Property(name="micronaut.mcp.server.transport", value="HTTP")
@Property(name="micronaut.mcp.server.tools.list-changed", value="true")
@Property(name="spec.name", value="PluginsTest")
@MicronautTest
class PluginsTest:
    client: Annotated[HttpClient, Inject, Client("/")]
    plugins: Annotated[Plugins, Inject]

    def call(self, method: str, params: str) -> str:
        return str(self.client.toBlocking().retrieve(HttpRequest.POST(
            "/mcp", '{"jsonrpc": "2.0", "id": 1, "method": "' + method + '", "params": ' + params + '}')))

    @Test
    def tools_are_installed_and_uninstalled_at_runtime(self) -> None:
        self.plugins.install("greeter")
        assert '"name":"greeter"' in self.call("tools/list", "{}")
        assert '"text":"installed"' in self.call("tools/call", '{"name": "greeter", "arguments": {}}')
        self.plugins.uninstall("greeter")
        assert '"name":"greeter"' not in self.call("tools/list", "{}")
