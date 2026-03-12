package com.github.dgdevel.core.db;

import com.github.dgdevel.core.common.PaginatedList;
import com.github.dgdevel.core.common.Paginator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseManagerTest {
    private DatabaseManager databaseManager;

    @BeforeEach
    public void setUp() throws SQLException {
        databaseManager = new DatabaseManager("jdbc:h2:mem:testdbmgr;DB_CLOSE_DELAY=-1", null, null);
        databaseManager.connect();
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (databaseManager != null) {
            databaseManager.cleanup();
            databaseManager.disconnect();
        }
    }

    @Test
    public void testGetCurrentTimestamp() throws SQLException {
        java.sql.Timestamp timestamp = databaseManager.getCurrentTimestamp();
        assertNotNull(timestamp);
        assertFalse(timestamp.toString().isEmpty());
    }

    @Test
    public void testSetConfigValue() throws SQLException {
        boolean result = databaseManager.setConfigValue("test", "key1", "value1");
        assertTrue(result);
    }

    @Test
    public void testGetConfigValue() throws SQLException {
        databaseManager.setConfigValue("test", "key1", "value1");
        String value = databaseManager.getConfigValue("test", "key1");
        assertEquals("value1", value);
    }

    @Test
    public void testGetConfigValueNotFound() throws SQLException {
        String value = databaseManager.getConfigValue("test", "nonexistent");
        assertNull(value);
    }

    @Test
    public void testUpdateConfigValue() throws SQLException {
        databaseManager.setConfigValue("test", "key1", "value1");
        databaseManager.setConfigValue("test", "key1", "value2");
        String value = databaseManager.getConfigValue("test", "key1");
        assertEquals("value2", value);
    }

    @Test
    public void testSetLocalization() throws SQLException {
        boolean result = databaseManager.setLocalization("greeting", "en", "Hello World");
        assertTrue(result);
    }

    @Test
    public void testGetTranslation() throws SQLException {
        databaseManager.setLocalization("greeting", "en", "Hello World");
        String translation = databaseManager.getTranslation("greeting", "en");
        assertEquals("Hello World", translation);
    }

    @Test
    public void testGetTranslationNotFound() throws SQLException {
        String translation = databaseManager.getTranslation("nonexistent", "en");
        assertNull(translation);
    }

    @Test
    public void testUpdateLocalization() throws SQLException {
        databaseManager.setLocalization("greeting", "en", "Hello");
        databaseManager.setLocalization("greeting", "en", "Hello World");
        String translation = databaseManager.getTranslation("greeting", "en");
        assertEquals("Hello World", translation);
    }

    @Test
    public void testMultipleTranslations() throws SQLException {
        databaseManager.setLocalization("greeting", "en", "Hello");
        databaseManager.setLocalization("greeting", "es", "Hola");
        databaseManager.setLocalization("greeting", "fr", "Bonjour");

        assertEquals("Hello", databaseManager.getTranslation("greeting", "en"));
        assertEquals("Hola", databaseManager.getTranslation("greeting", "es"));
        assertEquals("Bonjour", databaseManager.getTranslation("greeting", "fr"));
    }

    @Test
    public void testMultipleConfigValues() throws SQLException {
        databaseManager.setConfigValue("test", "key1", "value1");
        databaseManager.setConfigValue("test", "key2", "value2");
        databaseManager.setConfigValue("other", "key1", "value3");

        assertEquals("value1", databaseManager.getConfigValue("test", "key1"));
        assertEquals("value2", databaseManager.getConfigValue("test", "key2"));
        assertEquals("value3", databaseManager.getConfigValue("other", "key1"));
    }

    @Test
    public void testGetAllConfigValues() throws SQLException {
        databaseManager.setConfigValue("test", "key1", "value1");
        databaseManager.setConfigValue("test", "key2", "value2");
        databaseManager.setConfigValue("other", "key1", "value3");

        java.util.Map<String, String> allValues = databaseManager.getAllConfigValues();
        assertEquals(3, allValues.size());
        assertEquals("value1", allValues.get("test.key1"));
        assertEquals("value2", allValues.get("test.key2"));
        assertEquals("value3", allValues.get("other.key1"));
    }

    @Test
    public void testGetAllConfigValuesEmpty() throws SQLException {
        java.util.Map<String, String> allValues = databaseManager.getAllConfigValues();
        assertNotNull(allValues);
        assertTrue(allValues.isEmpty());
    }

    @Test
    public void testDisconnectNullConnection() throws SQLException {
        DatabaseManager manager = new DatabaseManager("jdbc:h2:mem:test2", null, null);
        manager.disconnect();
    }

    @Test
    public void testConnectAndDisconnect() throws SQLException {
        DatabaseManager manager = new DatabaseManager("jdbc:h2:mem:test3", null, null);
        manager.connect();
        assertNotNull(manager.getCurrentTimestamp());
        manager.disconnect();
    }

    @Test
    public void testAuditLog() throws SQLException {
        Long id = databaseManager.auditLog(null, "LOGIN", "user logged in", "127.0.0.1");
        assertNotNull(id);
        assertTrue(id > 0);
    }

    @Test
    public void testAuditLogWithNullUserId() throws SQLException {
        Long id = databaseManager.auditLog(null, "SYSTEM", "system event", "192.168.1.1");
        assertNotNull(id);
        assertTrue(id > 0);
    }

    @Test
    public void testAuditLogSameType() throws SQLException {
        Long id1 = databaseManager.auditLog(null, "LOGIN", "first login", "10.0.0.1");
        Long id2 = databaseManager.auditLog(null, "LOGIN", "second login", "10.0.0.2");
        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals(id1, id2);
    }

    @Test
    public void testAuditLogList() throws SQLException {
        databaseManager.auditLog(null, "LOGIN", "user 1 logged in", "127.0.0.1");
        databaseManager.auditLog(null, "LOGOUT", "user 2 logged out", "127.0.0.2");
        databaseManager.auditLog(null, "SYSTEM", "system event", "127.0.0.3");

        Paginator paginator = new Paginator();
        paginator.setPageNumber(1);
        paginator.setPageSize(10);

        PaginatedList result = databaseManager.auditLogList(paginator);
        assertNotNull(result);
        assertEquals(3, result.getTotalCount());
        assertNotNull(result.getPage());
        assertEquals(3, result.getPage().size());
    }

    @Test
    public void testAuditLogListEmpty() throws SQLException {
        Paginator paginator = new Paginator();
        paginator.setPageNumber(1);
        paginator.setPageSize(10);

        PaginatedList result = databaseManager.auditLogList(paginator);
        assertNotNull(result);
        assertEquals(0, result.getTotalCount());
        assertNotNull(result.getPage());
        assertTrue(result.getPage().isEmpty());
    }

    @Test
    public void testAuditLogListWithFilterByTypeCode() throws SQLException {
        databaseManager.auditLog(null, "LOGIN", "user 1 logged in", "127.0.0.1");
        databaseManager.auditLog(null, "LOGOUT", "user 2 logged out", "127.0.0.2");
        databaseManager.auditLog(null, "SYSTEM", "system event", "127.0.0.3");

        Paginator paginator = new Paginator();
        paginator.setPageNumber(1);
        paginator.setPageSize(10);
        paginator.setFilters(java.util.Map.of("type_code", "LOGIN"));

        PaginatedList result = databaseManager.auditLogList(paginator);
        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getPage().size());
    }

    @Test
    public void testCreateRemoteEndpoint() throws SQLException {
        Map<String, Long> result = databaseManager.createRemoteEndpoint("Test Endpoint");
        assertNotNull(result);
        assertTrue(result.containsKey("user_id"));
        assertTrue(result.containsKey("remote_endpoint_id"));
        assertTrue(result.get("user_id") > 0);
        assertTrue(result.get("remote_endpoint_id") > 0);
    }

    @Test
    public void testCreateMultipleRemoteEndpoints() throws SQLException {
        Map<String, Long> result1 = databaseManager.createRemoteEndpoint("Endpoint 1");
        Map<String, Long> result2 = databaseManager.createRemoteEndpoint("Endpoint 2");
        Map<String, Long> result3 = databaseManager.createRemoteEndpoint("Endpoint 3");
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertNotEquals(result1.get("user_id"), result2.get("user_id"));
        assertNotEquals(result2.get("user_id"), result3.get("user_id"));
        assertNotEquals(result1.get("user_id"), result3.get("user_id"));
        assertNotEquals(result1.get("remote_endpoint_id"), result2.get("remote_endpoint_id"));
        assertNotEquals(result2.get("remote_endpoint_id"), result3.get("remote_endpoint_id"));
        assertNotEquals(result1.get("remote_endpoint_id"), result3.get("remote_endpoint_id"));
    }

    @Test
    public void testUpdateRemoteEndpoint() throws SQLException {
        Map<String, Long> result = databaseManager.createRemoteEndpoint("Original Name");
        Long id = result.get("remote_endpoint_id");
        assertTrue(databaseManager.updateRemoteEndpoint(id, "Updated Name"));
    }

    @Test
    public void testUpdateRemoteEndpointNotFound() throws SQLException {
        boolean result = databaseManager.updateRemoteEndpoint(99999L, "Non-existent");
        assertFalse(result);
    }

    @Test
    public void testPingRemoteEndpoint() throws SQLException {
        Map<String, Long> result = databaseManager.createRemoteEndpoint("Test Endpoint");
        Long id = result.get("remote_endpoint_id");
        boolean pingResult = databaseManager.pingRemoteEndpoint(id, "192.168.1.100");
        assertTrue(pingResult);
    }

    @Test
    public void testPingRemoteEndpointNotFound() throws SQLException {
        boolean result = databaseManager.pingRemoteEndpoint(99999L, "192.168.1.100");
        assertFalse(result);
    }

    @Test
    public void testPingRemoteEndpointUpdatesAddress() throws SQLException {
        Map<String, Long> result = databaseManager.createRemoteEndpoint("Test Endpoint");
        Long id = result.get("remote_endpoint_id");
        assertTrue(databaseManager.pingRemoteEndpoint(id, "192.168.1.100"));
        assertTrue(databaseManager.pingRemoteEndpoint(id, "192.168.1.101"));
    }

    @Test
    public void testUpdateRemoteEndpointUpdatesUser() throws SQLException {
        Map<String, Long> result = databaseManager.createRemoteEndpoint("Original Name");
        Long endpointId = result.get("remote_endpoint_id");
        Long userId = result.get("user_id");

        String sql = "SELECT display_name FROM users WHERE id = ?";
        try (PreparedStatement stmt = databaseManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    assertEquals("Original Name", rs.getString("display_name"));
                }
            }
        }

        assertTrue(databaseManager.updateRemoteEndpoint(endpointId, "Updated Name"));

        try (PreparedStatement stmt2 = databaseManager.getConnection().prepareStatement(sql)) {
            stmt2.setLong(1, userId);
            try (ResultSet rs2 = stmt2.executeQuery()) {
                if (rs2.next()) {
                    assertEquals("Updated Name", rs2.getString("display_name"));
                }
            }
        }
    }
}
