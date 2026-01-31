package com.github.dgdevel.core.registry;

import com.github.dgdevel.core.model.Function;
import com.github.dgdevel.core.model.Menu;
import com.github.dgdevel.core.model.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AuthorizationRegistry {
  private final Connection connection;

  public AuthorizationRegistry(Connection connection) {
    this.connection = connection;
  }

  public Long create(Role role) throws SQLException {
    Long parentId = null;
    if (role.getParentCode() != null && !role.getParentCode().isEmpty()) {
      Role parentRole = findByCode(role.getParentCode());
      if (parentRole == null) {
        throw new SQLException("Parent role with code '" + role.getParentCode() + "' not found.");
      }
      parentId = parentRole.getId();
    }

    String sql = "INSERT INTO roles (code, name, parent_id) VALUES (?, ?, ?)";
    try (PreparedStatement stmt =
        connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
      stmt.setString(1, role.getCode());
      stmt.setString(2, role.getName());
      if (parentId != null) {
        stmt.setLong(3, parentId);
      } else {
        stmt.setNull(3, java.sql.Types.BIGINT);
      }
      int affectedRows = stmt.executeUpdate();
      if (affectedRows == 0) {
        throw new SQLException("Creating role failed, no rows affected.");
      }
      try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
        if (generatedKeys.next()) {
          return generatedKeys.getLong(1);
        } else {
          throw new SQLException("Creating role failed, no ID obtained.");
        }
      }
    }
  }

  public boolean update(Role role) throws SQLException {
    Long parentId = null;
    if (role.getParentCode() != null && !role.getParentCode().isEmpty()) {
      Role parentRole = findByCode(role.getParentCode());
      if (parentRole == null) {
        throw new SQLException("Parent role with code '" + role.getParentCode() + "' not found.");
      }
      if (parentRole.getId().equals(role.getId())) {
        throw new SQLException("Role cannot be its own parent.");
      }
      parentId = parentRole.getId();
    }

    String sql = "UPDATE roles SET code = ?, name = ?, parent_id = ? WHERE id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, role.getCode());
      stmt.setString(2, role.getName());
      if (parentId != null) {
        stmt.setLong(3, parentId);
      } else {
        stmt.setNull(3, java.sql.Types.BIGINT);
      }
      stmt.setLong(4, role.getId());
      int affectedRows = stmt.executeUpdate();
      return affectedRows > 0;
    }
  }

  public boolean checkOverlap(Long userId, Long roleId, Timestamp validFrom, Timestamp validUntil) throws SQLException {
    String sql =
        "SELECT COUNT(*) FROM authorizations WHERE user_id = ? AND role_id = ? "
            + "AND ((valid_from <= ? AND valid_until >= ?) "
            + "OR (valid_from <= ? AND valid_until >= ?) "
            + "OR (valid_from >= ? AND valid_until <= ?))";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, userId);
      stmt.setLong(2, roleId);
      stmt.setTimestamp(3, validFrom);
      stmt.setTimestamp(4, validFrom);
      stmt.setTimestamp(5, validUntil);
      stmt.setTimestamp(6, validUntil);
      stmt.setTimestamp(7, validFrom);
      stmt.setTimestamp(8, validUntil);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1) > 0;
        }
      }
    }
    return false;
  }

  public Long authorize(Long userId, Long roleId, Timestamp validFrom, Timestamp validUntil) throws SQLException {
    if (checkOverlap(userId, roleId, validFrom, validUntil)) {
      throw new SQLException(
          "Authorization period overlaps with existing authorization for the same user and role.");
    }
    String sql = "INSERT INTO authorizations (user_id, role_id, valid_from, valid_until) VALUES (?, ?, ?, ?)";
    try (PreparedStatement stmt =
        connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
      stmt.setLong(1, userId);
      stmt.setLong(2, roleId);
      stmt.setTimestamp(3, validFrom);
      stmt.setTimestamp(4, validUntil);
      int affectedRows = stmt.executeUpdate();
      if (affectedRows == 0) {
        throw new SQLException("Creating authorization failed, no rows affected.");
      }
      try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
        if (generatedKeys.next()) {
          return generatedKeys.getLong(1);
        } else {
          throw new SQLException("Creating authorization failed, no ID obtained.");
        }
      }
    }
  }

  public boolean deauthorize(Long userId, Long roleId) throws SQLException {
    String sql = "DELETE FROM authorizations WHERE user_id = ? AND role_id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, userId);
      stmt.setLong(2, roleId);
      int affectedRows = stmt.executeUpdate();
      return affectedRows > 0;
    }
  }

  public boolean isUserInRole(Long userId, Long roleId) throws SQLException {
    Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
    
    List<Long> ancestorRoleIds = getAncestorRoleIds(roleId);
    List<Long> allRoleIdsToCheck = new ArrayList<>();
    allRoleIdsToCheck.add(roleId);
    allRoleIdsToCheck.addAll(ancestorRoleIds);
    
    String inClause = String.join(",", java.util.Collections.nCopies(allRoleIdsToCheck.size(), "?"));
    String sql =
        "SELECT COUNT(*) FROM authorizations WHERE user_id = ? AND role_id IN (" + inClause + ") "
            + "AND valid_from <= ? AND valid_until >= ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      int paramIndex = 1;
      stmt.setLong(paramIndex++, userId);
      for (Long id : allRoleIdsToCheck) {
        stmt.setLong(paramIndex++, id);
      }
      stmt.setTimestamp(paramIndex++, currentTimestamp);
      stmt.setTimestamp(paramIndex, currentTimestamp);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1) > 0;
        }
      }
    }
    return false;
  }

  public boolean isUserInAnyRoles(Long userId, List<Long> roleIds) throws SQLException {
    if (roleIds.isEmpty()) {
      return false;
    }
    
    List<Long> allRoleIdsToCheck = new ArrayList<>();
    for (Long roleId : roleIds) {
      allRoleIdsToCheck.add(roleId);
      allRoleIdsToCheck.addAll(getAncestorRoleIds(roleId));
    }
    
    Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
    StringBuilder sql =
        new StringBuilder(
            "SELECT COUNT(*) FROM authorizations WHERE user_id = ? AND role_id IN (");
    sql.append(String.join(",", java.util.Collections.nCopies(allRoleIdsToCheck.size(), "?")));
    sql.append(") AND valid_from <= ? AND valid_until >= ?");
    try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
      int paramIndex = 1;
      stmt.setLong(paramIndex++, userId);
      for (Long roleId : allRoleIdsToCheck) {
        stmt.setLong(paramIndex++, roleId);
      }
      stmt.setTimestamp(paramIndex++, currentTimestamp);
      stmt.setTimestamp(paramIndex, currentTimestamp);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1) > 0;
        }
      }
    }
    return false;
  }

  public boolean isUserInAllRoles(Long userId, List<Long> roleIds) throws SQLException {
    if (roleIds.isEmpty()) {
      return true;
    }

    Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
    String sql = "SELECT role_id FROM authorizations WHERE user_id = ? AND valid_from <= ? AND valid_until >= ?";
    List<Long> userRoleIds = new ArrayList<>();
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, userId);
      stmt.setTimestamp(2, currentTimestamp);
      stmt.setTimestamp(3, currentTimestamp);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          userRoleIds.add(rs.getLong("role_id"));
        }
      }
    }

    if (userRoleIds.isEmpty()) {
      return false;
    }

    Set<Long> expandedUserRoles = new java.util.HashSet<>();
    for (Long userRoleId : userRoleIds) {
      expandedUserRoles.add(userRoleId);
      expandedUserRoles.addAll(getAllDescendantRoleIds(userRoleId));
    }

    for (Long requiredRoleId : roleIds) {
      List<Long> requiredRoleAndAncestors = new ArrayList<>();
      requiredRoleAndAncestors.add(requiredRoleId);
      requiredRoleAndAncestors.addAll(getAncestorRoleIds(requiredRoleId));

      boolean covered = false;
      for (Long role : requiredRoleAndAncestors) {
        if (expandedUserRoles.contains(role)) {
          covered = true;
          break;
        }
      }

      if (!covered) {
        return false;
      }
    }

    return true;
  }

  private List<Long> getAncestorRoleIds(Long roleId) throws SQLException {
    List<Long> ancestorIds = new ArrayList<>();
    Long currentId = roleId;
    
    while (currentId != null) {
      String sql = "SELECT parent_id FROM roles WHERE id = ?";
      try (PreparedStatement stmt = connection.prepareStatement(sql)) {
        stmt.setLong(1, currentId);
        try (ResultSet rs = stmt.executeQuery()) {
          if (rs.next()) {
            Long parentId = (Long) rs.getObject("parent_id");
            if (parentId != null) {
              ancestorIds.add(parentId);
              currentId = parentId;
            } else {
              currentId = null;
            }
          } else {
            currentId = null;
          }
        }
      }
    }

    return ancestorIds;
  }

  private List<Long> getAllDescendantRoleIds(Long roleId) throws SQLException {
    List<Long> descendantIds = new ArrayList<>();
    List<Long> currentLevel = new ArrayList<>();
    currentLevel.add(roleId);

    while (!currentLevel.isEmpty()) {
      List<Long> nextLevel = new ArrayList<>();
      String inClause = String.join(",", java.util.Collections.nCopies(currentLevel.size(), "?"));
      String sql = "SELECT id FROM roles WHERE parent_id IN (" + inClause + ")";
      try (PreparedStatement stmt = connection.prepareStatement(sql)) {
        int paramIndex = 1;
        for (Long id : currentLevel) {
          stmt.setLong(paramIndex++, id);
        }
        try (ResultSet rs = stmt.executeQuery()) {
          while (rs.next()) {
            Long childId = rs.getLong("id");
            if (!descendantIds.contains(childId)) {
              descendantIds.add(childId);
              nextLevel.add(childId);
            }
          }
        }
      }
      currentLevel = nextLevel;
    }

    return descendantIds;
  }

  public Role findById(Long id) throws SQLException {
    String sql = "SELECT r.id, r.code, r.name, r.parent_id, p.code as parent_code FROM roles r LEFT JOIN roles p ON r.parent_id = p.id WHERE r.id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          Role role = new Role();
          role.setId(rs.getLong("id"));
          role.setCode(rs.getString("code"));
          role.setName(rs.getString("name"));
          Long parentId = (Long) rs.getObject("parent_id");
          if (parentId != null) {
            role.setParentId(parentId);
            role.setParentCode(rs.getString("parent_code"));
          }
          return role;
        }
        return null;
      }
    }
  }

  public Role findByCode(String code) throws SQLException {
    String sql = "SELECT r.id, r.code, r.name, r.parent_id, p.code as parent_code FROM roles r LEFT JOIN roles p ON r.parent_id = p.id WHERE r.code = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, code);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          Role role = new Role();
          role.setId(rs.getLong("id"));
          role.setCode(rs.getString("code"));
          role.setName(rs.getString("name"));
          Long parentId = (Long) rs.getObject("parent_id");
          if (parentId != null) {
            role.setParentId(parentId);
            role.setParentCode(rs.getString("parent_code"));
          }
          return role;
        }
        return null;
      }
    }
  }

  public List<Role> findAll() throws SQLException {
    String sql = "SELECT r.id, r.code, r.name, r.parent_id, p.code as parent_code FROM roles r LEFT JOIN roles p ON r.parent_id = p.id ORDER BY r.code";
    List<Role> roles = new ArrayList<>();
    try (PreparedStatement stmt = connection.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        Role role = new Role();
        role.setId(rs.getLong("id"));
        role.setCode(rs.getString("code"));
        role.setName(rs.getString("name"));
        Long parentId = (Long) rs.getObject("parent_id");
        if (parentId != null) {
          role.setParentId(parentId);
          role.setParentCode(rs.getString("parent_code"));
        }
        roles.add(role);
      }
    }
    return roles;
  }

  public Long addFunctionToRole(Long roleId, Long functionId) throws SQLException {
    String sql = "INSERT INTO role_functions (role_id, function_id) VALUES (?, ?)";
    try (PreparedStatement stmt =
             connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
      stmt.setLong(1, roleId);
      stmt.setLong(2, functionId);
      int affectedRows = stmt.executeUpdate();
      if (affectedRows == 0) {
        throw new SQLException("Adding function to role failed, no rows affected.");
      }
      try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
        if (generatedKeys.next()) {
          return generatedKeys.getLong(1);
        } else {
          throw new SQLException("Adding function to role failed, no ID obtained.");
        }
      }
    } catch (SQLException e) {
      if (e.getMessage() != null && e.getMessage().contains("Unique index or primary key")) {
        throw new SQLException("Function already assigned to role", e);
      }
      throw e;
    }
  }

  public boolean removeFunctionFromRole(Long roleId, Long functionId) throws SQLException {
    String sql = "DELETE FROM role_functions WHERE role_id = ? AND function_id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, roleId);
      stmt.setLong(2, functionId);
      int affectedRows = stmt.executeUpdate();
      return affectedRows > 0;
    }
  }

  public List<Function> getFunctionsByRole(Long roleId) throws SQLException {
    String sql =
        "SELECT f.id, f.name, f.url FROM functions f "
            + "INNER JOIN role_functions rf ON f.id = rf.function_id "
            + "WHERE rf.role_id = ?";
    List<Function> functions = new ArrayList<>();
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, roleId);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Function function = new Function();
          function.setId(rs.getLong("id"));
          function.setName(rs.getString("name"));
          function.setUrl(rs.getString("url"));
          functions.add(function);
        }
      }
    }
    return functions;
  }

  public List<Menu> getMenuTree(Long userId) throws SQLException {
    Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());

    String sql =
        "SELECT DISTINCT m.id, m.function_id, m.parent_id, f.id as func_id, f.name as func_name, f.url as func_url "
            + "FROM menu m "
            + "INNER JOIN functions f ON m.function_id = f.id "
            + "INNER JOIN role_functions rf ON f.id = rf.function_id "
            + "INNER JOIN authorizations a ON rf.role_id = a.role_id "
            + "WHERE a.user_id = ? "
            + "AND a.valid_from <= ? AND a.valid_until >= ? "
            + "ORDER BY m.id";

    Set<Long> authorizedFunctionIds = new HashSet<>();
    Map<Long, Menu> menuMap = new HashMap<>();
    List<Menu> rootMenus = new ArrayList<>();

    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, userId);
      stmt.setTimestamp(2, currentTimestamp);
      stmt.setTimestamp(3, currentTimestamp);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Menu menu = new Menu();
          menu.setId(rs.getLong("id"));
          menu.setFunctionId((Long) rs.getObject("function_id"));
          menu.setParentId((Long) rs.getObject("parent_id"));

          Long funcId = (Long) rs.getObject("func_id");
          if (funcId != null) {
            authorizedFunctionIds.add(funcId);
            Function function = new Function();
            function.setId(funcId);
            function.setName(rs.getString("func_name"));
            function.setUrl(rs.getString("func_url"));
            menu.setFunction(function);
          }

          menuMap.put(menu.getId(), menu);
        }
      }
    }

    for (Menu menu : menuMap.values()) {
      if (menu.getParentId() == null) {
        rootMenus.add(menu);
      } else {
        Menu parent = menuMap.get(menu.getParentId());
        if (parent != null) {
          parent.addChild(menu);
        }
      }
    }

    return rootMenus;
  }
}
