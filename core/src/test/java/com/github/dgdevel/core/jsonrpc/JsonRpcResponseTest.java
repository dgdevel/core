package com.github.dgdevel.core.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JsonRpcResponseTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testDefaultJsonrpc() {
        JsonRpcResponse response = new JsonRpcResponse(1);
        assertEquals("2.0", response.getJsonrpc());
    }

    @Test
    public void testSetJsonrpc() {
        JsonRpcResponse response = new JsonRpcResponse(1);
        response.setJsonrpc("2.0");
        assertEquals("2.0", response.getJsonrpc());
    }

    @Test
    public void testId() {
        JsonRpcResponse response = new JsonRpcResponse(123);
        assertEquals(123, response.getId());
    }

    @Test
    public void testSetId() {
        JsonRpcResponse response = new JsonRpcResponse(1);
        response.setId(456);
        assertEquals(456, response.getId());
    }

    @Test
    public void testResult() {
        JsonRpcResponse response = new JsonRpcResponse(1);
        Object result = "test result";
        response.setResult(result);
        assertEquals(result, response.getResult());
    }

    @Test
    public void testError() {
        JsonRpcResponse response = new JsonRpcResponse(1);
        JsonRpcError error = new JsonRpcError(-32000, "Server error");
        response.setError(error);
        assertEquals(error, response.getError());
    }

    @Test
    public void testSuccessFactory() {
        JsonRpcResponse response = JsonRpcResponse.success(1, "result");
        assertEquals(1, response.getId());
        assertEquals("result", response.getResult());
        assertNull(response.getError());
    }

    @Test
    public void testErrorFactory() {
        JsonRpcResponse response = JsonRpcResponse.error(1, -32601, "Method not found");
        assertEquals(1, response.getId());
        assertEquals(-32601, response.getError().getCode());
        assertEquals("Method not found", response.getError().getMessage());
        assertNull(response.getResult());
    }

    @Test
    public void testSuccessSerialization() throws Exception {
        JsonRpcResponse response = JsonRpcResponse.success(1, "test result");
        String json = objectMapper.writeValueAsString(response);
        JsonNode node = objectMapper.readTree(json);

        assertEquals("2.0", node.get("jsonrpc").asText());
        assertEquals(1, node.get("id").asInt());
        assertEquals("test result", node.get("result").asText());
        assertFalse(node.has("error"));
    }

    @Test
    public void testErrorSerialization() throws Exception {
        JsonRpcResponse response = JsonRpcResponse.error(1, -32601, "Method not found");
        String json = objectMapper.writeValueAsString(response);
        JsonNode node = objectMapper.readTree(json);

        assertEquals("2.0", node.get("jsonrpc").asText());
        assertEquals(1, node.get("id").asInt());
        assertEquals(-32601, node.get("error").get("code").asInt());
        assertEquals("Method not found", node.get("error").get("message").asText());
        assertFalse(node.has("result"));
    }

    @Test
    public void testNullResultSerialization() throws Exception {
        JsonRpcResponse response = new JsonRpcResponse(1);
        response.setResult(null);

        String json = objectMapper.writeValueAsString(response);
        JsonNode node = objectMapper.readTree(json);

        assertFalse(node.has("result"));
    }

    @Test
    public void testNullErrorSerialization() throws Exception {
        JsonRpcResponse response = new JsonRpcResponse(1);
        response.setError(null);

        String json = objectMapper.writeValueAsString(response);
        JsonNode node = objectMapper.readTree(json);

        assertFalse(node.has("error"));
    }

    @Test
    public void testComplexResultSerialization() throws Exception {
        Object result = new Object[]{"key1", "value1"};
        JsonRpcResponse response = JsonRpcResponse.success(1, result);

        String json = objectMapper.writeValueAsString(response);
        JsonNode node = objectMapper.readTree(json);

        assertTrue(node.has("result"));
        assertTrue(node.get("result").isArray());
        assertEquals(2, node.get("result").size());
    }
}
