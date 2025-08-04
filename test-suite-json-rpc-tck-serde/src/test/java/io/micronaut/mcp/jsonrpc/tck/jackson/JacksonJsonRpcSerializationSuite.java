package io.micronaut.mcp.jsonrpc.tck.jackson;

import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SelectPackages("io.micronaut.mcp.jsonrpc.tck")
@SuiteDisplayName("JSON RPC Serialization TCK Serde")
public class JacksonJsonRpcSerializationSuite {
}
