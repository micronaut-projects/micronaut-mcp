package io.micronaut.mcp.server.exceptions;

import io.micronaut.core.convert.ConversionError;
import io.micronaut.core.convert.exceptions.ConversionErrorException;
import io.micronaut.core.type.Argument;
import io.micronaut.serde.exceptions.SerdeException;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputErrorMappersTest {

    @Test
    void theBuiltInMappersOfArgumentsAreInputErrorMappers() {
        // A tool reports what an InputErrorMapper maps as a tool execution error, rather than a protocol error
        assertInstanceOf(InputErrorMapper.class, new ConstraintViolationExceptionMcpErrorMapper());
        assertInstanceOf(InputErrorMapper.class, new ConversionErrorExceptionMcpErrorMapper());
        assertInstanceOf(InputErrorMapper.class, new InvalidFormatExceptionMcpErrorMapper());
        assertInstanceOf(InputErrorMapper.class, new SerdeExceptionMcpErrorMapper());
        assertInstanceOf(InputErrorMapper.class, new UnsatisfiedArgumentExceptionMcpErrorMapper());
    }

    @Test
    void aValueThatCannotBeConvertedIsAnInvalidParameter() {
        ConversionErrorExceptionMcpErrorMapper mapper = new ConversionErrorExceptionMcpErrorMapper();
        ConversionErrorException exception = new ConversionErrorException(Argument.of(Integer.class, "count"),
            new IllegalArgumentException("not a number"));
        assertTrue(mapper.canMap(ConversionErrorException.class));
        McpError error = mapper.map(exception);
        assertEquals(McpSchema.ErrorCodes.INVALID_PARAMS, error.getJsonRpcError().code());
        assertEquals(exception.getMessage(), error.getMessage());
    }

    @Test
    void theMessageOfASerdeExceptionIsTheMessageOfItsCause() {
        SerdeExceptionMcpErrorMapper mapper = new SerdeExceptionMcpErrorMapper();
        assertEquals("Unexpected character", mapper.map(new SerdeException("Error decoding property", new IllegalStateException("Unexpected character"))).getMessage());
        assertEquals("Error decoding property", mapper.map(new SerdeException("Error decoding property")).getMessage());
    }

    @Test
    void aSerdeExceptionWhoseCauseHasNoMessageKeepsItsOwnMessage() {
        // The SDK rejects an error without a message
        SerdeExceptionMcpErrorMapper mapper = new SerdeExceptionMcpErrorMapper();
        assertEquals("Error decoding property", mapper.map(new SerdeException("Error decoding property", new NullPointerException())).getMessage());
    }
}
