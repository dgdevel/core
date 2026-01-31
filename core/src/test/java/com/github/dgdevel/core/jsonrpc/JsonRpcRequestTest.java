package com.github.dgdevel.core.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JsonRpcRequestTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testDefaultJsonrpc() {
        JsonRpcRequest request = new JsonRpcRequest();
        assertEquals("2.0", request.getJsonrpc());
    }

    @Test
    public void testSetJsonrpc() {
        JsonRpcRequest request = new JsonRpcRequest();
        request.setJsonrpc("2.0");
        assertEquals("2.0", request.getJsonrpc());
    }

    @Test
    public void testMethod() {
        JsonRpcRequest request = new JsonRpcRequest();
        request.setMethod("testMethod");
        assertEquals("testMethod", request.getMethod());
    }

    @Test
    public void testParams() {
        JsonRpcRequest request = new JsonRpcRequest();
        Object params = new Object[]{"param1", "param2"};
        request.setParams(params);
        assertEquals(params, request.getParams());
    }

    @Test
    public void testId() {
        JsonRpcRequest request = new JsonRpcRequest();
        request.setId(123);
        assertEquals(123, request.getId());
    }

    @Test
    public void testSerialization() throws Exception {
        JsonRpcRequest request = new JsonRpcRequest();
        request.setMethod("testMethod");
        request.setParams(new Object[]{"arg1", "arg2"});
        request.setId(1);

        String json = objectMapper.writeValueAsString(request);
        JsonNode node = objectMapper.readTree(json);

        assertEquals("2.0", node.get("jsonrpc").asText());
        assertEquals("testMethod", node.get("method").asText());
        assertEquals(2, node.get("params").size());
        assertEquals(1, node.get("id").asInt());
    }

    @Test
    public void testDeserialization() throws Exception {
        String json = """
            {
                "jsonrpc": "2.0",
                "method": "testMethod",
                "params": ["arg1", "arg2"],
                "id": 1
            }
            """;

        JsonRpcRequest request = objectMapper.readValue(json, JsonRpcRequest.class);
        assertEquals("2.0", request.getJsonrpc());
        assertEquals("testMethod", request.getMethod());
        assertTrue(request.getParams() instanceof java.util.List);
        assertEquals("arg1", ((java.util.List<?>) request.getParams()).get(0));
        assertEquals(1, request.getId());
    }

    @Test
    public void testNullParamsSerialization() throws Exception {
        JsonRpcRequest request = new JsonRpcRequest();
        request.setMethod("testMethod");
        request.setParams(null);
        request.setId(1);

        String json = objectMapper.writeValueAsString(request);
        JsonNode node = objectMapper.readTree(json);

        assertFalse(node.has("params"));
    }

    @Test
    public void testEmptyParamsSerialization() throws Exception {
        JsonRpcRequest request = new JsonRpcRequest();
        request.setMethod("testMethod");
        request.setParams(new Object[]{});
        request.setId(1);

        String json = objectMapper.writeValueAsString(request);
        JsonNode node = objectMapper.readTree(json);

        assertTrue(node.has("params"));
        assertEquals(0, node.get("params").size());
    }
}
