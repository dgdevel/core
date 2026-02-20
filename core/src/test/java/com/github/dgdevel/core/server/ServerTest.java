package com.github.dgdevel.core.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

public class ServerTest {
    private Server server;
    private final int testPort = 18080;
    private final String testDbUrl = "jdbc:h2:mem:test";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() throws Exception {
        server = new Server(testPort, testDbUrl);
        server.start();
    }

    @AfterEach
    public void tearDown() {
        server.shutdown();
    }

    @Test
    public void testGenericPing() throws Exception {
        String request = """
            {
                "jsonrpc": "2.0",
                "method": "generic/ping",
                "params": {},
                "id": 1
            }
            """;

        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(request.getBytes());
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        assertEquals("2.0", jsonResponse.get("jsonrpc").asText());
        assertEquals(1, jsonResponse.get("id").asInt());
        assertNotNull(jsonResponse.get("result"));
        assertTrue(jsonResponse.get("result").asText().matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d+"));
    }

    @Test
    public void testInvalidMethod() throws Exception {
        String request = """
            {
                "jsonrpc": "2.0",
                "method": "invalid/method",
                "params": {},
                "id": 2
            }
            """;

        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(request.getBytes());
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        assertEquals("2.0", jsonResponse.get("jsonrpc").asText());
        assertEquals(2, jsonResponse.get("id").asInt());
        assertNotNull(jsonResponse.get("error"));
        assertEquals(-32601, jsonResponse.get("error").get("code").asInt());
        assertEquals("Method not found", jsonResponse.get("error").get("message").asText());
    }

    @Test
    public void testSetConfigValue() throws Exception {
        String request = """
            {
                "jsonrpc": "2.0",
                "method": "generic/setConfigValue",
                "params": ["test", "key1", "value1"],
                "id": 3
            }
            """;

        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(request.getBytes());
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        assertEquals("2.0", jsonResponse.get("jsonrpc").asText());
        assertEquals(3, jsonResponse.get("id").asInt());
        assertNotNull(jsonResponse.get("result"));
        assertTrue(jsonResponse.get("result").get("success").asBoolean());
    }

    @Test
    public void testGetConfigValue() throws Exception {
        String setRequest = """
            {
                "jsonrpc": "2.0",
                "method": "generic/setConfigValue",
                "params": ["test", "key1", "value1"],
                "id": 4
            }
            """;

        HttpURLConnection setConnection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        setConnection.setRequestMethod("POST");
        setConnection.setRequestProperty("Content-Type", "application/json");
        setConnection.setDoOutput(true);

        try (OutputStream os = setConnection.getOutputStream()) {
            os.write(setRequest.getBytes());
            os.flush();
        }
        setConnection.getResponseCode();

        String getRequest = """
            {
                "jsonrpc": "2.0",
                "method": "generic/getConfigValue",
                "params": ["test", "key1"],
                "id": 5
            }
            """;

        HttpURLConnection getConnection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        getConnection.setRequestMethod("POST");
        getConnection.setRequestProperty("Content-Type", "application/json");
        getConnection.setDoOutput(true);

        try (OutputStream os = getConnection.getOutputStream()) {
            os.write(getRequest.getBytes());
            os.flush();
        }

        int responseCode = getConnection.getResponseCode();
        assertEquals(200, responseCode);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getConnection.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        assertEquals("2.0", jsonResponse.get("jsonrpc").asText());
        assertEquals(5, jsonResponse.get("id").asInt());
        assertEquals("value1", jsonResponse.get("result").asText());
    }

    @Test
    public void testGetConfigValueNotFound() throws Exception {
        String request = """
            {
                "jsonrpc": "2.0",
                "method": "generic/getConfigValue",
                "params": ["test", "nonexistent"],
                "id": 6
            }
            """;

        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(request.getBytes());
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        assertEquals("2.0", jsonResponse.get("jsonrpc").asText());
        assertEquals(6, jsonResponse.get("id").asInt());
        assertNull(jsonResponse.get("result"));
    }

    @Test
    public void testLocalize() throws Exception {
        String request = """
            {
                "jsonrpc": "2.0",
                "method": "generic/localize",
                "params": ["greeting", "en", "Hello World"],
                "id": 7
            }
            """;

        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(request.getBytes());
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        assertEquals("2.0", jsonResponse.get("jsonrpc").asText());
        assertEquals(7, jsonResponse.get("id").asInt());
        assertNotNull(jsonResponse.get("result"));
        assertTrue(jsonResponse.get("result").get("success").asBoolean());
    }

    @Test
    public void testTranslate() throws Exception {
        String localizeRequest = """
            {
                "jsonrpc": "2.0",
                "method": "generic/localize",
                "params": ["greeting", "en", "Hello World"],
                "id": 8
            }
            """;

        HttpURLConnection localizeConnection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        localizeConnection.setRequestMethod("POST");
        localizeConnection.setRequestProperty("Content-Type", "application/json");
        localizeConnection.setDoOutput(true);

        try (OutputStream os = localizeConnection.getOutputStream()) {
            os.write(localizeRequest.getBytes());
            os.flush();
        }
        localizeConnection.getResponseCode();

        String translateRequest = """
            {
                "jsonrpc": "2.0",
                "method": "generic/translate",
                "params": ["greeting", "en"],
                "id": 9
            }
            """;

        HttpURLConnection translateConnection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        translateConnection.setRequestMethod("POST");
        translateConnection.setRequestProperty("Content-Type", "application/json");
        translateConnection.setDoOutput(true);

        try (OutputStream os = translateConnection.getOutputStream()) {
            os.write(translateRequest.getBytes());
            os.flush();
        }

        int responseCode = translateConnection.getResponseCode();
        assertEquals(200, responseCode);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(translateConnection.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        assertEquals("2.0", jsonResponse.get("jsonrpc").asText());
        assertEquals(9, jsonResponse.get("id").asInt());
        assertEquals("Hello World", jsonResponse.get("result").asText());
    }

    @Test
    public void testTranslateNotFound() throws Exception {
        String request = """
            {
                "jsonrpc": "2.0",
                "method": "generic/translate",
                "params": ["nonexistent", "fr"],
                "id": 10
            }
            """;

        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(request.getBytes());
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        assertEquals("2.0", jsonResponse.get("jsonrpc").asText());
        assertEquals(10, jsonResponse.get("id").asInt());
        assertNull(jsonResponse.get("result"));
    }

    @Test
    public void testSetConfigValueUpdate() throws Exception {
        String setRequest = """
            {
                "jsonrpc": "2.0",
                "method": "generic/setConfigValue",
                "params": ["test", "key1", "value1"],
                "id": 11
            }
            """;

        HttpURLConnection setConnection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        setConnection.setRequestMethod("POST");
        setConnection.setRequestProperty("Content-Type", "application/json");
        setConnection.setDoOutput(true);

        try (OutputStream os = setConnection.getOutputStream()) {
            os.write(setRequest.getBytes());
            os.flush();
        }
        setConnection.getResponseCode();

        String updateRequest = """
            {
                "jsonrpc": "2.0",
                "method": "generic/setConfigValue",
                "params": ["test", "key1", "updated_value"],
                "id": 12
            }
            """;

        HttpURLConnection updateConnection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        updateConnection.setRequestMethod("POST");
        updateConnection.setRequestProperty("Content-Type", "application/json");
        updateConnection.setDoOutput(true);

        try (OutputStream os = updateConnection.getOutputStream()) {
            os.write(updateRequest.getBytes());
            os.flush();
        }
        updateConnection.getResponseCode();

        String getRequest = """
            {
                "jsonrpc": "2.0",
                "method": "generic/getConfigValue",
                "params": ["test", "key1"],
                "id": 13
            }
            """;

        HttpURLConnection getConnection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        getConnection.setRequestMethod("POST");
        getConnection.setRequestProperty("Content-Type", "application/json");
        getConnection.setDoOutput(true);

        try (OutputStream os = getConnection.getOutputStream()) {
            os.write(getRequest.getBytes());
            os.flush();
        }

        int responseCode = getConnection.getResponseCode();
        assertEquals(200, responseCode);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getConnection.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        assertEquals("2.0", jsonResponse.get("jsonrpc").asText());
        assertEquals(13, jsonResponse.get("id").asInt());
        assertEquals("updated_value", jsonResponse.get("result").asText());
    }

    @Test
    public void testInvalidParams() throws Exception {
        String request = """
            {
                "jsonrpc": "2.0",
                "method": "generic/setConfigValue",
                "params": "invalid_params",
                "id": 14
            }
            """;

        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(request.getBytes());
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        assertEquals("2.0", jsonResponse.get("jsonrpc").asText());
        assertEquals(14, jsonResponse.get("id").asInt());
        assertNotNull(jsonResponse.get("error"));
        assertTrue(jsonResponse.get("error").get("code").asInt() < 0);
    }

    @Test
    public void testMalformedJsonRequest() throws Exception {
        String request = "invalid json";

        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(request.getBytes());
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        assertEquals(500, responseCode);
    }

    @Test
    public void testEmptyMethod() throws Exception {
        String request = """
            {
                "jsonrpc": "2.0",
                "method": "",
                "params": {},
                "id": 15
            }
            """;

        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(request.getBytes());
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        assertEquals("2.0", jsonResponse.get("jsonrpc").asText());
        assertEquals(15, jsonResponse.get("id").asInt());
        assertNotNull(jsonResponse.get("error"));
        assertEquals(-32601, jsonResponse.get("error").get("code").asInt());
    }

    @Test
    public void testInvalidHttpMethod() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        int responseCode = connection.getResponseCode();
        assertEquals(405, responseCode);
    }

    @Test
    public void testNullParams() throws Exception {
        String request = """
            {
                "jsonrpc": "2.0",
                "method": "generic/ping",
                "params": null,
                "id": 16
            }
            """;

        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(request.getBytes());
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        assertEquals("2.0", jsonResponse.get("jsonrpc").asText());
        assertEquals(16, jsonResponse.get("id").asInt());
        assertNotNull(jsonResponse.get("result"));
    }

    @Test
    public void testObjectParams() throws Exception {
        String request = """
            {
                "jsonrpc": "2.0",
                "method": "generic/ping",
                "params": {"key": "value"},
                "id": 17
            }
            """;

        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(request.getBytes());
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        assertEquals("2.0", jsonResponse.get("jsonrpc").asText());
        assertEquals(17, jsonResponse.get("id").asInt());
    }

    @Test
    public void testArrayParams() throws Exception {
        String request = """
            {
                "jsonrpc": "2.0",
                "method": "generic/setConfigValue",
                "params": ["test", "key2", "value2"],
                "id": 18
            }
            """;

        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(request.getBytes());
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        assertEquals("2.0", jsonResponse.get("jsonrpc").asText());
        assertEquals(18, jsonResponse.get("id").asInt());
        assertNotNull(jsonResponse.get("result"));
        assertTrue(jsonResponse.get("result").get("success").asBoolean());
    }

    @Test
    public void testLocalizeUpdate() throws Exception {
        String localizeRequest = """
            {
                "jsonrpc": "2.0",
                "method": "generic/localize",
                "params": ["greeting", "en", "Hello"],
                "id": 19
            }
            """;

        HttpURLConnection localizeConnection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        localizeConnection.setRequestMethod("POST");
        localizeConnection.setRequestProperty("Content-Type", "application/json");
        localizeConnection.setDoOutput(true);

        try (OutputStream os = localizeConnection.getOutputStream()) {
            os.write(localizeRequest.getBytes());
            os.flush();
        }
        localizeConnection.getResponseCode();

        String updateRequest = """
            {
                "jsonrpc": "2.0",
                "method": "generic/localize",
                "params": ["greeting", "en", "Hello World"],
                "id": 20
            }
            """;

        HttpURLConnection updateConnection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        updateConnection.setRequestMethod("POST");
        updateConnection.setRequestProperty("Content-Type", "application/json");
        updateConnection.setDoOutput(true);

        try (OutputStream os = updateConnection.getOutputStream()) {
            os.write(updateRequest.getBytes());
            os.flush();
        }
        updateConnection.getResponseCode();

        String translateRequest = """
            {
                "jsonrpc": "2.0",
                "method": "generic/translate",
                "params": ["greeting", "en"],
                "id": 21
            }
            """;

        HttpURLConnection translateConnection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        translateConnection.setRequestMethod("POST");
        translateConnection.setRequestProperty("Content-Type", "application/json");
        translateConnection.setDoOutput(true);

        try (OutputStream os = translateConnection.getOutputStream()) {
            os.write(translateRequest.getBytes());
            os.flush();
        }

        int responseCode = translateConnection.getResponseCode();
        assertEquals(200, responseCode);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(translateConnection.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        assertEquals("2.0", jsonResponse.get("jsonrpc").asText());
        assertEquals(21, jsonResponse.get("id").asInt());
        assertEquals("Hello World", jsonResponse.get("result").asText());
    }

    @Test
    public void testGetChannel() throws Exception {
        assertNotNull(server.getJsonRpcChannel());
    }

    @Test
    public void testSetConfigValueWithDbError() throws Exception {
        String request = """
            {
                "jsonrpc": "2.0",
                "method": "generic/setConfigValue",
                "params": null,
                "id": 21
            }
            """;

        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(request.getBytes());
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        assertEquals("2.0", jsonResponse.get("jsonrpc").asText());
        assertEquals(21, jsonResponse.get("id").asInt());
        assertNotNull(jsonResponse.get("error"));
        assertTrue(jsonResponse.get("error").get("code").asInt() < 0);
    }

    @Test
    public void testGetAllConfigValues() throws Exception {
        String setRequest1 = """
            {
                "jsonrpc": "2.0",
                "method": "generic/setConfigValue",
                "params": ["test", "key1", "value1"],
                "id": 22
            }
            """;

        HttpURLConnection setConnection1 = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        setConnection1.setRequestMethod("POST");
        setConnection1.setRequestProperty("Content-Type", "application/json");
        setConnection1.setDoOutput(true);

        try (OutputStream os = setConnection1.getOutputStream()) {
            os.write(setRequest1.getBytes());
            os.flush();
        }
        setConnection1.getResponseCode();

        String setRequest2 = """
            {
                "jsonrpc": "2.0",
                "method": "generic/setConfigValue",
                "params": ["test", "key2", "value2"],
                "id": 23
            }
            """;

        HttpURLConnection setConnection2 = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        setConnection2.setRequestMethod("POST");
        setConnection2.setRequestProperty("Content-Type", "application/json");
        setConnection2.setDoOutput(true);

        try (OutputStream os = setConnection2.getOutputStream()) {
            os.write(setRequest2.getBytes());
            os.flush();
        }
        setConnection2.getResponseCode();

        String setRequest3 = """
            {
                "jsonrpc": "2.0",
                "method": "generic/setConfigValue",
                "params": ["other", "key1", "value3"],
                "id": 24
            }
            """;

        HttpURLConnection setConnection3 = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        setConnection3.setRequestMethod("POST");
        setConnection3.setRequestProperty("Content-Type", "application/json");
        setConnection3.setDoOutput(true);

        try (OutputStream os = setConnection3.getOutputStream()) {
            os.write(setRequest3.getBytes());
            os.flush();
        }
        setConnection3.getResponseCode();

        String getRequest = """
            {
                "jsonrpc": "2.0",
                "method": "generic/getAllConfigValues",
                "params": [],
                "id": 25
            }
            """;

        HttpURLConnection getConnection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        getConnection.setRequestMethod("POST");
        getConnection.setRequestProperty("Content-Type", "application/json");
        getConnection.setDoOutput(true);

        try (OutputStream os = getConnection.getOutputStream()) {
            os.write(getRequest.getBytes());
            os.flush();
        }

        int responseCode = getConnection.getResponseCode();
        assertEquals(200, responseCode);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getConnection.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        assertEquals("2.0", jsonResponse.get("jsonrpc").asText());
        assertEquals(25, jsonResponse.get("id").asInt());
        assertNotNull(jsonResponse.get("result"));
        assertEquals(3, jsonResponse.get("result").size());
        assertEquals("value1", jsonResponse.get("result").get("test.key1").asText());
        assertEquals("value2", jsonResponse.get("result").get("test.key2").asText());
        assertEquals("value3", jsonResponse.get("result").get("other.key1").asText());
    }

    @Test
    public void testGetAllConfigValuesEmpty() throws Exception {
        String request = """
            {
                "jsonrpc": "2.0",
                "method": "generic/getAllConfigValues",
                "params": [],
                "id": 26
            }
            """;

        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(request.getBytes());
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        assertEquals("2.0", jsonResponse.get("jsonrpc").asText());
        assertEquals(26, jsonResponse.get("id").asInt());
        assertNotNull(jsonResponse.get("result"));
        assertEquals(0, jsonResponse.get("result").size());
    }

    @Test
    public void testAuditLog() throws Exception {
        String request = """
            {
                "jsonrpc": "2.0",
                "method": "audit/log",
                "params": [null, "LOGIN", "user logged in"],
                "id": 27
            }
            """;

        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(request.getBytes());
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        assertEquals("2.0", jsonResponse.get("jsonrpc").asText());
        assertEquals(27, jsonResponse.get("id").asInt());
        assertNotNull(jsonResponse.get("result"));
        assertNotNull(jsonResponse.get("result").get("id"));
        assertTrue(jsonResponse.get("result").get("id").asLong() > 0);
    }

    @Test
    public void testAuditLogWithNullUserId() throws Exception {
        String request = """
            {
                "jsonrpc": "2.0",
                "method": "audit/log",
                "params": [null, "SYSTEM", "system event"],
                "id": 28
            }
            """;

        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(request.getBytes());
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        assertEquals("2.0", jsonResponse.get("jsonrpc").asText());
        assertEquals(28, jsonResponse.get("id").asInt());
        assertNotNull(jsonResponse.get("result"));
        assertNotNull(jsonResponse.get("result").get("id"));
        assertTrue(jsonResponse.get("result").get("id").asLong() > 0);
    }

    @Test
    public void testAuditLogList() throws Exception {
        String logRequest1 = """
            {
                "jsonrpc": "2.0",
                "method": "audit/log",
                "params": [null, "LOGIN", "user 1 logged in"],
                "id": 29
            }
            """;

        HttpURLConnection logConnection1 = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        logConnection1.setRequestMethod("POST");
        logConnection1.setRequestProperty("Content-Type", "application/json");
        logConnection1.setDoOutput(true);

        try (OutputStream os = logConnection1.getOutputStream()) {
            os.write(logRequest1.getBytes());
            os.flush();
        }
        logConnection1.getResponseCode();

        String logRequest2 = """
            {
                "jsonrpc": "2.0",
                "method": "audit/log",
                "params": [null, "LOGOUT", "user 2 logged out"],
                "id": 30
            }
            """;

        HttpURLConnection logConnection2 = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        logConnection2.setRequestMethod("POST");
        logConnection2.setRequestProperty("Content-Type", "application/json");
        logConnection2.setDoOutput(true);

        try (OutputStream os = logConnection2.getOutputStream()) {
            os.write(logRequest2.getBytes());
            os.flush();
        }
        logConnection2.getResponseCode();

        String listRequest = """
            {
                "jsonrpc": "2.0",
                "method": "audit/list",
                "params": [{"pageNumber": 1, "pageSize": 10}],
                "id": 31
            }
            """;

        HttpURLConnection listConnection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        listConnection.setRequestMethod("POST");
        listConnection.setRequestProperty("Content-Type", "application/json");
        listConnection.setDoOutput(true);

        try (OutputStream os = listConnection.getOutputStream()) {
            os.write(listRequest.getBytes());
            os.flush();
        }

        int responseCode = listConnection.getResponseCode();
        assertEquals(200, responseCode);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(listConnection.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        assertEquals("2.0", jsonResponse.get("jsonrpc").asText());
        assertEquals(31, jsonResponse.get("id").asInt());
        assertNotNull(jsonResponse.get("result"));
        assertEquals(2, jsonResponse.get("result").get("totalCount").asInt());
        assertEquals(2, jsonResponse.get("result").get("page").size());
    }

    @Test
    public void testAuditLogListEmpty() throws Exception {
        String request = """
            {
                "jsonrpc": "2.0",
                "method": "audit/list",
                "params": [{"pageNumber": 1, "pageSize": 10}],
                "id": 32
            }
            """;

        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + testPort).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(request.getBytes());
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        assertEquals("2.0", jsonResponse.get("jsonrpc").asText());
        assertEquals(32, jsonResponse.get("id").asInt());
        assertNotNull(jsonResponse.get("result"));
        assertEquals(0, jsonResponse.get("result").get("totalCount").asInt());
        assertEquals(0, jsonResponse.get("result").get("page").size());
    }
}
