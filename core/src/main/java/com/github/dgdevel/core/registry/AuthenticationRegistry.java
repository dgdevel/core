package com.github.dgdevel.core.registry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class AuthenticationRegistry {
  private final Connection connection;

  public AuthenticationRegistry(Connection connection) {
    this.connection = connection;
  }

  public void registerType(String code, boolean onlyOnePerUserId, boolean onlyOneSecurityPrincipal)
      throws SQLException {
    String sql =
        "MERGE INTO credential_type (code, only_one_per_user_id, only_one_security_principal) "
            + "KEY (code) VALUES (?, ?, ?)";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, code);
      stmt.setBoolean(2, onlyOnePerUserId);
      stmt.setBoolean(3, onlyOneSecurityPrincipal);
      stmt.executeUpdate();
    }
  }

  public Long create(
      Long userId,
      String credentialTypeCode,
      Timestamp validFrom,
      Timestamp validUntil,
      String securityPrincipal,
      String securityCredentials)
      throws SQLException {
    Long credentialTypeId = getCredentialTypeId(credentialTypeCode);
    if (credentialTypeId == null) {
      throw new SQLException("Credential type not found: " + credentialTypeCode);
    }

    Boolean onlyOnePerUserId = getOnlyOnePerUserId(credentialTypeId);
    Boolean onlyOneSecurityPrincipal = getOnlyOneSecurityPrincipal(credentialTypeId);

    if (onlyOnePerUserId != null && onlyOnePerUserId) {
      expireAll(userId, credentialTypeCode);
    }

    if (onlyOneSecurityPrincipal != null && onlyOneSecurityPrincipal && securityPrincipal != null) {
      expireBySecurityPrincipal(credentialTypeCode, securityPrincipal);
    }

    String sql =
        "INSERT INTO credentials (user_id, credential_type_id, valid_from, valid_until, "
            + "security_principal, security_credentials) "
            + "VALUES (?, ?, ?, ?, ?, ?)";
    try (PreparedStatement stmt =
        connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
      stmt.setLong(1, userId);
      stmt.setLong(2, credentialTypeId);
      stmt.setTimestamp(3, validFrom);
      stmt.setTimestamp(4, validUntil);
      stmt.setString(5, securityPrincipal);
      stmt.setString(6, securityCredentials);
      int affectedRows = stmt.executeUpdate();
      System.out.println("[AuthRegistry.create] affectedRows=" + affectedRows + ", userId=" + userId + ", principal=" + securityPrincipal);
      if (affectedRows == 0) {
        throw new SQLException("Creating credential failed, no rows affected.");
      }
      try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
        if (generatedKeys.next()) {
          return generatedKeys.getLong(1);
        } else {
          throw new SQLException("Creating credential failed, no ID obtained.");
        }
      }
    }
  }

  public boolean expireOne(Long userId, String credentialTypeCode, String securityPrincipal)
      throws SQLException {
    Long credentialTypeId = getCredentialTypeId(credentialTypeCode);
    if (credentialTypeId == null) {
      throw new SQLException("Credential type not found: " + credentialTypeCode);
    }

    String sql =
        "UPDATE credentials SET valid_until = CURRENT_TIMESTAMP "
            + "WHERE user_id = ? AND credential_type_id = ? AND security_principal = ? "
            + "AND valid_until > CURRENT_TIMESTAMP";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, userId);
      stmt.setLong(2, credentialTypeId);
      stmt.setString(3, securityPrincipal);
      int affectedRows = stmt.executeUpdate();
      System.out.println("[AuthRegistry.expireOne] affectedRows=" + affectedRows + ", userId=" + userId + ", principal=" + securityPrincipal);
      return affectedRows > 0;
    }
  }

  public boolean expireAll(Long userId, String credentialTypeCode) throws SQLException {
    Long credentialTypeId = getCredentialTypeId(credentialTypeCode);
    if (credentialTypeId == null) {
      throw new SQLException("Credential type not found: " + credentialTypeCode);
    }

    String sql =
        "UPDATE credentials SET valid_until = CURRENT_TIMESTAMP "
            + "WHERE user_id = ? AND credential_type_id = ? AND valid_until > CURRENT_TIMESTAMP";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, userId);
      stmt.setLong(2, credentialTypeId);
      int affectedRows = stmt.executeUpdate();
      return affectedRows > 0;
    }
  }

  public Long verify(String credentialTypeCode, String securityPrincipal, String securityCredentials)
      throws SQLException {
    Long credentialTypeId = getCredentialTypeId(credentialTypeCode);
    if (credentialTypeId == null) {
      return null;
    }

    String sql =
        "SELECT user_id FROM credentials "
            + "WHERE credential_type_id = ? AND security_principal = ? "
            + "AND (security_credentials = ? OR (security_credentials IS NULL AND ? IS NULL)) "
            + "AND valid_from <= CURRENT_TIMESTAMP AND valid_until > CURRENT_TIMESTAMP";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, credentialTypeId);
      stmt.setString(2, securityPrincipal);
      stmt.setString(3, securityCredentials);
      stmt.setString(4, securityCredentials);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getLong("user_id");
        }
        return null;
      }
    }
  }

  private Long getCredentialTypeId(String code) throws SQLException {
    String sql = "SELECT id FROM credential_type WHERE code = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, code);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getLong("id");
        }
        return null;
      }
    }
  }

  private Boolean getOnlyOnePerUserId(Long credentialTypeId) throws SQLException {
    String sql = "SELECT only_one_per_user_id FROM credential_type WHERE id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, credentialTypeId);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getBoolean("only_one_per_user_id");
        }
        return null;
      }
    }
  }

  private Boolean getOnlyOneSecurityPrincipal(Long credentialTypeId) throws SQLException {
    String sql = "SELECT only_one_security_principal FROM credential_type WHERE id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, credentialTypeId);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getBoolean("only_one_security_principal");
        }
        return null;
      }
    }
  }

  private boolean expireBySecurityPrincipal(String credentialTypeCode, String securityPrincipal)
      throws SQLException {
    Long credentialTypeId = getCredentialTypeId(credentialTypeCode);
    if (credentialTypeId == null) {
      throw new SQLException("Credential type not found: " + credentialTypeCode);
    }

    String sql =
        "UPDATE credentials SET valid_until = CURRENT_TIMESTAMP "
            + "WHERE credential_type_id = ? AND security_principal = ? "
            + "AND valid_until > CURRENT_TIMESTAMP";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, credentialTypeId);
      stmt.setString(2, securityPrincipal);
      int affectedRows = stmt.executeUpdate();
      return affectedRows > 0;
    }
  }
}
