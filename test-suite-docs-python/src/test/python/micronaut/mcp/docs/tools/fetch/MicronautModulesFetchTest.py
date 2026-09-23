import json
from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.http import HttpRequest
from micronaut.http.client import HttpClient
from micronaut.http.client.annotation import Client
from micronaut.mcp.server.tools.fetch import FetchTool
from micronaut.mcp.docs.JsonRpcMessages import EXPECTED_FETCH_TOOL_CALL, FETCH_TOOL_CALL, TOOLS_LIST
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test


@Property(name="micronaut.mcp.server.transport", value="HTTP")
@Property(name="spec.name", value="MicronautModulesFetchTest")
@MicronautTest
class MicronautModulesFetchTest:
    client: Annotated[HttpClient, Inject, Client("/")]
    tool: Annotated[FetchTool, Inject]

    @Test
    def fetch_tool(self) -> None:
        assert self.tool.getName() == "fetch"
        assert self.tool.getTitle() == "Fetch"
        tools = str(self.client.toBlocking().retrieve(HttpRequest.POST("/mcp", TOOLS_LIST)))
        assert '"name":"fetch"' in tools, tools
        assert '"title":"Fetch"' in tools, tools
        assert '"description":"This tool retrieves the full contents of a search result document or item."' in tools, tools
        result = str(self.client.toBlocking().retrieve(HttpRequest.POST("/mcp", FETCH_TOOL_CALL)))
        assert json.loads(result) == json.loads(EXPECTED_FETCH_TOOL_CALL)
