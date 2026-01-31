package com.github.dgdevel.core.registry;

import com.github.dgdevel.core.db.DatabaseManager;
import com.github.dgdevel.core.model.Role;
import com.github.dgdevel.core.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AuthorizationRegistryTest {
    private DatabaseManager databaseManager;
    private com.github.dgdevel.core.registry.UserRegistry userRegistry;
    private AuthorizationRegistry authorizationRegistry;

    @BeforeEach
    public void setUp() throws SQLException {
        databaseManager = new DatabaseManager("jdbc:h2:mem:testauth;DB_CLOSE_DELAY=-1");
        databaseManager.connect();
        userRegistry = new UserRegistry(databaseManager.getConnection());
        authorizationRegistry = new AuthorizationRegistry(databaseManager.getConnection());
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (databaseManager != null) {
            databaseManager.cleanup();
            databaseManager.disconnect();
        }
    }

    @Test
    public void testCreateRole() throws SQLException {
        Role role = new Role();
        role.setCode("ADMIN");
        role.setName("Administrator");

        Long id = authorizationRegistry.create(role);

        assertNotNull(id);
        assertTrue(id > 0);
    }

    @Test
    public void testCreateMultipleRoles() throws SQLException {
        Role role1 = new Role();
        role1.setCode("ADMIN");
        role1.setName("Administrator");

        Role role2 = new Role();
        role2.setCode("USER");
        role2.setName("Regular User");

        Long id1 = authorizationRegistry.create(role1);
        Long id2 = authorizationRegistry.create(role2);

        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals(id1, id2);
    }

    @Test
    public void testUpdateRole() throws SQLException {
        Role role = new Role();
        role.setCode("ADMIN");
        role.setName("Administrator");

        Long id = authorizationRegistry.create(role);
        role.setId(id);
        role.setName("Super Administrator");

        boolean success = authorizationRegistry.update(role);

        assertTrue(success);

        Role updatedRole = authorizationRegistry.findById(id);
        assertEquals("Super Administrator", updatedRole.getName());
    }

    @Test
    public void testUpdateNonExistentRole() throws SQLException {
        Role role = new Role();
        role.setId(999999L);
        role.setCode("NONEXISTENT");
        role.setName("Non Existent");

        boolean success = authorizationRegistry.update(role);

        assertFalse(success);
    }

    @Test
    public void testAuthorizeUser() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Role role = new Role();
        role.setCode("ADMIN");
        role.setName("Administrator");
        Long roleId = authorizationRegistry.create(role);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        Long authId = authorizationRegistry.authorize(userId, roleId, validFrom, validUntil);

        assertNotNull(authId);
        assertTrue(authId > 0);
    }

    @Test
    public void testAuthorizeWithOverlap() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Role role = new Role();
        role.setCode("ADMIN");
        role.setName("Administrator");
        Long roleId = authorizationRegistry.create(role);

        Timestamp validFrom1 = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil1 = new Timestamp(System.currentTimeMillis() + 86400000L);

        authorizationRegistry.authorize(userId, roleId, validFrom1, validUntil1);

        Timestamp validFrom2 = new Timestamp(System.currentTimeMillis() + 3600000L);
        Timestamp validUntil2 = new Timestamp(System.currentTimeMillis() + 90000000L);

        SQLException exception =
            assertThrows(
                SQLException.class,
                () -> authorizationRegistry.authorize(userId, roleId, validFrom2, validUntil2));
        assertTrue(exception.getMessage().contains("overlaps"));
    }

    @Test
    public void testAuthorizeNonOverlapping() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Role role = new Role();
        role.setCode("ADMIN");
        role.setName("Administrator");
        Long roleId = authorizationRegistry.create(role);

        Timestamp validFrom1 = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil1 = new Timestamp(System.currentTimeMillis() + 86400000L);

        authorizationRegistry.authorize(userId, roleId, validFrom1, validUntil1);

        Timestamp validFrom2 = new Timestamp(System.currentTimeMillis() + 90000000L);
        Timestamp validUntil2 = new Timestamp(System.currentTimeMillis() + 172800000L);

        Long authId = authorizationRegistry.authorize(userId, roleId, validFrom2, validUntil2);

        assertNotNull(authId);
        assertTrue(authId > 0);
    }

    @Test
    public void testDeauthorizeUser() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Role role = new Role();
        role.setCode("ADMIN");
        role.setName("Administrator");
        Long roleId = authorizationRegistry.create(role);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        authorizationRegistry.authorize(userId, roleId, validFrom, validUntil);

        boolean success = authorizationRegistry.deauthorize(userId, roleId);

        assertTrue(success);
    }

    @Test
    public void testDeauthorizeNonExistent() throws SQLException {
        boolean success = authorizationRegistry.deauthorize(999999L, 999999L);

        assertFalse(success);
    }

    @Test
    public void testIsUserInRoleActive() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Role role = new Role();
        role.setCode("ADMIN");
        role.setName("Administrator");
        Long roleId = authorizationRegistry.create(role);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        authorizationRegistry.authorize(userId, roleId, validFrom, validUntil);

        boolean result = authorizationRegistry.isUserInRole(userId, roleId);

        assertTrue(result);
    }

    @Test
    public void testIsUserInRoleNotActive() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Role role = new Role();
        role.setCode("ADMIN");
        role.setName("Administrator");
        Long roleId = authorizationRegistry.create(role);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis() - 86400000L);
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() - 3600000L);

        authorizationRegistry.authorize(userId, roleId, validFrom, validUntil);

        boolean result = authorizationRegistry.isUserInRole(userId, roleId);

        assertFalse(result);
    }

    @Test
    public void testIsUserInRoleNeverAuthorized() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Role role = new Role();
        role.setCode("ADMIN");
        role.setName("Administrator");
        Long roleId = authorizationRegistry.create(role);

        boolean result = authorizationRegistry.isUserInRole(userId, roleId);

        assertFalse(result);
    }

    @Test
    public void testIsUserInAnyRoles() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Role role1 = new Role();
        role1.setCode("ADMIN");
        role1.setName("Administrator");
        Long roleId1 = authorizationRegistry.create(role1);

        Role role2 = new Role();
        role2.setCode("USER");
        role2.setName("Regular User");
        Long roleId2 = authorizationRegistry.create(role2);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        authorizationRegistry.authorize(userId, roleId1, validFrom, validUntil);

        List<Long> roleIds = new ArrayList<>();
        roleIds.add(roleId1);
        roleIds.add(roleId2);

        boolean result = authorizationRegistry.isUserInAnyRoles(userId, roleIds);

        assertTrue(result);
    }

    @Test
    public void testIsUserInAnyRolesNoneActive() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Role role1 = new Role();
        role1.setCode("ADMIN");
        role1.setName("Administrator");
        Long roleId1 = authorizationRegistry.create(role1);

        Role role2 = new Role();
        role2.setCode("USER");
        role2.setName("Regular User");
        Long roleId2 = authorizationRegistry.create(role2);

        List<Long> roleIds = new ArrayList<>();
        roleIds.add(roleId1);
        roleIds.add(roleId2);

        boolean result = authorizationRegistry.isUserInAnyRoles(userId, roleIds);

        assertFalse(result);
    }

    @Test
    public void testIsUserInAnyRolesEmptyList() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        List<Long> roleIds = new ArrayList<>();

        boolean result = authorizationRegistry.isUserInAnyRoles(userId, roleIds);

        assertFalse(result);
    }

    @Test
    public void testIsUserInAllRoles() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Role role1 = new Role();
        role1.setCode("ADMIN");
        role1.setName("Administrator");
        Long roleId1 = authorizationRegistry.create(role1);

        Role role2 = new Role();
        role2.setCode("USER");
        role2.setName("Regular User");
        Long roleId2 = authorizationRegistry.create(role2);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        authorizationRegistry.authorize(userId, roleId1, validFrom, validUntil);
        authorizationRegistry.authorize(userId, roleId2, validFrom, validUntil);

        List<Long> roleIds = new ArrayList<>();
        roleIds.add(roleId1);
        roleIds.add(roleId2);

        boolean result = authorizationRegistry.isUserInAllRoles(userId, roleIds);

        assertTrue(result);
    }

    @Test
    public void testIsUserInAllRolesNotAllActive() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Role role1 = new Role();
        role1.setCode("ADMIN");
        role1.setName("Administrator");
        Long roleId1 = authorizationRegistry.create(role1);

        Role role2 = new Role();
        role2.setCode("USER");
        role2.setName("Regular User");
        Long roleId2 = authorizationRegistry.create(role2);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        authorizationRegistry.authorize(userId, roleId1, validFrom, validUntil);

        List<Long> roleIds = new ArrayList<>();
        roleIds.add(roleId1);
        roleIds.add(roleId2);

        boolean result = authorizationRegistry.isUserInAllRoles(userId, roleIds);

        assertFalse(result);
    }

    @Test
    public void testIsUserInAllRolesEmptyList() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        List<Long> roleIds = new ArrayList<>();

        boolean result = authorizationRegistry.isUserInAllRoles(userId, roleIds);

        assertTrue(result);
    }

    @Test
    public void testFindById() throws SQLException {
        Role role = new Role();
        role.setCode("ADMIN");
        role.setName("Administrator");

        Long id = authorizationRegistry.create(role);

        Role foundRole = authorizationRegistry.findById(id);

        assertNotNull(foundRole);
        assertEquals(id, foundRole.getId());
        assertEquals("ADMIN", foundRole.getCode());
        assertEquals("Administrator", foundRole.getName());
    }

    @Test
    public void testFindByIdNotFound() throws SQLException {
        Role foundRole = authorizationRegistry.findById(999999L);

        assertNull(foundRole);
    }

    @Test
    public void testFindByCode() throws SQLException {
        Role role = new Role();
        role.setCode("ADMIN");
        role.setName("Administrator");

        Long id = authorizationRegistry.create(role);

        Role foundRole = authorizationRegistry.findByCode("ADMIN");

        assertNotNull(foundRole);
        assertEquals(id, foundRole.getId());
        assertEquals("ADMIN", foundRole.getCode());
        assertEquals("Administrator", foundRole.getName());
    }

    @Test
    public void testFindByCodeNotFound() throws SQLException {
        Role foundRole = authorizationRegistry.findByCode("NONEXISTENT");

        assertNull(foundRole);
    }

    @Test
    public void testFindAll() throws SQLException {
        Role role1 = new Role();
        role1.setCode("ADMIN");
        role1.setName("Administrator");

        Role role2 = new Role();
        role2.setCode("USER");
        role2.setName("Regular User");

        authorizationRegistry.create(role1);
        authorizationRegistry.create(role2);

        List<Role> roles = authorizationRegistry.findAll();

        assertEquals(2, roles.size());
    }

    @Test
    public void testFindAllEmpty() throws SQLException {
        List<Role> roles = authorizationRegistry.findAll();

        assertEquals(0, roles.size());
    }

    @Test
    public void testFullLifecycle() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Role role = new Role();
        role.setCode("ADMIN");
        role.setName("Administrator");
        Long roleId = authorizationRegistry.create(role);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        Long authId = authorizationRegistry.authorize(userId, roleId, validFrom, validUntil);
        assertNotNull(authId);

        boolean isInRole = authorizationRegistry.isUserInRole(userId, roleId);
        assertTrue(isInRole);

        boolean deauthSuccess = authorizationRegistry.deauthorize(userId, roleId);
        assertTrue(deauthSuccess);

        boolean isNotInRole = authorizationRegistry.isUserInRole(userId, roleId);
        assertFalse(isNotInRole);
    }

    @Test
    public void testMultipleAuthorizationsDifferentUsers() throws SQLException {
        User user1 = new User();
        user1.setDisplayName("User 1");
        user1.setActive(true);
        Long userId1 = userRegistry.create(user1);

        User user2 = new User();
        user2.setDisplayName("User 2");
        user2.setActive(true);
        Long userId2 = userRegistry.create(user2);

        Role role = new Role();
        role.setCode("ADMIN");
        role.setName("Administrator");
        Long roleId = authorizationRegistry.create(role);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        authorizationRegistry.authorize(userId1, roleId, validFrom, validUntil);
        authorizationRegistry.authorize(userId2, roleId, validFrom, validUntil);

        assertTrue(authorizationRegistry.isUserInRole(userId1, roleId));
        assertTrue(authorizationRegistry.isUserInRole(userId2, roleId));
    }

    @Test
    public void testMultipleAuthorizationsDifferentRoles() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Role role1 = new Role();
        role1.setCode("ADMIN");
        role1.setName("Administrator");
        Long roleId1 = authorizationRegistry.create(role1);

        Role role2 = new Role();
        role2.setCode("USER");
        role2.setName("Regular User");
        Long roleId2 = authorizationRegistry.create(role2);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        authorizationRegistry.authorize(userId, roleId1, validFrom, validUntil);
        authorizationRegistry.authorize(userId, roleId2, validFrom, validUntil);

        assertTrue(authorizationRegistry.isUserInRole(userId, roleId1));
        assertTrue(authorizationRegistry.isUserInRole(userId, roleId2));
    }

    @Test
    public void testAuthorizationPersistAfterRoleUpdate() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Role role = new Role();
        role.setCode("ADMIN");
        role.setName("Administrator");
        Long roleId = authorizationRegistry.create(role);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        authorizationRegistry.authorize(userId, roleId, validFrom, validUntil);

        role.setId(roleId);
        role.setName("Super Administrator");
        authorizationRegistry.update(role);

        assertTrue(authorizationRegistry.isUserInRole(userId, roleId));
    }

    @Test
    public void testCreateRoleWithParent() throws SQLException {
        Role parentRole = new Role();
        parentRole.setCode("ADMIN");
        parentRole.setName("Administrator");
        Long parentId = authorizationRegistry.create(parentRole);

        Role childRole = new Role();
        childRole.setCode("ADMIN_READONLY");
        childRole.setName("Read Only Admin");
        childRole.setParentCode("ADMIN");
        Long childId = authorizationRegistry.create(childRole);

        assertNotNull(childId);
        assertTrue(childId > 0);
        assertNotEquals(parentId, childId);

        Role retrievedChild = authorizationRegistry.findById(childId);
        assertEquals(parentId, retrievedChild.getParentId());
        assertEquals("ADMIN", retrievedChild.getParentCode());
    }

    @Test
    public void testCreateRoleWithNonExistentParent() throws SQLException {
        Role childRole = new Role();
        childRole.setCode("ADMIN_READONLY");
        childRole.setName("Read Only Admin");
        childRole.setParentCode("NONEXISTENT");

        SQLException exception = assertThrows(SQLException.class, () -> authorizationRegistry.create(childRole));
        assertTrue(exception.getMessage().contains("Parent role"));
    }

    @Test
    public void testUpdateRoleParent() throws SQLException {
        Role parentRole = new Role();
        parentRole.setCode("ADMIN");
        parentRole.setName("Administrator");
        Long parentId = authorizationRegistry.create(parentRole);

        Role childRole = new Role();
        childRole.setCode("ADMIN_READONLY");
        childRole.setName("Read Only Admin");
        Long childId = authorizationRegistry.create(childRole);

        childRole.setId(childId);
        childRole.setParentCode("ADMIN");
        authorizationRegistry.update(childRole);

        Role retrievedChild = authorizationRegistry.findById(childId);
        assertEquals(parentId, retrievedChild.getParentId());
        assertEquals("ADMIN", retrievedChild.getParentCode());
    }

    @Test
    public void testUpdateRoleWithSelfAsParent() throws SQLException {
        Role role = new Role();
        role.setCode("ADMIN");
        role.setName("Administrator");
        Long roleId = authorizationRegistry.create(role);

        role.setId(roleId);
        role.setParentCode("ADMIN");

        SQLException exception = assertThrows(SQLException.class, () -> authorizationRegistry.update(role));
        assertTrue(exception.getMessage().contains("own parent"));
    }

    @Test
    public void testIsUserInRoleWithHierarchy() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Role parentRole = new Role();
        parentRole.setCode("ADMIN");
        parentRole.setName("Administrator");
        Long parentRoleId = authorizationRegistry.create(parentRole);

        Role childRole = new Role();
        childRole.setCode("ADMIN_READONLY");
        childRole.setName("Read Only Admin");
        childRole.setParentCode("ADMIN");
        Long childRoleId = authorizationRegistry.create(childRole);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        authorizationRegistry.authorize(userId, parentRoleId, validFrom, validUntil);

        assertTrue(authorizationRegistry.isUserInRole(userId, parentRoleId));
        assertTrue(authorizationRegistry.isUserInRole(userId, childRoleId));
    }

    @Test
    public void testIsUserInRoleChildNotInParent() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Role parentRole = new Role();
        parentRole.setCode("ADMIN");
        parentRole.setName("Administrator");
        Long parentRoleId = authorizationRegistry.create(parentRole);

        Role childRole = new Role();
        childRole.setCode("ADMIN_READONLY");
        childRole.setName("Read Only Admin");
        childRole.setParentCode("ADMIN");
        Long childRoleId = authorizationRegistry.create(childRole);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        authorizationRegistry.authorize(userId, childRoleId, validFrom, validUntil);

        assertTrue(authorizationRegistry.isUserInRole(userId, childRoleId));
        assertFalse(authorizationRegistry.isUserInRole(userId, parentRoleId));
    }

    @Test
    public void testIsUserInAnyRolesWithHierarchy() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Role parentRole = new Role();
        parentRole.setCode("ADMIN");
        parentRole.setName("Administrator");
        Long parentRoleId = authorizationRegistry.create(parentRole);

        Role childRole1 = new Role();
        childRole1.setCode("ADMIN_READONLY");
        childRole1.setName("Read Only Admin");
        childRole1.setParentCode("ADMIN");
        Long childRoleId1 = authorizationRegistry.create(childRole1);

        Role childRole2 = new Role();
        childRole2.setCode("ADMIN_WRITE");
        childRole2.setName("Write Admin");
        childRole2.setParentCode("ADMIN");
        Long childRoleId2 = authorizationRegistry.create(childRole2);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        authorizationRegistry.authorize(userId, parentRoleId, validFrom, validUntil);

        List<Long> roleIds = new ArrayList<>();
        roleIds.add(childRoleId1);
        roleIds.add(childRoleId2);

        assertTrue(authorizationRegistry.isUserInAnyRoles(userId, roleIds));
    }

    @Test
    public void testIsUserInAllRolesWithHierarchy() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Role parentRole = new Role();
        parentRole.setCode("ADMIN");
        parentRole.setName("Administrator");
        Long parentRoleId = authorizationRegistry.create(parentRole);

        Role childRole1 = new Role();
        childRole1.setCode("ADMIN_READONLY");
        childRole1.setName("Read Only Admin");
        childRole1.setParentCode("ADMIN");
        Long childRoleId1 = authorizationRegistry.create(childRole1);

        Role childRole2 = new Role();
        childRole2.setCode("ADMIN_WRITE");
        childRole2.setName("Write Admin");
        childRole2.setParentCode("ADMIN");
        Long childRoleId2 = authorizationRegistry.create(childRole2);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        authorizationRegistry.authorize(userId, parentRoleId, validFrom, validUntil);

        List<Long> roleIds = new ArrayList<>();
        roleIds.add(childRoleId1);
        roleIds.add(childRoleId2);

        assertTrue(authorizationRegistry.isUserInAllRoles(userId, roleIds));
    }

    @Test
    public void testMultiLevelHierarchy() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Role grandparentRole = new Role();
        grandparentRole.setCode("SUPER_ADMIN");
        grandparentRole.setName("Super Administrator");
        Long grandparentRoleId = authorizationRegistry.create(grandparentRole);

        Role parentRole = new Role();
        parentRole.setCode("ADMIN");
        parentRole.setName("Administrator");
        parentRole.setParentCode("SUPER_ADMIN");
        Long parentRoleId = authorizationRegistry.create(parentRole);

        Role childRole = new Role();
        childRole.setCode("ADMIN_READONLY");
        childRole.setName("Read Only Admin");
        childRole.setParentCode("ADMIN");
        Long childRoleId = authorizationRegistry.create(childRole);

        Timestamp validFrom = new Timestamp(System.currentTimeMillis());
        Timestamp validUntil = new Timestamp(System.currentTimeMillis() + 86400000L);

        authorizationRegistry.authorize(userId, grandparentRoleId, validFrom, validUntil);

        assertTrue(authorizationRegistry.isUserInRole(userId, grandparentRoleId));
        assertTrue(authorizationRegistry.isUserInRole(userId, parentRoleId));
        assertTrue(authorizationRegistry.isUserInRole(userId, childRoleId));
    }

    @Test
    public void testFindByIdReturnsParent() throws SQLException {
        Role parentRole = new Role();
        parentRole.setCode("ADMIN");
        parentRole.setName("Administrator");
        Long parentId = authorizationRegistry.create(parentRole);

        Role childRole = new Role();
        childRole.setCode("ADMIN_READONLY");
        childRole.setName("Read Only Admin");
        childRole.setParentCode("ADMIN");
        Long childId = authorizationRegistry.create(childRole);

        Role foundRole = authorizationRegistry.findById(childId);

        assertNotNull(foundRole);
        assertEquals(childId, foundRole.getId());
        assertEquals("ADMIN_READONLY", foundRole.getCode());
        assertEquals(parentId, foundRole.getParentId());
        assertEquals("ADMIN", foundRole.getParentCode());
    }

    @Test
    public void testFindByCodeReturnsParent() throws SQLException {
        Role parentRole = new Role();
        parentRole.setCode("ADMIN");
        parentRole.setName("Administrator");
        authorizationRegistry.create(parentRole);

        Role childRole = new Role();
        childRole.setCode("ADMIN_READONLY");
        childRole.setName("Read Only Admin");
        childRole.setParentCode("ADMIN");
        authorizationRegistry.create(childRole);

        Role foundRole = authorizationRegistry.findByCode("ADMIN_READONLY");

        assertNotNull(foundRole);
        assertEquals("ADMIN_READONLY", foundRole.getCode());
        assertNotNull(foundRole.getParentId());
        assertEquals("ADMIN", foundRole.getParentCode());
    }

    @Test
    public void testFindAllReturnsParent() throws SQLException {
        Role parentRole = new Role();
        parentRole.setCode("ADMIN");
        parentRole.setName("Administrator");
        Long parentId = authorizationRegistry.create(parentRole);

        Role childRole = new Role();
        childRole.setCode("ADMIN_READONLY");
        childRole.setName("Read Only Admin");
        childRole.setParentCode("ADMIN");
        authorizationRegistry.create(childRole);

        List<Role> roles = authorizationRegistry.findAll();

        assertEquals(2, roles.size());

        Role child = roles.stream().filter(r -> r.getCode().equals("ADMIN_READONLY")).findFirst().orElse(null);
        assertNotNull(child);
        assertEquals(parentId, child.getParentId());
        assertEquals("ADMIN", child.getParentCode());
    }
}
