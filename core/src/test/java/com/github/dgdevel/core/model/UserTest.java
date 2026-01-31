package com.github.dgdevel.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    @Test
    public void testDefaultConstructor() {
        User user = new User();
        assertNull(user.getId());
        assertNull(user.getDisplayName());
        assertFalse(user.isActive());
    }

    @Test
    public void testSettersAndGetters() {
        User user = new User();
        user.setId(1L);
        user.setDisplayName("John Doe");
        user.setActive(true);

        assertEquals(1L, user.getId());
        assertEquals("John Doe", user.getDisplayName());
        assertTrue(user.isActive());
    }

    @Test
    public void testSetActiveFalse() {
        User user = new User();
        user.setActive(false);

        assertFalse(user.isActive());
    }

    @Test
    public void testSetMultipleTimes() {
        User user = new User();
        user.setId(100L);
        user.setDisplayName("User One");
        user.setActive(true);

        assertEquals(100L, user.getId());
        assertEquals("User One", user.getDisplayName());
        assertTrue(user.isActive());

        user.setId(200L);
        user.setDisplayName("User Two");
        user.setActive(false);

        assertEquals(200L, user.getId());
        assertEquals("User Two", user.getDisplayName());
        assertFalse(user.isActive());
    }

    @Test
    public void testNullDisplayName() {
        User user = new User();
        user.setDisplayName(null);

        assertNull(user.getDisplayName());
    }

    @Test
    public void testEmptyDisplayName() {
        User user = new User();
        user.setDisplayName("");

        assertEquals("", user.getDisplayName());
    }
}
