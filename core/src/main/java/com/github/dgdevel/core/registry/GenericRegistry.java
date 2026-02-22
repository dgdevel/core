package com.github.dgdevel.core.registry;

import com.github.dgdevel.core.model.Function;
import com.github.dgdevel.core.model.Menu;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenericRegistry {
  private final Connection connection;

  public GenericRegistry(Connection connection) {
    this.connection = connection;
  }

  public Long createFunction(Function function) throws SQLException {
    String sql = "INSERT INTO functions (name, url) VALUES (?, ?)";
    try (PreparedStatement stmt =
             connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
      stmt.setString(1, function.getName());
      if (function.getUrl() != null) {
        stmt.setString(2, function.getUrl());
      } else {
        stmt.setNull(2, java.sql.Types.VARCHAR);
      }
      int affectedRows = stmt.executeUpdate();
      if (affectedRows == 0) {
        throw new SQLException("Creating function failed, no rows affected.");
      }
      try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
        if (generatedKeys.next()) {
          return generatedKeys.getLong(1);
        } else {
          throw new SQLException("Creating function failed, no ID obtained.");
        }
      }
    }
  }

  public boolean updateFunction(Function function) throws SQLException {
    String sql = "UPDATE functions SET name = ?, url = ? WHERE id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, function.getName());
      if (function.getUrl() != null) {
        stmt.setString(2, function.getUrl());
      } else {
        stmt.setNull(2, java.sql.Types.VARCHAR);
      }
      stmt.setLong(3, function.getId());
      int affectedRows = stmt.executeUpdate();
      return affectedRows > 0;
    }
  }

  public Long createMenu(Menu menu) throws SQLException {
    String sql = "INSERT INTO menu (function_id, parent_id) VALUES (?, ?)";
    try (PreparedStatement stmt =
             connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
      if (menu.getFunctionId() != null) {
        stmt.setLong(1, menu.getFunctionId());
      } else {
        stmt.setNull(1, java.sql.Types.BIGINT);
      }
      if (menu.getParentId() != null) {
        stmt.setLong(2, menu.getParentId());
      } else {
        stmt.setNull(2, java.sql.Types.BIGINT);
      }
      int affectedRows = stmt.executeUpdate();
      if (affectedRows == 0) {
        throw new SQLException("Creating menu failed, no rows affected.");
      }
      try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
        if (generatedKeys.next()) {
          return generatedKeys.getLong(1);
        } else {
          throw new SQLException("Creating menu failed, no ID obtained.");
        }
      }
    }
  }

  public boolean updateMenu(Menu menu) throws SQLException {
    String sql = "UPDATE menu SET function_id = ?, parent_id = ? WHERE id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      if (menu.getFunctionId() != null) {
        stmt.setLong(1, menu.getFunctionId());
      } else {
        stmt.setNull(1, java.sql.Types.BIGINT);
      }
      if (menu.getParentId() != null) {
        stmt.setLong(2, menu.getParentId());
      } else {
        stmt.setNull(2, java.sql.Types.BIGINT);
      }
      stmt.setLong(3, menu.getId());
      int affectedRows = stmt.executeUpdate();
      return affectedRows > 0;
    }
  }

  public Function findFunctionByName(String name) throws SQLException {
    String sql = "SELECT id, name, url FROM functions WHERE name = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, name);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          Function function = new Function();
          function.setId(rs.getLong("id"));
          function.setName(rs.getString("name"));
          function.setUrl(rs.getString("url"));
          return function;
        }
      }
    }
    return null;
  }

  public List<Function> getAllFunctions() throws SQLException {
    String sql = "SELECT id, name, url FROM functions ORDER BY id";
    List<Function> functions = new ArrayList<>();
    try (PreparedStatement stmt = connection.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        Function function = new Function();
        function.setId(rs.getLong("id"));
        function.setName(rs.getString("name"));
        function.setUrl(rs.getString("url"));
        functions.add(function);
      }
    }
    return functions;
  }

  public List<Menu> getMenuTree() throws SQLException {
    String sql =
        "SELECT m.id, m.function_id, m.parent_id, f.id as func_id, f.name as func_name, f.url as func_url "
            + "FROM menu m LEFT JOIN functions f ON m.function_id = f.id ORDER BY m.id";

    Map<Long, Menu> menuMap = new HashMap<>();
    List<Menu> rootMenus = new ArrayList<>();

    try (PreparedStatement stmt = connection.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        Menu menu = new Menu();
        menu.setId(rs.getLong("id"));
        menu.setFunctionId((Long) rs.getObject("function_id"));
        menu.setParentId((Long) rs.getObject("parent_id"));

        Long funcId = (Long) rs.getObject("func_id");
        if (funcId != null) {
          Function function = new Function();
          function.setId(funcId);
          function.setName(rs.getString("func_name"));
          function.setUrl(rs.getString("func_url"));
          menu.setFunction(function);
        }

        menuMap.put(menu.getId(), menu);
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
