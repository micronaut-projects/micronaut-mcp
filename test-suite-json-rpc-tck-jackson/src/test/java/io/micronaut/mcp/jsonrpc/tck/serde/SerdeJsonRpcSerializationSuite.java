package io.micronaut.mcp.jsonrpc.tck.serde;

import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SelectPackages("io.micronaut.mcp.jsonrpc.tck")
@SuiteDisplayName("JSON RPC Serialization TCK Jackson")
public class SerdeJsonRpcSerializationSuite {
}
