package com.github.dgdevel.core.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {
    private final String bindAddress;
    private final int jsonRpcPort;
    private final int msgPackPort;
    private final String dbUrl;
    private final String dbUsername;
    private final String dbPassword;
    private static final String DEFAULT_CONFIG_FILE = "config.ini";

    public Config(String bindAddress, int jsonRpcPort, int msgPackPort, String dbUrl, String dbUsername, String dbPassword) {
        this.bindAddress = bindAddress;
        this.jsonRpcPort = jsonRpcPort;
        this.msgPackPort = msgPackPort;
        this.dbUrl = dbUrl;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public int getJsonRpcPort() {
        return jsonRpcPort;
    }

    public int getMsgPackPort() {
        return msgPackPort;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public static Config load(String[] args) throws IOException {
        if (args.length > 0) {
            return fromArgs(args);
        }
        
        File configFile = new File(DEFAULT_CONFIG_FILE);
        if (configFile.exists()) {
            return fromConfigFile(configFile);
        }
        
        return defaults();
    }

    private static Config fromArgs(String[] args) {
        String bindAddress = "0.0.0.0";
        int jsonRpcPort = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        int msgPackPort = args.length > 1 ? Integer.parseInt(args[1]) : jsonRpcPort + 1;
        String dbUrl = args.length > 2 ? args[2] : "jdbc:h2:mem:test";
        String dbUsername = args.length > 3 ? args[3] : null;
        String dbPassword = args.length > 4 ? args[4] : null;
        return new Config(bindAddress, jsonRpcPort, msgPackPort, dbUrl, dbUsername, dbPassword);
    }

    private static Config fromConfigFile(File configFile) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            props.load(fis);
        }
        
        String bindAddress = props.getProperty("bindAddress", "0.0.0.0");
        int jsonRpcPort = Integer.parseInt(props.getProperty("jsonRpcPort", "8080"));
        int msgPackPort = Integer.parseInt(props.getProperty("msgPackPort", String.valueOf(jsonRpcPort + 1)));
        String dbUrl = props.getProperty("dbUrl", "jdbc:h2:mem:test");
        String dbUsername = props.getProperty("dbUsername", null);
        String dbPassword = props.getProperty("dbPassword", null);
        
        return new Config(bindAddress, jsonRpcPort, msgPackPort, dbUrl, dbUsername, dbPassword);
    }

    private static Config defaults() {
        return new Config("0.0.0.0", 8080, 8081, "jdbc:h2:mem:test", null, null);
    }
}
