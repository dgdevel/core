package com.github.dgdevel.core.registry;

import com.github.dgdevel.core.common.PaginatedList;
import com.github.dgdevel.core.common.Paginator;
import com.github.dgdevel.core.db.DatabaseManager;
import com.github.dgdevel.core.model.Address;
import com.github.dgdevel.core.model.AddressType;
import com.github.dgdevel.core.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class UserRegistryTest {
    private DatabaseManager databaseManager;
    private UserRegistry userRegistry;

    @BeforeEach
    public void setUp() throws SQLException {
        databaseManager = new DatabaseManager("jdbc:h2:mem:testuser;DB_CLOSE_DELAY=-1");
        databaseManager.connect();
        userRegistry = new UserRegistry(databaseManager.getConnection());
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (databaseManager != null) {
            databaseManager.cleanup();
            databaseManager.disconnect();
        }
    }

    @Test
    public void testCreateUser() throws SQLException {
        User user = new User();
        user.setDisplayName("Alice Smith");
        user.setActive(true);

        Long id = userRegistry.create(user);

        assertNotNull(id);
        assertTrue(id > 0);
    }

    @Test
    public void testCreateInactiveUser() throws SQLException {
        User user = new User();
        user.setDisplayName("Bob Johnson");
        user.setActive(false);

        Long id = userRegistry.create(user);

        assertNotNull(id);
        assertTrue(id > 0);
    }

    @Test
    public void testCreateMultipleUsers() throws SQLException {
        User user1 = new User();
        user1.setDisplayName("User One");
        user1.setActive(true);

        User user2 = new User();
        user2.setDisplayName("User Two");
        user2.setActive(false);

        Long id1 = userRegistry.create(user1);
        Long id2 = userRegistry.create(user2);

        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals(id1, id2);
    }

    @Test
    public void testUpdateUser() throws SQLException {
        User user = new User();
        user.setDisplayName("Original Name");
        user.setActive(true);

        Long id = userRegistry.create(user);
        user.setId(id);
        user.setDisplayName("Updated Name");
        user.setActive(false);

        boolean success = userRegistry.update(user);

        assertTrue(success);
    }

    @Test
    public void testUpdateNonExistentUser() throws SQLException {
        User user = new User();
        user.setId(999999L);
        user.setDisplayName("Non Existent");
        user.setActive(true);

        boolean success = userRegistry.update(user);

        assertFalse(success);
    }

    @Test
    public void testActivateUser() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(false);

        Long id = userRegistry.create(user);

        boolean success = userRegistry.activate(id);

        assertTrue(success);
    }

    @Test
    public void testActivateNonExistentUser() throws SQLException {
        boolean success = userRegistry.activate(999999L);

        assertFalse(success);
    }

    @Test
    public void testDeactivateUser() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);

        Long id = userRegistry.create(user);

        boolean success = userRegistry.deactivate(id);

        assertTrue(success);
    }

    @Test
    public void testDeactivateNonExistentUser() throws SQLException {
        boolean success = userRegistry.deactivate(999999L);

        assertFalse(success);
    }

    @Test
    public void testFindById() throws SQLException {
        User originalUser = new User();
        originalUser.setDisplayName("Find Me");
        originalUser.setActive(true);

        Long id = userRegistry.create(originalUser);

        User foundUser = userRegistry.findById(id);

        assertNotNull(foundUser);
        assertEquals(id, foundUser.getId());
        assertEquals("Find Me", foundUser.getDisplayName());
        assertTrue(foundUser.isActive());
    }

    @Test
    public void testFindByIdNotFound() throws SQLException {
        User foundUser = userRegistry.findById(999999L);

        assertNull(foundUser);
    }

    @Test
    public void testFindByIdAfterUpdate() throws SQLException {
        User originalUser = new User();
        originalUser.setDisplayName("Original");
        originalUser.setActive(false);

        Long id = userRegistry.create(originalUser);
        originalUser.setId(id);
        originalUser.setDisplayName("Updated");
        originalUser.setActive(true);
        userRegistry.update(originalUser);

        User foundUser = userRegistry.findById(id);

        assertNotNull(foundUser);
        assertEquals(id, foundUser.getId());
        assertEquals("Updated", foundUser.getDisplayName());
        assertTrue(foundUser.isActive());
    }

    @Test
    public void testFindByIdAfterDeactivate() throws SQLException {
        User user = new User();
        user.setDisplayName("To Deactivate");
        user.setActive(true);

        Long id = userRegistry.create(user);
        userRegistry.deactivate(id);

        User foundUser = userRegistry.findById(id);

        assertNotNull(foundUser);
        assertEquals(id, foundUser.getId());
        assertEquals("To Deactivate", foundUser.getDisplayName());
        assertFalse(foundUser.isActive());
    }

    @Test
    public void testFindAllWithPagination() throws SQLException {
        for (int i = 1; i <= 25; i++) {
            User user = new User();
            user.setDisplayName("User " + i);
            user.setActive(true);
            userRegistry.create(user);
        }

        Paginator paginator = new Paginator();
        paginator.setPageNumber(1);
        paginator.setPageSize(10);

        PaginatedList<User> result = userRegistry.findBy(paginator);

        assertNotNull(result);
        assertEquals(10, result.getPage().size());
        assertEquals(25, result.getTotalCount());
    }

    @Test
    public void testFindByWithSecondPage() throws SQLException {
        for (int i = 1; i <= 25; i++) {
            User user = new User();
            user.setDisplayName("User " + i);
            user.setActive(true);
            userRegistry.create(user);
        }

        Paginator paginator = new Paginator();
        paginator.setPageNumber(2);
        paginator.setPageSize(10);

        PaginatedList<User> result = userRegistry.findBy(paginator);

        assertNotNull(result);
        assertEquals(10, result.getPage().size());
        assertEquals(25, result.getTotalCount());
    }

    @Test
    public void testFindByWithSortAscending() throws SQLException {
        User user1 = new User();
        user1.setDisplayName("Charlie");
        user1.setActive(true);
        userRegistry.create(user1);

        User user2 = new User();
        user2.setDisplayName("Alice");
        user2.setActive(true);
        userRegistry.create(user2);

        User user3 = new User();
        user3.setDisplayName("Bob");
        user3.setActive(true);
        userRegistry.create(user3);

        Paginator paginator = new Paginator();
        paginator.setPageNumber(1);
        paginator.setPageSize(10);
        paginator.setSortKey("display_name");
        paginator.setSortDirection("ASC");

        PaginatedList<User> result = userRegistry.findBy(paginator);

        assertNotNull(result);
        assertEquals(3, result.getPage().size());
        assertEquals("Alice", result.getPage().get(0).getDisplayName());
        assertEquals("Bob", result.getPage().get(1).getDisplayName());
        assertEquals("Charlie", result.getPage().get(2).getDisplayName());
    }

    @Test
    public void testFindByWithSortDescending() throws SQLException {
        User user1 = new User();
        user1.setDisplayName("Charlie");
        user1.setActive(true);
        userRegistry.create(user1);

        User user2 = new User();
        user2.setDisplayName("Alice");
        user2.setActive(true);
        userRegistry.create(user2);

        User user3 = new User();
        user3.setDisplayName("Bob");
        user3.setActive(true);
        userRegistry.create(user3);

        Paginator paginator = new Paginator();
        paginator.setPageNumber(1);
        paginator.setPageSize(10);
        paginator.setSortKey("display_name");
        paginator.setSortDirection("DESC");

        PaginatedList<User> result = userRegistry.findBy(paginator);

        assertNotNull(result);
        assertEquals(3, result.getPage().size());
        assertEquals("Charlie", result.getPage().get(0).getDisplayName());
        assertEquals("Bob", result.getPage().get(1).getDisplayName());
        assertEquals("Alice", result.getPage().get(2).getDisplayName());
    }

    @Test
    public void testFindByWithFilter() throws SQLException {
        User user1 = new User();
        user1.setDisplayName("Active User");
        user1.setActive(true);
        userRegistry.create(user1);

        User user2 = new User();
        user2.setDisplayName("Inactive User");
        user2.setActive(false);
        userRegistry.create(user2);

        User user3 = new User();
        user3.setDisplayName("Another Active User");
        user3.setActive(true);
        userRegistry.create(user3);

        Paginator paginator = new Paginator();
        paginator.setPageNumber(1);
        paginator.setPageSize(10);
        Map<String, String> filters = new HashMap<>();
        filters.put("active", "true");
        paginator.setFilters(filters);

        PaginatedList<User> result = userRegistry.findBy(paginator);

        assertNotNull(result);
        assertEquals(2, result.getPage().size());
        assertEquals(2, result.getTotalCount());
        for (User user : result.getPage()) {
            assertTrue(user.isActive());
        }
    }

    @Test
    public void testFindByEmptyDatabase() throws SQLException {
        Paginator paginator = new Paginator();
        paginator.setPageNumber(1);
        paginator.setPageSize(10);

        PaginatedList<User> result = userRegistry.findBy(paginator);

        assertNotNull(result);
        assertEquals(0, result.getPage().size());
        assertEquals(0, result.getTotalCount());
    }

    @Test
    public void testFindByBeyondPageBounds() throws SQLException {
        User user = new User();
        user.setDisplayName("Single User");
        user.setActive(true);
        userRegistry.create(user);

        Paginator paginator = new Paginator();
        paginator.setPageNumber(2);
        paginator.setPageSize(10);

        PaginatedList<User> result = userRegistry.findBy(paginator);

        assertNotNull(result);
        assertEquals(0, result.getPage().size());
        assertEquals(1, result.getTotalCount());
    }

    @Test
    public void testFullLifecycle() throws SQLException {
        User user = new User();
        user.setDisplayName("Lifecycle User");
        user.setActive(true);

        Long id = userRegistry.create(user);
        assertNotNull(id);

        User foundUser = userRegistry.findById(id);
        assertNotNull(foundUser);
        assertEquals("Lifecycle User", foundUser.getDisplayName());
        assertTrue(foundUser.isActive());

        foundUser.setDisplayName("Updated User");
        boolean updateSuccess = userRegistry.update(foundUser);
        assertTrue(updateSuccess);

        User updatedUser = userRegistry.findById(id);
        assertNotNull(updatedUser);
        assertEquals("Updated User", updatedUser.getDisplayName());
        assertTrue(updatedUser.isActive());

        boolean deactivateSuccess = userRegistry.deactivate(id);
        assertTrue(deactivateSuccess);

        User deactivatedUser = userRegistry.findById(id);
        assertNotNull(deactivatedUser);
        assertFalse(deactivatedUser.isActive());
    }

    @Test
    public void testCreateUserWithEmptyDisplayName() throws SQLException {
        User user = new User();
        user.setDisplayName("");
        user.setActive(true);

        Long id = userRegistry.create(user);

        assertNotNull(id);
        assertTrue(id > 0);
    }

    @Test
    public void testSetAttribute() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        boolean success = userRegistry.setAttribute(userId, "email", "test@example.com");

        assertTrue(success);
    }

    @Test
    public void testGetAttribute() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);
        userRegistry.setAttribute(userId, "email", "test@example.com");

        String value = userRegistry.getAttribute(userId, "email");

        assertEquals("test@example.com", value);
    }

    @Test
    public void testGetAttributeNotFound() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        String value = userRegistry.getAttribute(userId, "nonexistent");

        assertNull(value);
    }

    @Test
    public void testUpdateAttribute() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);
        userRegistry.setAttribute(userId, "email", "old@example.com");

        boolean success = userRegistry.setAttribute(userId, "email", "new@example.com");

        assertTrue(success);
        assertEquals("new@example.com", userRegistry.getAttribute(userId, "email"));
    }

    @Test
    public void testMultipleAttributes() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        userRegistry.setAttribute(userId, "email", "test@example.com");
        userRegistry.setAttribute(userId, "phone", "123-456-7890");
        userRegistry.setAttribute(userId, "location", "New York");

        assertEquals("test@example.com", userRegistry.getAttribute(userId, "email"));
        assertEquals("123-456-7890", userRegistry.getAttribute(userId, "phone"));
        assertEquals("New York", userRegistry.getAttribute(userId, "location"));
    }

    @Test
    public void testSetAttributeForNonExistentUser() throws SQLException {
        boolean success = userRegistry.setAttribute(999999L, "email", "test@example.com");

        assertFalse(success);
    }

    @Test
    public void testGetAttributeForNonExistentUser() throws SQLException {
        String value = userRegistry.getAttribute(999999L, "email");

        assertNull(value);
    }

    @Test
    public void testSetAttributeWithEmptyValue() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        boolean success = userRegistry.setAttribute(userId, "comment", "");

        assertTrue(success);
        assertEquals("", userRegistry.getAttribute(userId, "comment"));
    }

    @Test
    public void testSetAttributeWithSpecialCharacters() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        String value = "Test with \"quotes\" and 'apostrophes' & symbols!";
        boolean success = userRegistry.setAttribute(userId, "special", value);

        assertTrue(success);
        assertEquals(value, userRegistry.getAttribute(userId, "special"));
    }

    @Test
    public void testAttributesPersistAcrossUserUpdate() throws SQLException {
        User user = new User();
        user.setDisplayName("Original Name");
        user.setActive(true);
        Long userId = userRegistry.create(user);
        userRegistry.setAttribute(userId, "email", "test@example.com");

        user.setId(userId);
        user.setDisplayName("Updated Name");
        userRegistry.update(user);

        assertEquals("test@example.com", userRegistry.getAttribute(userId, "email"));
    }

    @Test
    public void testAddAddress() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Address address = new Address();
        address.setAddressType(AddressType.HOME);
        address.setStreet1("123 Main St");
        address.setCity("Springfield");
        address.setState("IL");
        address.setPostalCode("62701");
        address.setCountry("USA");
        address.setEmail("home@example.com");
        address.setPhone("555-1234");

        Long addressId = userRegistry.addAddress(userId, address);

        assertNotNull(addressId);
        assertTrue(addressId > 0);
    }

    @Test
    public void testAddAddressToNonExistentUser() throws SQLException {
        Address address = new Address();
        address.setAddressType(AddressType.HOME);
        address.setStreet1("123 Main St");
        address.setCity("Springfield");
        address.setState("IL");
        address.setPostalCode("62701");

        SQLException exception = assertThrows(SQLException.class, () -> userRegistry.addAddress(999999L, address));
        assertTrue(exception.getMessage().contains("User not found"));
    }

    @Test
    public void testAddMultipleAddresses() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        List<Address> addresses = new ArrayList<>();
        
        Address homeAddress = new Address();
        homeAddress.setAddressType(AddressType.HOME);
        homeAddress.setStreet1("123 Main St");
        homeAddress.setCity("Springfield");
        homeAddress.setState("IL");
        homeAddress.setPostalCode("62701");
        homeAddress.setCountry("USA");
        addresses.add(homeAddress);

        Address workAddress = new Address();
        workAddress.setAddressType(AddressType.WORK);
        workAddress.setStreet1("456 Office Blvd");
        workAddress.setCity("Chicago");
        workAddress.setState("IL");
        workAddress.setPostalCode("60601");
        workAddress.setCountry("USA");
        addresses.add(workAddress);

        List<Long> addressIds = userRegistry.addAddresses(userId, addresses);

        assertNotNull(addressIds);
        assertEquals(2, addressIds.size());
        assertTrue(addressIds.get(0) > 0);
        assertTrue(addressIds.get(1) > 0);
        assertNotEquals(addressIds.get(0), addressIds.get(1));
    }

    @Test
    public void testGetAddresses() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Address address1 = new Address();
        address1.setAddressType(AddressType.HOME);
        address1.setStreet1("123 Main St");
        address1.setCity("Springfield");
        address1.setState("IL");
        address1.setPostalCode("62701");
        address1.setEmail("home@example.com");

        Address address2 = new Address();
        address2.setAddressType(AddressType.WORK);
        address2.setStreet1("456 Office Blvd");
        address2.setCity("Chicago");
        address2.setState("IL");
        address2.setPostalCode("60601");
        address2.setEmail("work@example.com");

        userRegistry.addAddress(userId, address1);
        userRegistry.addAddress(userId, address2);

        List<Address> addresses = userRegistry.getAddresses(userId);

        assertNotNull(addresses);
        assertEquals(2, addresses.size());
    }

    @Test
    public void testGetAddressesForNonExistentUser() throws SQLException {
        List<Address> addresses = userRegistry.getAddresses(999999L);

        assertNotNull(addresses);
        assertEquals(0, addresses.size());
    }

    @Test
    public void testGetAddressesByType() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Address homeAddress = new Address();
        homeAddress.setAddressType(AddressType.HOME);
        homeAddress.setStreet1("123 Main St");
        homeAddress.setCity("Springfield");
        homeAddress.setState("IL");
        homeAddress.setPostalCode("62701");

        Address workAddress = new Address();
        workAddress.setAddressType(AddressType.WORK);
        workAddress.setStreet1("456 Office Blvd");
        workAddress.setCity("Chicago");
        workAddress.setState("IL");
        workAddress.setPostalCode("60601");

        Address anotherHomeAddress = new Address();
        anotherHomeAddress.setAddressType(AddressType.HOME);
        anotherHomeAddress.setStreet1("789 Beach Ave");
        anotherHomeAddress.setCity("Miami");
        anotherHomeAddress.setState("FL");
        anotherHomeAddress.setPostalCode("33101");

        userRegistry.addAddresses(userId, List.of(homeAddress, workAddress, anotherHomeAddress));

        List<Address> homeAddresses = userRegistry.getAddressesByType(userId, AddressType.HOME);

        assertNotNull(homeAddresses);
        assertEquals(2, homeAddresses.size());
        for (Address address : homeAddresses) {
            assertEquals(AddressType.HOME, address.getAddressType());
        }
    }

    @Test
    public void testUpdateAddress() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Address address = new Address();
        address.setAddressType(AddressType.HOME);
        address.setStreet1("123 Main St");
        address.setCity("Springfield");
        address.setState("IL");
        address.setPostalCode("62701");
        address.setCountry("USA");
        address.setEmail("old@example.com");

        Long addressId = userRegistry.addAddress(userId, address);

        address.setId(addressId);
        address.setStreet1("456 New St");
        address.setCity("New City");
        address.setEmail("new@example.com");

        boolean success = userRegistry.updateAddress(address);

        assertTrue(success);

        List<Address> addresses = userRegistry.getAddresses(userId);
        assertEquals(1, addresses.size());
        assertEquals("456 New St", addresses.get(0).getStreet1());
        assertEquals("New City", addresses.get(0).getCity());
        assertEquals("new@example.com", addresses.get(0).getEmail());
    }

    @Test
    public void testUpdateNonExistentAddress() throws SQLException {
        Address address = new Address();
        address.setId(999999L);
        address.setAddressType(AddressType.HOME);
        address.setStreet1("123 Main St");
        address.setCity("Springfield");
        address.setState("IL");

        boolean success = userRegistry.updateAddress(address);

        assertFalse(success);
    }

    @Test
    public void testDeleteAddress() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Address address = new Address();
        address.setAddressType(AddressType.HOME);
        address.setStreet1("123 Main St");
        address.setCity("Springfield");
        address.setState("IL");
        address.setPostalCode("62701");

        Long addressId = userRegistry.addAddress(userId, address);

        boolean success = userRegistry.deleteAddress(addressId);

        assertTrue(success);

        List<Address> addresses = userRegistry.getAddresses(userId);
        assertEquals(0, addresses.size());
    }

    @Test
    public void testDeleteNonExistentAddress() throws SQLException {
        boolean success = userRegistry.deleteAddress(999999L);

        assertFalse(success);
    }

    @Test
    public void testDeleteAddresses() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        List<Address> addresses = new ArrayList<>();
        
        Address address1 = new Address();
        address1.setAddressType(AddressType.HOME);
        address1.setStreet1("123 Main St");
        address1.setCity("Springfield");
        addresses.add(address1);

        Address address2 = new Address();
        address2.setAddressType(AddressType.WORK);
        address2.setStreet1("456 Office Blvd");
        address2.setCity("Chicago");
        addresses.add(address2);

        Address address3 = new Address();
        address3.setAddressType(AddressType.BILLING);
        address3.setStreet1("789 Billing Ave");
        address3.setCity("New York");
        addresses.add(address3);

        userRegistry.addAddresses(userId, addresses);

        boolean success = userRegistry.deleteAddresses(userId);

        assertTrue(success);

        List<Address> remainingAddresses = userRegistry.getAddresses(userId);
        assertEquals(0, remainingAddresses.size());
    }

    @Test
    public void testDeleteAddressesForNonExistentUser() throws SQLException {
        boolean success = userRegistry.deleteAddresses(999999L);

        assertFalse(success);
    }

    @Test
    public void testAddressWithAllFields() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Address address = new Address();
        address.setAddressType(AddressType.BILLING);
        address.setStreet1("123 Main St");
        address.setStreet2("Apt 4B");
        address.setCity("Springfield");
        address.setState("IL");
        address.setPostalCode("62701");
        address.setCountry("USA");
        address.setEmail("billing@example.com");
        address.setPhone("555-1234");
        address.setMobile("555-5678");
        address.setFax("555-9999");
        address.setFullname("John Doe");

        Long addressId = userRegistry.addAddress(userId, address);

        List<Address> addresses = userRegistry.getAddresses(userId);
        assertEquals(1, addresses.size());

        Address retrieved = addresses.get(0);
        assertEquals(AddressType.BILLING, retrieved.getAddressType());
        assertEquals("123 Main St", retrieved.getStreet1());
        assertEquals("Apt 4B", retrieved.getStreet2());
        assertEquals("Springfield", retrieved.getCity());
        assertEquals("IL", retrieved.getState());
        assertEquals("62701", retrieved.getPostalCode());
        assertEquals("USA", retrieved.getCountry());
        assertEquals("billing@example.com", retrieved.getEmail());
        assertEquals("555-1234", retrieved.getPhone());
        assertEquals("555-5678", retrieved.getMobile());
        assertEquals("555-9999", retrieved.getFax());
        assertEquals("John Doe", retrieved.getFullname());
    }

    @Test
    public void testAddressWithPartialFields() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Address address = new Address();
        address.setAddressType(AddressType.PRIMARY);
        address.setEmail("primary@example.com");
        address.setMobile("555-1111");

        Long addressId = userRegistry.addAddress(userId, address);

        List<Address> addresses = userRegistry.getAddresses(userId);
        assertEquals(1, addresses.size());

        Address retrieved = addresses.get(0);
        assertEquals(AddressType.PRIMARY, retrieved.getAddressType());
        assertNull(retrieved.getStreet1());
        assertNull(retrieved.getCity());
        assertEquals("primary@example.com", retrieved.getEmail());
        assertEquals("555-1111", retrieved.getMobile());
    }

    @Test
    public void testAddressWithEmptyStrings() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Address address = new Address();
        address.setAddressType(AddressType.SECONDARY);
        address.setStreet1("");
        address.setCity("");
        address.setEmail("");

        Long addressId = userRegistry.addAddress(userId, address);

        List<Address> addresses = userRegistry.getAddresses(userId);
        assertEquals(1, addresses.size());

        Address retrieved = addresses.get(0);
        assertEquals("", retrieved.getStreet1());
        assertEquals("", retrieved.getCity());
        assertEquals("", retrieved.getEmail());
    }

    @Test
    public void testAddressesPersistWhenUserDeactivated() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Address address = new Address();
        address.setAddressType(AddressType.HOME);
        address.setStreet1("123 Main St");
        address.setCity("Springfield");

        userRegistry.addAddress(userId, address);

        List<Address> addressesBefore = userRegistry.getAddresses(userId);
        assertEquals(1, addressesBefore.size());

        boolean userDeactivated = userRegistry.deactivate(userId);
        assertTrue(userDeactivated);

        List<Address> addressesAfter = userRegistry.getAddresses(userId);
        assertEquals(1, addressesAfter.size());
        assertEquals("123 Main St", addressesAfter.get(0).getStreet1());
    }

    @Test
    public void testAddressWithFullname() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Address address = new Address();
        address.setAddressType(AddressType.SHIPPING);
        address.setStreet1("789 Shipping Lane");
        address.setCity("Seattle");
        address.setState("WA");
        address.setPostalCode("98101");
        address.setFullname("Alice Smith");

        Long addressId = userRegistry.addAddress(userId, address);

        List<Address> addresses = userRegistry.getAddresses(userId);
        assertEquals(1, addresses.size());

        Address retrieved = addresses.get(0);
        assertEquals("Alice Smith", retrieved.getFullname());
        assertEquals("789 Shipping Lane", retrieved.getStreet1());
    }

    @Test
    public void testUpdateAddressWithFullname() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Address address = new Address();
        address.setAddressType(AddressType.WORK);
        address.setStreet1("123 Office St");
        address.setFullname("John Doe");

        Long addressId = userRegistry.addAddress(userId, address);

        address.setId(addressId);
        address.setFullname("Jane Doe");
        address.setStreet1("456 New Office St");

        boolean success = userRegistry.updateAddress(address);

        assertTrue(success);

        List<Address> addresses = userRegistry.getAddresses(userId);
        assertEquals(1, addresses.size());
        assertEquals("Jane Doe", addresses.get(0).getFullname());
        assertEquals("456 New Office St", addresses.get(0).getStreet1());
    }

    @Test
    public void testAddressWithNullFullname() throws SQLException {
        User user = new User();
        user.setDisplayName("Test User");
        user.setActive(true);
        Long userId = userRegistry.create(user);

        Address address = new Address();
        address.setAddressType(AddressType.OTHER);
        address.setEmail("other@example.com");

        Long addressId = userRegistry.addAddress(userId, address);

        List<Address> addresses = userRegistry.getAddresses(userId);
        assertEquals(1, addresses.size());
        assertNull(addresses.get(0).getFullname());
    }
}
