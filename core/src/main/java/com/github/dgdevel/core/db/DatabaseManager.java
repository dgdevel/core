package com.github.dgdevel.core.db;

import com.github.dgdevel.core.common.PaginatedList;
import com.github.dgdevel.core.common.Paginator;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class DatabaseManager {
    private Connection connection;
    private final String dbUrl;
    private final String dbUsername;
    private final String dbPassword;

    public DatabaseManager(String dbUrl, String dbUsername, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }

    public void connect() throws SQLException {
        if (dbUsername != null && !dbUsername.isEmpty()) {
            connection = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
        } else {
            connection = DriverManager.getConnection(dbUrl);
        }
        initializeSchema();
    }

    public void disconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    public void cleanup() throws SQLException {
        if (connection == null || connection.isClosed()) {
            return;
        }
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP ALL OBJECTS");
        }
    }

    public Connection getConnection() {
        return connection;
    }

    private void initializeSchema() throws SQLException {
        try {
            var schemaFiles = getClass().getClassLoader().getResources("schema");
            java.util.ArrayList<String> schemaPaths = new java.util.ArrayList<>();
            while (schemaFiles.hasMoreElements()) {
                java.net.URL url = schemaFiles.nextElement();
                if ("file".equals(url.getProtocol())) {
                    java.io.File dir = new java.io.File(url.toURI());
                    java.io.File[] files = dir.listFiles((d, name) -> name.endsWith(".sql"));
                    if (files != null) {
                        for (java.io.File file : files) {
                            schemaPaths.add(file.getAbsolutePath());
                        }
                    }
                } else if ("jar".equals(url.getProtocol())) {
                    try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(url.getPath().split("!")[0].substring("file:".length()))) {
                        java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();
                        while (entries.hasMoreElements()) {
                            java.util.jar.JarEntry entry = entries.nextElement();
                            if (entry.getName().startsWith("schema/") && entry.getName().endsWith(".sql")) {
                                schemaPaths.add(entry.getName());
                            }
                        }
                    }
                }
            }
            java.util.Collections.sort(schemaPaths);
            for (String schemaPath : schemaPaths) {
                String resourceName = schemaPath.contains("/") ? schemaPath.substring(schemaPath.indexOf("schema/")) : "schema/" + new java.io.File(schemaPath).getName();
                InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName);
                if (inputStream != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder sql = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sql.append(line).append("\n");
                    }
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute(sql.toString());
                    }
                    reader.close();
                    inputStream.close();
                }
            }
        } catch (Exception e) {
            throw new SQLException("Failed to initialize database schema: " + e.getMessage(), e);
        }
    }

    public Timestamp getCurrentTimestamp() throws SQLException {
        String sql = "SELECT CURRENT_TIMESTAMP";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getTimestamp(1);
            }
        }
        throw new SQLException("Failed to get current timestamp");
    }

    public boolean setConfigValue(String namespace, String key, String value) throws SQLException {
        String sql = "MERGE INTO config (namespace, config_key, config_value, updated_at) KEY (namespace, config_key) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, namespace);
            stmt.setString(2, key);
            stmt.setString(3, value);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    public String getConfigValue(String namespace, String key) throws SQLException {
        String sql = "SELECT config_value FROM config WHERE namespace = ? AND config_key = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, namespace);
            stmt.setString(2, key);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
                return null;
            }
        }
    }

    public Map<String, String> getAllConfigValues() throws SQLException {
        String sql = "SELECT namespace, config_key, config_value FROM config ORDER BY namespace, config_key";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            Map<String, String> configValues = new HashMap<>();
            while (rs.next()) {
                String namespace = rs.getString(1);
                String key = rs.getString(2);
                String value = rs.getString(3);
                String fullKey = namespace + "." + key;
                configValues.put(fullKey, value);
            }
            return configValues;
        }
    }

    private Long getOrCreateLogTypeId(String typeCode) throws SQLException {
        String selectSql = "SELECT id FROM audit_log_type WHERE code = ?";
        try (PreparedStatement stmt = connection.prepareStatement(selectSql)) {
            stmt.setString(1, typeCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        String insertSql = "INSERT INTO audit_log_type (code) VALUES (?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, typeCode);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }
        }
        throw new SQLException("Failed to create log type: " + typeCode);
    }

    public Long auditLog(Long userId, String typeCode, String payload) throws SQLException {
        Long logTypeId = getOrCreateLogTypeId(typeCode);
        String sql = "INSERT INTO audit_log (instant_at, user_id, log_type_id, payload) VALUES (CURRENT_TIMESTAMP, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (userId != null) {
                stmt.setLong(1, userId);
            } else {
                stmt.setNull(1, java.sql.Types.BIGINT);
            }
            stmt.setLong(2, logTypeId);
            stmt.setString(3, payload);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }
        }
        throw new SQLException("Failed to create audit log entry");
    }

    public PaginatedList auditLogList(Paginator paginator) throws SQLException {
        StringBuilder baseSql = new StringBuilder("SELECT COUNT(*) FROM audit_log al JOIN audit_log_type alt ON al.log_type_id = alt.id");
        StringBuilder selectSql = new StringBuilder("SELECT al.id, al.instant_at, al.user_id, alt.code as type_code, al.payload FROM audit_log al JOIN audit_log_type alt ON al.log_type_id = alt.id");

        if (paginator.getFilters() != null && !paginator.getFilters().isEmpty()) {
            String whereClause = buildWhereClause(paginator.getFilters());
            baseSql.append(whereClause);
            selectSql.append(whereClause);
        }

        String countSql = baseSql.toString();
        try (PreparedStatement countStmt = connection.prepareStatement(countSql)) {
            setFilterParams(countStmt, paginator.getFilters());
            try (ResultSet countRs = countStmt.executeQuery()) {
                if (countRs.next()) {
                    int totalCount = countRs.getInt(1);

                    String orderBy = buildOrderBy(paginator.getSortKey(), paginator.getSortDirection());
                    String pagination = buildPagination(paginator.getPageNumber(), paginator.getPageSize());

                    selectSql.append(orderBy).append(pagination);

                    try (PreparedStatement selectStmt = connection.prepareStatement(selectSql.toString())) {
                        setFilterParams(selectStmt, paginator.getFilters());
                        try (ResultSet rs = selectStmt.executeQuery()) {
                            java.util.List<java.util.Map<String, Object>> page = new java.util.ArrayList<>();
                            while (rs.next()) {
                                java.util.Map<String, Object> entry = new java.util.HashMap<>();
                                entry.put("id", rs.getLong("id"));
                                entry.put("instant_at", rs.getTimestamp("instant_at"));
                                entry.put("user_id", rs.getObject("user_id"));
                                entry.put("type_code", rs.getString("type_code"));
                                entry.put("payload", rs.getString("payload"));
                                page.add(entry);
                            }
                            PaginatedList<java.util.Map<String, Object>> result = new PaginatedList<>();
                            result.setPage(page);
                            result.setTotalCount(totalCount);
                            return result;
                        }
                    }
                }
            }
        }
        throw new SQLException("Failed to retrieve audit log list");
    }

    private String buildWhereClause(java.util.Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) {
            return "";
        }
        StringBuilder where = new StringBuilder(" WHERE ");
        java.util.List<String> conditions = new java.util.ArrayList<>();
        for (String key : filters.keySet()) {
            switch (key) {
                case "type_code":
                    conditions.add("alt.code = ?");
                    break;
                case "user_id":
                    conditions.add("al.user_id = ?");
                    break;
                default:
                    break;
            }
        }
        return where.append(String.join(" AND ", conditions)).toString();
    }

    private String buildOrderBy(String sortKey, String sortDirection) {
        if (sortKey == null || sortKey.isEmpty()) {
            return " ORDER BY al.instant_at DESC";
        }
        String column = switch (sortKey) {
            case "instant_at" -> "al.instant_at";
            case "type_code" -> "alt.code";
            case "user_id" -> "al.user_id";
            default -> "al.instant_at";
        };
        String direction = sortDirection != null && sortDirection.equalsIgnoreCase("ASC") ? "ASC" : "DESC";
        return " ORDER BY " + column + " " + direction;
    }

    private String buildPagination(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return " LIMIT " + pageSize + " OFFSET " + offset;
    }

    private void setFilterParams(PreparedStatement stmt, java.util.Map<String, String> filters) throws SQLException {
        if (filters == null || filters.isEmpty()) {
            return;
        }
        int paramIndex = 1;
        for (String key : filters.keySet()) {
            if (key.equals("type_code")) {
                stmt.setString(paramIndex++, filters.get(key));
            } else if (key.equals("user_id")) {
                stmt.setLong(paramIndex++, Long.parseLong(filters.get(key)));
            }
        }
    }

    public boolean setLocalization(String key, String languageCode, String translation) throws SQLException {
        String sql = "MERGE INTO translations (translation_key, language_code, translation, updated_at) KEY (translation_key, language_code) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, key);
            stmt.setString(2, languageCode);
            stmt.setString(3, translation);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    public String getTranslation(String key, String languageCode) throws SQLException {
        String sql = "SELECT translation FROM translations WHERE translation_key = ? AND language_code = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, key);
            stmt.setString(2, languageCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
                return null;
            }
        }
    }
}
