package com.github.dgdevel.core.db;

import com.github.dgdevel.core.common.PaginatedList;
import com.github.dgdevel.core.common.Paginator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseManagerTest {
    private DatabaseManager databaseManager;

    @BeforeEach
    public void setUp() throws SQLException {
        databaseManager = new DatabaseManager("jdbc:h2:mem:testdbmgr;DB_CLOSE_DELAY=-1");
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
        DatabaseManager manager = new DatabaseManager("jdbc:h2:mem:test2");
        manager.disconnect();
    }

    @Test
    public void testConnectAndDisconnect() throws SQLException {
        DatabaseManager manager = new DatabaseManager("jdbc:h2:mem:test3");
        manager.connect();
        assertNotNull(manager.getCurrentTimestamp());
        manager.disconnect();
    }

    @Test
    public void testAuditLog() throws SQLException {
        Long id = databaseManager.auditLog(null, "LOGIN", "user logged in");
        assertNotNull(id);
        assertTrue(id > 0);
    }

    @Test
    public void testAuditLogWithNullUserId() throws SQLException {
        Long id = databaseManager.auditLog(null, "SYSTEM", "system event");
        assertNotNull(id);
        assertTrue(id > 0);
    }

    @Test
    public void testAuditLogSameType() throws SQLException {
        Long id1 = databaseManager.auditLog(null, "LOGIN", "first login");
        Long id2 = databaseManager.auditLog(null, "LOGIN", "second login");
        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals(id1, id2);
    }

    @Test
    public void testAuditLogList() throws SQLException {
        databaseManager.auditLog(null, "LOGIN", "user 1 logged in");
        databaseManager.auditLog(null, "LOGOUT", "user 2 logged out");
        databaseManager.auditLog(null, "SYSTEM", "system event");

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
        databaseManager.auditLog(null, "LOGIN", "user 1 logged in");
        databaseManager.auditLog(null, "LOGOUT", "user 2 logged out");
        databaseManager.auditLog(null, "SYSTEM", "system event");

        Paginator paginator = new Paginator();
        paginator.setPageNumber(1);
        paginator.setPageSize(10);
        paginator.setFilters(java.util.Map.of("type_code", "LOGIN"));

        PaginatedList result = databaseManager.auditLogList(paginator);
        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getPage().size());
    }
}
