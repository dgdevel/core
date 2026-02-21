package com.github.dgdevel.core.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

public class ServerInvalidRequestTest {
    private Server server;
    private final int testPort = 28080;
    private final String testDbUrl = "jdbc:h2:mem:testinvalid";

    @BeforeEach
    public void setUp() throws Exception {
        server = new Server(testPort, testDbUrl, null, null);
        server.start();
    }

    @AfterEach
    public void tearDown() {
        server.shutdown();
    }

    @Test
    public void testInvalidHttpRequest() throws Exception {
        try (Socket socket = new Socket("localhost", testPort);
             OutputStream out = socket.getOutputStream()) {
            
            String badRequest = "INVALID HTTP REQUEST\r\n\r\n";
            out.write(badRequest.getBytes());
            out.flush();
            
            Thread.sleep(100);
        }
    }
}
