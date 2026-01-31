package com.github.dgdevel.core.registry;

import com.github.dgdevel.core.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AuthenticationRegistryTest {
    private Connection connection;
    private AuthenticationRegistry authenticationRegistry;
    private com.github.dgdevel.core.registry.UserRegistry userRegistry;

    @BeforeEach
    public void setUp() throws SQLException {
        String uniqueId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String testDbUrl = "jdbc:h2:mem:authtest" + uniqueId;
        connection = DriverManager.getConnection(testDbUrl);
        authenticationRegistry = new AuthenticationRegistry(connection);
        userRegistry = new UserRegistry(connection);
        initializeSchema();
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    private void initializeSchema() throws SQLException {
        connection.createStatement().executeUpdate(
            "CREATE TABLE IF NOT EXISTS users ("
                + "id IDENTITY PRIMARY KEY, "
                + "display_name VARCHAR(255) NOT NULL, "
                + "active BOOLEAN DEFAULT TRUE, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ")");

        connection.createStatement().executeUpdate(
            "CREATE TABLE IF NOT EXISTS credential_type ("
                + "id IDENTITY PRIMARY KEY, "
                + "code VARCHAR(255) NOT NULL, "
                + "only_one_per_user_id BOOLEAN DEFAULT FALSE, "
                + "only_one_security_principal BOOLEAN DEFAULT FALSE, "
                + "UNIQUE (code)"
                + ")");

        connection.createStatement().executeUpdate(
            "CREATE TABLE IF NOT EXISTS credentials ("
                + "id IDENTITY PRIMARY KEY, "
                + "user_id BIGINT NOT NULL, "
                + "credential_type_id BIGINT NOT NULL, "
                + "valid_from TIMESTAMP NOT NULL, "
                + "valid_until TIMESTAMP NOT NULL, "
                + "security_principal VARCHAR(255) NOT NULL, "
                + "security_credentials VARCHAR(1000), "
                + "FOREIGN KEY (credential_type_id) REFERENCES credential_type(id) ON DELETE CASCADE, "
                + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE"
                + ")");
    }

    @Test
    public void testRegisterType() throws SQLException {
        authenticationRegistry.registerType("BASIC", true, true);

        long id = getCredentialTypeId("BASIC");
        assertTrue(id > 0);
    }

    @Test
    public void testRegisterTypeNoErrorIfAlreadyPresent() throws SQLException {
        authenticationRegistry.registerType("TOKEN", false, true);
        authenticationRegistry.registerType("TOKEN", false, true);

        long id = getCredentialTypeId("TOKEN");
        assertTrue(id > 0);
    }

    @Test
    public void testCreateCredential() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        authenticationRegistry.registerType("PASSWORD", true, false);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        Long credentialId =
            authenticationRegistry.create(
                userId, "PASSWORD", validFrom, validUntil, "testuser", "hashedpassword");

        assertNotNull(credentialId);
        assertTrue(credentialId > 0);
    }

    @Test
    public void testCreateCredentialWithNullSecurityCredentials() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        authenticationRegistry.registerType("TOKEN", false, true);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        Long credentialId =
            authenticationRegistry.create(userId, "TOKEN", validFrom, validUntil, "abc123", null);

        assertNotNull(credentialId);
        assertTrue(credentialId > 0);
    }

    @Test
    public void testCreateCredentialOnlyOnePerUserId() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        authenticationRegistry.registerType("BASIC", true, false);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        Long credentialId1 =
            authenticationRegistry.create(userId, "BASIC", validFrom, validUntil, "user1", "pass1");
        assertNotNull(credentialId1);

        Long credentialId2 =
            authenticationRegistry.create(userId, "BASIC", validFrom, validUntil, "user2", "pass2");
        assertNotNull(credentialId2);

        Long verifiedUserId = authenticationRegistry.verify("BASIC", "user2", "pass2");
        assertEquals(userId, verifiedUserId);

        verifiedUserId = authenticationRegistry.verify("BASIC", "user1", "pass1");
        assertNull(verifiedUserId);
    }

    @Test
    public void testCreateCredentialOnlyOneSecurityPrincipal() throws SQLException {
        User user1 = new User();
        user1.setDisplayName("Test User 1");
        user1.setActive(true);
        Long userId1 = userRegistry.create(user1);

        User user2 = new User();
        user2.setDisplayName("Test User 2");
        user2.setActive(true);
        Long userId2 = userRegistry.create(user2);

        authenticationRegistry.registerType("UNIQUE_TOKEN", false, true);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        Long credentialId1 =
            authenticationRegistry.create(userId1, "UNIQUE_TOKEN", validFrom, validUntil, "token123", null);
        assertNotNull(credentialId1);

        Long credentialId2 =
            authenticationRegistry.create(userId2, "UNIQUE_TOKEN", validFrom, validUntil, "token123", null);
        assertNotNull(credentialId2);

        Long verifiedUserId = authenticationRegistry.verify("UNIQUE_TOKEN", "token123", null);
        assertEquals(userId2, verifiedUserId);
    }

    @Test
    public void testCreateCredentialNonExistentType() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        assertThrows(
            SQLException.class,
            () ->
                authenticationRegistry.create(
                    userId, "NONEXISTENT", validFrom, validUntil, "user", "pass"));
    }

    @Test
    public void testVerifyCredential() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        authenticationRegistry.registerType("PASSWORD", false, false);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        authenticationRegistry.create(userId, "PASSWORD", validFrom, validUntil, "testuser", "hashedpass");

        Long verifiedUserId = authenticationRegistry.verify("PASSWORD", "testuser", "hashedpass");
        assertEquals(userId, verifiedUserId);
    }

    @Test
    public void testVerifyCredentialWithNullCredentials() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        authenticationRegistry.registerType("TOKEN", false, false);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        authenticationRegistry.create(userId, "TOKEN", validFrom, validUntil, "abc123", null);

        Long verifiedUserId = authenticationRegistry.verify("TOKEN", "abc123", null);
        assertEquals(userId, verifiedUserId);
    }

    @Test
    public void testVerifyCredentialInvalid() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        authenticationRegistry.registerType("PASSWORD", false, false);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        authenticationRegistry.create(userId, "PASSWORD", validFrom, validUntil, "testuser", "hashedpass");

        Long verifiedUserId = authenticationRegistry.verify("PASSWORD", "testuser", "wrongpass");
        assertNull(verifiedUserId);
    }

    @Test
    public void testVerifyCredentialExpired() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        authenticationRegistry.registerType("PASSWORD", false, false);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis() - 172800000L);
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() - 86400000L);

        authenticationRegistry.create(userId, "PASSWORD", validFrom, validUntil, "testuser", "hashedpass");

        Long verifiedUserId = authenticationRegistry.verify("PASSWORD", "testuser", "hashedpass");
        assertNull(verifiedUserId);
    }

    @Test
    public void testVerifyCredentialNotYetValid() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        authenticationRegistry.registerType("PASSWORD", false, false);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis() + 86400000L);
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 172800000L);

        authenticationRegistry.create(userId, "PASSWORD", validFrom, validUntil, "testuser", "hashedpass");

        Long verifiedUserId = authenticationRegistry.verify("PASSWORD", "testuser", "hashedpass");
        assertNull(verifiedUserId);
    }

    @Test
    public void testVerifyCredentialNonExistentType() throws SQLException {
        Long verifiedUserId = authenticationRegistry.verify("NONEXISTENT", "user", "pass");
        assertNull(verifiedUserId);
    }

    @Test
    public void testExpireOne() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        authenticationRegistry.registerType("PASSWORD", false, false);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        authenticationRegistry.create(userId, "PASSWORD", validFrom, validUntil, "testuser", "pass1");
        authenticationRegistry.create(userId, "PASSWORD", validFrom, validUntil, "testuser2", "pass2");

        boolean success = authenticationRegistry.expireOne(userId, "PASSWORD", "testuser");
        assertTrue(success);

        Long verifiedUserId = authenticationRegistry.verify("PASSWORD", "testuser", "pass1");
        assertNull(verifiedUserId);

        verifiedUserId = authenticationRegistry.verify("PASSWORD", "testuser2", "pass2");
        assertEquals(userId, verifiedUserId);
    }

    @Test
    public void testExpireOneNonExistent() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        authenticationRegistry.registerType("PASSWORD", false, false);

        boolean success = authenticationRegistry.expireOne(userId, "PASSWORD", "nonexistent");
        assertFalse(success);
    }

    @Test
    public void testExpireOneNonExistentType() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        assertThrows(
            SQLException.class, () -> authenticationRegistry.expireOne(userId, "NONEXISTENT", "user"));
    }

    @Test
    public void testExpireAll() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        authenticationRegistry.registerType("PASSWORD", false, false);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        authenticationRegistry.create(userId, "PASSWORD", validFrom, validUntil, "testuser1", "pass1");
        authenticationRegistry.create(userId, "PASSWORD", validFrom, validUntil, "testuser2", "pass2");
        authenticationRegistry.create(userId, "PASSWORD", validFrom, validUntil, "testuser3", "pass3");

        boolean success = authenticationRegistry.expireAll(userId, "PASSWORD");
        assertTrue(success);

        assertNull(authenticationRegistry.verify("PASSWORD", "testuser1", "pass1"));
        assertNull(authenticationRegistry.verify("PASSWORD", "testuser2", "pass2"));
        assertNull(authenticationRegistry.verify("PASSWORD", "testuser3", "pass3"));
    }

    @Test
    public void testExpireAllNoCredentials() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        authenticationRegistry.registerType("PASSWORD", false, false);

        boolean success = authenticationRegistry.expireAll(userId, "PASSWORD");
        assertFalse(success);
    }

    @Test
    public void testExpireAllNonExistentType() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        assertThrows(
            SQLException.class, () -> authenticationRegistry.expireAll(userId, "NONEXISTENT"));
    }

    @Test
    public void testMultipleUsersSameCredentialType() throws SQLException {
        User user1 = new User();
        user1.setDisplayName("Test User 1");
        user1.setActive(true);
        Long userId1 = userRegistry.create(user1);

        User user2 = new User();
        user2.setDisplayName("Test User 2");
        user2.setActive(true);
        Long userId2 = userRegistry.create(user2);

        authenticationRegistry.registerType("PASSWORD", true, false);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        authenticationRegistry.create(userId1, "PASSWORD", validFrom, validUntil, "user1", "pass1");
        authenticationRegistry.create(userId2, "PASSWORD", validFrom, validUntil, "user2", "pass2");

        assertEquals(userId1, authenticationRegistry.verify("PASSWORD", "user1", "pass1"));
        assertEquals(userId2, authenticationRegistry.verify("PASSWORD", "user2", "pass2"));
    }

    @Test
    public void testCreateCredentialWithDifferentFlags() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        authenticationRegistry.registerType("TYPE1", true, false);
        authenticationRegistry.registerType("TYPE2", false, true);
        authenticationRegistry.registerType("TYPE3", false, false);
        authenticationRegistry.registerType("TYPE4", true, true);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        Long id1 = authenticationRegistry.create(userId, "TYPE1", validFrom, validUntil, "p1", "c1");
        Long id2 = authenticationRegistry.create(userId, "TYPE2", validFrom, validUntil, "p2", "c2");
        Long id3 = authenticationRegistry.create(userId, "TYPE3", validFrom, validUntil, "p3", "c3");
        Long id4 = authenticationRegistry.create(userId, "TYPE4", validFrom, validUntil, "p4", "c4");

        assertNotNull(id1);
        assertNotNull(id2);
        assertNotNull(id3);
        assertNotNull(id4);
    }

    private long getCredentialTypeId(String code) throws SQLException {
        String sql = "SELECT id FROM credential_type WHERE code = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, code);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
                return -1;
            }
        }
    }
}
