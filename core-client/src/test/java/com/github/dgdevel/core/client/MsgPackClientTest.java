package com.github.dgdevel.core.client;

import com.github.dgdevel.core.server.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MsgPackClientTest {
    private Server server;
    private final int jsonRpcTestPort = 18090;
    private final int msgPackTestPort = 18091;
    private final String testDbUrl = "jdbc:h2:mem:testmsgpack";

    @BeforeEach
    public void setUp() throws Exception {
        server = new Server(jsonRpcTestPort, msgPackTestPort, testDbUrl);
        server.start();
    }

    @AfterEach
    public void tearDown() {
        server.shutdown();
    }

    @Test
    public void testGenericPing() throws Exception {
        Object response = MsgPackClient.sendRequest("localhost", msgPackTestPort, "generic/ping", new Object[]{});
        assertNotNull(response);
        assertTrue(response instanceof String);
        assertTrue(((String) response).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d+"));
    }

    @Test
    public void testInvalidMethod() throws Exception {
        Object response = MsgPackClient.sendRequest("localhost", msgPackTestPort, "invalid/method", new Object[]{});
        assertNotNull(response);
        assertTrue(response instanceof Map);
        Map<?, ?> errorResponse = (Map<?, ?>) response;
        assertNotNull(errorResponse.get("error"));
    }

    @Test
    public void testSetConfigValue() throws Exception {
        Object response = MsgPackClient.sendRequest("localhost", msgPackTestPort, "generic/setConfigValue",
            new Object[]{"test", "key1", "value1"});
        assertNotNull(response);
        assertTrue(response instanceof Map);
        Map<?, ?> result = (Map<?, ?>) response;
        assertTrue(result.containsKey("success"));
        assertTrue((Boolean) result.get("success"));
    }

    @Test
    public void testGetConfigValue() throws Exception {
        MsgPackClient.sendRequest("localhost", msgPackTestPort, "generic/setConfigValue",
            new Object[]{"test", "key1", "value1"});

        Object response = MsgPackClient.sendRequest("localhost", msgPackTestPort, "generic/getConfigValue",
            new Object[]{"test", "key1"});
        assertNotNull(response);
        assertEquals("value1", response);
    }

    @Test
    public void testGetConfigValueNotFound() throws Exception {
        Object response = MsgPackClient.sendRequest("localhost", msgPackTestPort, "generic/getConfigValue",
            new Object[]{"test", "nonexistent"});
        assertNull(response);
    }

    @Test
    public void testLocalize() throws Exception {
        Object response = MsgPackClient.sendRequest("localhost", msgPackTestPort, "generic/localize",
            new Object[]{"greeting", "en", "Hello World"});
        assertNotNull(response);
        assertTrue(response instanceof Map);
        Map<?, ?> result = (Map<?, ?>) response;
        assertTrue(result.containsKey("success"));
        assertTrue((Boolean) result.get("success"));
    }

    @Test
    public void testTranslate() throws Exception {
        MsgPackClient.sendRequest("localhost", msgPackTestPort, "generic/localize",
            new Object[]{"greeting", "en", "Hello World"});

        Object response = MsgPackClient.sendRequest("localhost", msgPackTestPort, "generic/translate",
            new Object[]{"greeting", "en"});
        assertNotNull(response);
        assertEquals("Hello World", response);
    }

    @Test
    public void testTranslateNotFound() throws Exception {
        Object response = MsgPackClient.sendRequest("localhost", msgPackTestPort, "generic/translate",
            new Object[]{"nonexistent", "fr"});
        assertNull(response);
    }

    @Test
    public void testSetConfigValueUpdate() throws Exception {
        MsgPackClient.sendRequest("localhost", msgPackTestPort, "generic/setConfigValue",
            new Object[]{"test", "key1", "value1"});

        MsgPackClient.sendRequest("localhost", msgPackTestPort, "generic/setConfigValue",
            new Object[]{"test", "key1", "updated_value"});

        Object response = MsgPackClient.sendRequest("localhost", msgPackTestPort, "generic/getConfigValue",
            new Object[]{"test", "key1"});
        assertNotNull(response);
        assertEquals("updated_value", response);
    }

    @Test
    public void testNullParams() throws Exception {
        Object response = MsgPackClient.sendRequest("localhost", msgPackTestPort, "generic/ping", new Object[]{});
        assertNotNull(response);
        assertTrue(response instanceof String);
    }

    @Test
    public void testLocalizeUpdate() throws Exception {
        MsgPackClient.sendRequest("localhost", msgPackTestPort, "generic/localize",
            new Object[]{"greeting", "en", "Hello"});

        MsgPackClient.sendRequest("localhost", msgPackTestPort, "generic/localize",
            new Object[]{"greeting", "en", "Hello World"});

        Object response = MsgPackClient.sendRequest("localhost", msgPackTestPort, "generic/translate",
            new Object[]{"greeting", "en"});
        assertNotNull(response);
        assertEquals("Hello World", response);
    }

    @Test
    public void testGetAllConfigValues() throws Exception {
        MsgPackClient.sendRequest("localhost", msgPackTestPort, "generic/setConfigValue",
            new Object[]{"test", "key1", "value1"});

        MsgPackClient.sendRequest("localhost", msgPackTestPort, "generic/setConfigValue",
            new Object[]{"test", "key2", "value2"});

        MsgPackClient.sendRequest("localhost", msgPackTestPort, "generic/setConfigValue",
            new Object[]{"other", "key1", "value3"});

        Object response = MsgPackClient.sendRequest("localhost", msgPackTestPort, "generic/getAllConfigValues",
            new Object[]{});
        assertNotNull(response);
        assertTrue(response instanceof Map);
        Map<?, ?> result = (Map<?, ?>) response;
        assertEquals(3, result.size());
        assertEquals("value1", result.get("test.key1"));
        assertEquals("value2", result.get("test.key2"));
        assertEquals("value3", result.get("other.key1"));
    }

    @Test
    public void testGetAllConfigValuesEmpty() throws Exception {
        Object response = MsgPackClient.sendRequest("localhost", msgPackTestPort, "generic/getAllConfigValues",
            new Object[]{});
        assertNotNull(response);
        assertTrue(response instanceof Map);
        Map<?, ?> result = (Map<?, ?>) response;
        assertEquals(0, result.size());
    }

    @Test
    public void testAuditLog() throws Exception {
        Object response = MsgPackClient.sendRequest("localhost", msgPackTestPort, "audit/log",
            new Object[]{null, "LOGIN", "user logged in"});
        assertNotNull(response);
        assertTrue(response instanceof Map);
        Map<?, ?> result = (Map<?, ?>) response;
        assertTrue(result.containsKey("id"));
        assertTrue(((Number) result.get("id")).longValue() > 0);
    }

    @Test
    public void testAuditLogWithNullUserId() throws Exception {
        Object response = MsgPackClient.sendRequest("localhost", msgPackTestPort, "audit/log",
            new Object[]{null, "SYSTEM", "system event"});
        assertNotNull(response);
        assertTrue(response instanceof Map);
        Map<?, ?> result = (Map<?, ?>) response;
        assertTrue(result.containsKey("id"));
        assertTrue(((Number) result.get("id")).longValue() > 0);
    }

    @Test
    public void testAuditLogList() throws Exception {
        MsgPackClient.sendRequest("localhost", msgPackTestPort, "audit/log",
            new Object[]{null, "LOGIN", "user 1 logged in"});

        MsgPackClient.sendRequest("localhost", msgPackTestPort, "audit/log",
            new Object[]{null, "LOGOUT", "user 2 logged out"});

        Object paginator = Map.of(
            "pageNumber", 1,
            "pageSize", 10
        );

        Object response = MsgPackClient.sendRequest("localhost", msgPackTestPort, "audit/list",
            new Object[]{paginator});
        assertNotNull(response);
        assertTrue(response instanceof Map);
        Map<?, ?> result = (Map<?, ?>) response;
        assertEquals(2L, result.get("totalCount"));
        assertTrue(result.get("page") instanceof List || result.get("page") instanceof Object[]);
    }

    @Test
    public void testAuditLogListEmpty() throws Exception {
        Object paginator = Map.of(
            "pageNumber", 1,
            "pageSize", 10
        );

        Object response = MsgPackClient.sendRequest("localhost", msgPackTestPort, "audit/list",
            new Object[]{paginator});
        assertNotNull(response);
        assertTrue(response instanceof Map);
        Map<?, ?> result = (Map<?, ?>) response;
        assertEquals(0L, result.get("totalCount"));
    }
}
