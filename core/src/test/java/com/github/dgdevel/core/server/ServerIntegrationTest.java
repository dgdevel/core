package com.github.dgdevel.core.server;

import com.github.dgdevel.core.server.Server;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ServerIntegrationTest {

    @Test
    public void testMainWithNoArgs() throws Exception {
        Thread thread = new Thread(() -> {
            try {
                Server.main(new String[]{});
            } catch (Exception e) {
                fail(e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.start();
        Thread.sleep(100);
    }

    @Test
    public void testMainWithPortArg() throws Exception {
        Thread thread = new Thread(() -> {
            try {
                Server.main(new String[]{"9090"});
            } catch (Exception e) {
                fail(e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.start();
        Thread.sleep(100);
    }

    @Test
    public void testMainWithPortAndDbArg() throws Exception {
        Thread thread = new Thread(() -> {
            try {
                Server.main(new String[]{"9091", "jdbc:h2:mem:testdb"});
            } catch (Exception e) {
                fail(e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.start();
        Thread.sleep(100);
    }
}
