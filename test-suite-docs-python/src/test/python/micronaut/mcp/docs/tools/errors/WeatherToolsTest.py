import json
from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.http import HttpRequest
from micronaut.http.client import HttpClient
from micronaut.http.client.annotation import Client
from micronaut.http.client.exceptions import HttpClientResponseException
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test


def request(tool: str, city: str) -> HttpRequest:
    return HttpRequest.POST("/mcp", json.dumps(
        {"jsonrpc": "2.0", "id": 1, "method": "tools/call", "params": {"name": tool, "arguments": {"city": city}}}))


@Property(name="micronaut.mcp.server.transport", value="HTTP")
@Property(name="spec.name", value="WeatherToolsTest")
@MicronautTest
class WeatherToolsTest:
    client: Annotated[HttpClient, Inject, Client("/")]

    def call(self, tool: str, city: str) -> dict:
        return json.loads(str(self.client.toBlocking().retrieve(request(tool, city))))["result"]

    @Test
    def an_expected_failure_is_an_error_result(self) -> None:
        result = self.call("temperature", "Paris")
        assert result["isError"] is True
        assert result["content"][0]["text"] == "Unknown city Paris, use Madrid or London"

    @Test
    def an_exception_is_a_tool_execution_error(self) -> None:
        result = self.call("forecast", "Madrid")
        assert result["isError"] is True
        assert result["content"][0]["text"] == "Tool forecast failed"

    @Test
    def an_mcp_error_is_a_protocol_error(self) -> None:
        try:
            self.client.toBlocking().retrieve(request("alerts", "Madrid"))
        except HttpClientResponseException as ex:
            error = json.loads(str(ex.getResponse().body()))["error"]
            assert error == {"code": -32603, "message": "Weather alerts are disabled on this server"}, error
        else:
            assert False, "a protocol error was expected"
