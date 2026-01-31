package com.github.dgdevel.core.jsonrpc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JsonRpcErrorTest {
    @Test
    public void testConstructor() {
        JsonRpcError error = new JsonRpcError(-32601, "Method not found");
        assertEquals(-32601, error.getCode());
        assertEquals("Method not found", error.getMessage());
        assertNull(error.getData());
    }

    @Test
    public void testSetCode() {
        JsonRpcError error = new JsonRpcError(-32601, "Method not found");
        error.setCode(-32603);
        assertEquals(-32603, error.getCode());
    }

    @Test
    public void testSetMessage() {
        JsonRpcError error = new JsonRpcError(-32601, "Method not found");
        error.setMessage("Internal error");
        assertEquals("Internal error", error.getMessage());
    }

    @Test
    public void testSetData() {
        JsonRpcError error = new JsonRpcError(-32601, "Method not found");
        Object data = "additional data";
        error.setData(data);
        assertEquals(data, error.getData());
    }

    @Test
    public void testGetData() {
        JsonRpcError error = new JsonRpcError(-32601, "Method not found");
        assertNull(error.getData());
    }

    @Test
    public void testParseError() {
        JsonRpcError error = new JsonRpcError(-32700, "Parse error");
        assertEquals(-32700, error.getCode());
        assertEquals("Parse error", error.getMessage());
    }

    @Test
    public void testInvalidRequest() {
        JsonRpcError error = new JsonRpcError(-32600, "Invalid Request");
        assertEquals(-32600, error.getCode());
        assertEquals("Invalid Request", error.getMessage());
    }

    @Test
    public void testMethodNotFound() {
        JsonRpcError error = new JsonRpcError(-32601, "Method not found");
        assertEquals(-32601, error.getCode());
        assertEquals("Method not found", error.getMessage());
    }

    @Test
    public void testInvalidParams() {
        JsonRpcError error = new JsonRpcError(-32602, "Invalid params");
        assertEquals(-32602, error.getCode());
        assertEquals("Invalid params", error.getMessage());
    }

    @Test
    public void testInternalError() {
        JsonRpcError error = new JsonRpcError(-32603, "Internal error");
        assertEquals(-32603, error.getCode());
        assertEquals("Internal error", error.getMessage());
    }

    @Test
    public void testServerError() {
        JsonRpcError error = new JsonRpcError(-32000, "Server error");
        assertEquals(-32000, error.getCode());
        assertEquals("Server error", error.getMessage());
    }
}
