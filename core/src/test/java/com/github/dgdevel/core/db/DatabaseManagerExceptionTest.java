package com.github.dgdevel.core.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseManagerExceptionTest {
    private DatabaseManager databaseManager;

    @BeforeEach
    public void setUp() throws SQLException {
        databaseManager = new DatabaseManager("jdbc:h2:mem:test", null, null);
        databaseManager.connect();
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
    }

    @Test
    public void testGetCurrentTimestampAfterDisconnect() throws SQLException {
        databaseManager.disconnect();
        assertThrows(SQLException.class, () -> databaseManager.getCurrentTimestamp());
    }

    @Test
    public void testSetConfigValueAfterDisconnect() throws SQLException {
        databaseManager.disconnect();
        assertThrows(SQLException.class, () -> databaseManager.setConfigValue("test", "key", "value"));
    }

    @Test
    public void testGetConfigValueAfterDisconnect() throws SQLException {
        databaseManager.disconnect();
        assertThrows(SQLException.class, () -> databaseManager.getConfigValue("test", "key"));
    }

    @Test
    public void testSetLocalizationAfterDisconnect() throws SQLException {
        databaseManager.disconnect();
        assertThrows(SQLException.class, () -> databaseManager.setLocalization("key", "en", "translation"));
    }

    @Test
    public void testGetTranslationAfterDisconnect() throws SQLException {
        databaseManager.disconnect();
        assertThrows(SQLException.class, () -> databaseManager.getTranslation("key", "en"));
    }
}
