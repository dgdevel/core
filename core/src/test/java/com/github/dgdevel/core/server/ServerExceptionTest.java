package com.github.dgdevel.core.server;

import com.github.dgdevel.core.server.Server;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ServerExceptionTest {

    @Test
    public void testServerConstructor() {
        Server server = new Server(9999, "jdbc:h2:mem:test");
        assertNotNull(server);
    }

    @Test
    public void testServerShutdownWithoutStart() {
        Server server = new Server(9999, "jdbc:h2:mem:test");
        assertDoesNotThrow(server::shutdown);
    }
}
