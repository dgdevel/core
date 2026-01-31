package com.github.dgdevel.core.registry;

import com.github.dgdevel.core.common.PaginatedList;
import com.github.dgdevel.core.common.Paginator;
import com.github.dgdevel.core.model.Address;
import com.github.dgdevel.core.model.AddressType;
import com.github.dgdevel.core.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserRegistry {
  private final Connection connection;

  public UserRegistry(Connection connection) {
    this.connection = connection;
  }

    public Long create(User user) throws SQLException {
        String sql = "INSERT INTO users (display_name, active) VALUES (?, ?)";
        try (PreparedStatement stmt =
            connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
      stmt.setString(1, user.getDisplayName());
      stmt.setBoolean(2, user.isActive());
      int affectedRows = stmt.executeUpdate();
      if (affectedRows == 0) {
        throw new SQLException("Creating user failed, no rows affected.");
      }
      try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
        if (generatedKeys.next()) {
          return generatedKeys.getLong(1);
        } else {
          throw new SQLException("Creating user failed, no ID obtained.");
        }
      }
    }
  }

  public boolean update(User user) throws SQLException {
    String sql = "UPDATE users SET display_name = ?, active = ? WHERE id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, user.getDisplayName());
      stmt.setBoolean(2, user.isActive());
      stmt.setLong(3, user.getId());
      int affectedRows = stmt.executeUpdate();
      return affectedRows > 0;
    }
  }

  public boolean activate(Long id) throws SQLException {
    String sql = "UPDATE users SET active = TRUE WHERE id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, id);
      int affectedRows = stmt.executeUpdate();
      return affectedRows > 0;
    }
  }

  public boolean deactivate(Long id) throws SQLException {
    String sql = "UPDATE users SET active = FALSE WHERE id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, id);
      int affectedRows = stmt.executeUpdate();
      return affectedRows > 0;
    }
  }

  public boolean setAttribute(Long userId, String name, String value) throws SQLException {
    String sql =
        "MERGE INTO user_attributes (user_id, name, attr_value, updated_at) "
            + "KEY (user_id, name) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, userId);
      stmt.setString(2, name);
      stmt.setString(3, value);
      int affectedRows = stmt.executeUpdate();
      return affectedRows > 0;
    } catch (SQLException e) {
      if (e.getMessage() != null && e.getMessage().contains("Referential integrity")) {
        return false;
      }
      throw e;
    }
  }

  public String getAttribute(Long userId, String name) throws SQLException {
    String sql = "SELECT attr_value FROM user_attributes WHERE user_id = ? AND name = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, userId);
      stmt.setString(2, name);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getString("attr_value");
        }
        return null;
      }
    }
  }

  public User findById(Long id) throws SQLException {
    String sql = "SELECT id, display_name, active FROM users WHERE id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          User user = new User();
          user.setId(rs.getLong("id"));
          user.setDisplayName(rs.getString("display_name"));
          user.setActive(rs.getBoolean("active"));
          return user;
        }
        return null;
      }
    }
  }

  public PaginatedList<User> findBy(Paginator paginator) throws SQLException {
    String baseSql = "SELECT id, display_name, active FROM users";
    String countSql = "SELECT COUNT(*) FROM users";
    String whereClause = buildWhereClause(paginator.getFilters());
    String orderByClause = buildOrderByClause(paginator.getSortKey(), paginator.getSortDirection());
    String limitClause = buildLimitClause(paginator.getPageNumber(), paginator.getPageSize());

    String querySql = baseSql + whereClause + orderByClause + limitClause;

    int totalCount = 0;
    try (PreparedStatement stmt = connection.prepareStatement(countSql + whereClause)) {
      if (paginator.getFilters() != null) {
        int paramIndex = 1;
        for (String value : paginator.getFilters().values()) {
          stmt.setString(paramIndex++, value);
        }
      }
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          totalCount = rs.getInt(1);
        }
      }
    }

    List<User> users = new ArrayList<>();
    try (PreparedStatement stmt = connection.prepareStatement(querySql)) {
      if (paginator.getFilters() != null) {
        int paramIndex = 1;
        for (String value : paginator.getFilters().values()) {
          stmt.setString(paramIndex++, value);
        }
      }
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          User user = new User();
          user.setId(rs.getLong("id"));
          user.setDisplayName(rs.getString("display_name"));
          user.setActive(rs.getBoolean("active"));
          users.add(user);
        }
      }
    }

    PaginatedList<User> result = new PaginatedList<>();
    result.setPage(users);
    result.setTotalCount(totalCount);
    return result;
  }

  private String buildWhereClause(Map<String, String> filters) {
    if (filters == null || filters.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder(" WHERE ");
    List<String> conditions = new ArrayList<>();
    for (String key : filters.keySet()) {
      conditions.add(key + " = ?");
    }
    sb.append(String.join(" AND ", conditions));
    return sb.toString();
  }

  private String buildOrderByClause(String sortKey, String sortDirection) {
    if (sortKey == null || sortKey.isEmpty()) {
      return " ORDER BY id";
    }
    return " ORDER BY " + sortKey + " "
        + (sortDirection != null && sortDirection.equalsIgnoreCase("DESC") ? "DESC" : "ASC");
  }

  private String buildLimitClause(int pageNumber, int pageSize) {
    int offset = (pageNumber - 1) * pageSize;
    return " LIMIT " + pageSize + " OFFSET " + offset;
  }

  public Long addAddress(Long userId, Address address) throws SQLException {
    String sql =
        "INSERT INTO addresses (user_id, address_type, street1, street2, city, state, postal_code, country, email, phone, mobile, fax, fullname) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement stmt =
        connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
      stmt.setLong(1, userId);
      stmt.setString(2, address.getAddressType().name());
      stmt.setString(3, address.getStreet1());
      stmt.setString(4, address.getStreet2());
      stmt.setString(5, address.getCity());
      stmt.setString(6, address.getState());
      stmt.setString(7, address.getPostalCode());
      stmt.setString(8, address.getCountry());
      stmt.setString(9, address.getEmail());
      stmt.setString(10, address.getPhone());
      stmt.setString(11, address.getMobile());
      stmt.setString(12, address.getFax());
      stmt.setString(13, address.getFullname());
      int affectedRows = stmt.executeUpdate();
      if (affectedRows == 0) {
        throw new SQLException("Creating address failed, no rows affected.");
      }
      try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
        if (generatedKeys.next()) {
          return generatedKeys.getLong(1);
        } else {
          throw new SQLException("Creating address failed, no ID obtained.");
        }
      }
    } catch (SQLException e) {
      if (e.getMessage() != null && e.getMessage().contains("Referential integrity")) {
        throw new SQLException("User not found", e);
      }
      throw e;
    }
  }

  public List<Long> addAddresses(Long userId, List<Address> addresses) throws SQLException {
    List<Long> addressIds = new ArrayList<>();
    for (Address address : addresses) {
      Long addressId = addAddress(userId, address);
      addressIds.add(addressId);
    }
    return addressIds;
  }

  public List<Address> getAddresses(Long userId) throws SQLException {
    String sql =
        "SELECT id, user_id, address_type, street1, street2, city, state, postal_code, country, email, phone, mobile, fax, fullname "
            + "FROM addresses WHERE user_id = ?";
    List<Address> addresses = new ArrayList<>();
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, userId);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Address address = new Address();
          address.setId(rs.getLong("id"));
          address.setUserId(rs.getLong("user_id"));
          address.setAddressType(AddressType.valueOf(rs.getString("address_type")));
          address.setStreet1(rs.getString("street1"));
          address.setStreet2(rs.getString("street2"));
          address.setCity(rs.getString("city"));
          address.setState(rs.getString("state"));
          address.setPostalCode(rs.getString("postal_code"));
          address.setCountry(rs.getString("country"));
          address.setEmail(rs.getString("email"));
          address.setPhone(rs.getString("phone"));
          address.setMobile(rs.getString("mobile"));
          address.setFax(rs.getString("fax"));
          address.setFullname(rs.getString("fullname"));
          addresses.add(address);
        }
      }
    }
    return addresses;
  }

  public List<Address> getAddressesByType(Long userId, AddressType type) throws SQLException {
    String sql =
        "SELECT id, user_id, address_type, street1, street2, city, state, postal_code, country, email, phone, mobile, fax, fullname "
            + "FROM addresses WHERE user_id = ? AND address_type = ?";
    List<Address> addresses = new ArrayList<>();
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, userId);
      stmt.setString(2, type.name());
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Address address = new Address();
          address.setId(rs.getLong("id"));
          address.setUserId(rs.getLong("user_id"));
          address.setAddressType(AddressType.valueOf(rs.getString("address_type")));
          address.setStreet1(rs.getString("street1"));
          address.setStreet2(rs.getString("street2"));
          address.setCity(rs.getString("city"));
          address.setState(rs.getString("state"));
          address.setPostalCode(rs.getString("postal_code"));
          address.setCountry(rs.getString("country"));
          address.setEmail(rs.getString("email"));
          address.setPhone(rs.getString("phone"));
          address.setMobile(rs.getString("mobile"));
          address.setFax(rs.getString("fax"));
          address.setFullname(rs.getString("fullname"));
          addresses.add(address);
        }
      }
    }
    return addresses;
  }

  public boolean updateAddress(Address address) throws SQLException {
    String sql =
        "UPDATE addresses SET address_type = ?, street1 = ?, street2 = ?, city = ?, state = ?, postal_code = ?, country = ?, email = ?, phone = ?, mobile = ?, fax = ?, fullname = ? "
            + "WHERE id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, address.getAddressType().name());
      stmt.setString(2, address.getStreet1());
      stmt.setString(3, address.getStreet2());
      stmt.setString(4, address.getCity());
      stmt.setString(5, address.getState());
      stmt.setString(6, address.getPostalCode());
      stmt.setString(7, address.getCountry());
      stmt.setString(8, address.getEmail());
      stmt.setString(9, address.getPhone());
      stmt.setString(10, address.getMobile());
      stmt.setString(11, address.getFax());
      stmt.setString(12, address.getFullname());
      stmt.setLong(13, address.getId());
      int affectedRows = stmt.executeUpdate();
      return affectedRows > 0;
    }
  }

  public boolean deleteAddress(Long addressId) throws SQLException {
    String sql = "DELETE FROM addresses WHERE id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, addressId);
      int affectedRows = stmt.executeUpdate();
      return affectedRows > 0;
    }
  }

  public boolean deleteAddresses(Long userId) throws SQLException {
    String sql = "DELETE FROM addresses WHERE user_id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, userId);
      int affectedRows = stmt.executeUpdate();
      return affectedRows > 0;
    }
  }
}
