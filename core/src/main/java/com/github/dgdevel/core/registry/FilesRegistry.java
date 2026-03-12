package com.github.dgdevel.core.registry;

import com.github.dgdevel.core.common.PaginatedList;
import com.github.dgdevel.core.common.Paginator;
import com.github.dgdevel.core.model.File;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FilesRegistry {
  private final Connection connection;

  public FilesRegistry(Connection connection) {
    this.connection = connection;
  }

  public Long create(File file) throws SQLException {
    String sql = "INSERT INTO files (name, content_type, payload) VALUES (?, ?, ?)";
    try (PreparedStatement stmt =
             connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
      stmt.setString(1, file.getName());
      if (file.getContentType() != null) {
        stmt.setString(2, file.getContentType());
      } else {
        stmt.setNull(2, java.sql.Types.VARCHAR);
      }
      if (file.getPayload() != null) {
        stmt.setString(3, file.getPayload());
      } else {
        stmt.setNull(3, java.sql.Types.CLOB);
      }
      int affectedRows = stmt.executeUpdate();
      if (affectedRows == 0) {
        throw new SQLException("Creating file failed, no rows affected.");
      }
      try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
        if (generatedKeys.next()) {
          return generatedKeys.getLong(1);
        } else {
          throw new SQLException("Creating file failed, no ID obtained.");
        }
      }
    }
  }

  public boolean delete(Long id) throws SQLException {
    String sql = "DELETE FROM files WHERE id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, id);
      int affectedRows = stmt.executeUpdate();
      return affectedRows > 0;
    }
  }

  public File findById(Long id) throws SQLException {
    String sql = "SELECT id, name, content_type, payload, created_at FROM files WHERE id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          File file = new File();
          file.setId(rs.getLong("id"));
          file.setName(rs.getString("name"));
          file.setContentType(rs.getString("content_type"));
          file.setPayload(rs.getString("payload"));
          file.setCreatedAt(rs.getTimestamp("created_at"));
          return file;
        }
      }
    }
    return null;
  }

  public PaginatedList findBy(Paginator paginator) throws SQLException {
    StringBuilder baseQuery = new StringBuilder("SELECT id, name, content_type, payload, created_at FROM files");
    StringBuilder countQuery = new StringBuilder("SELECT COUNT(*) FROM files");
    List<Object> params = new ArrayList<>();

    if (paginator.getFilters() != null) {
      List<String> conditions = new ArrayList<>();
      if (paginator.getFilters().containsKey("name")) {
        conditions.add("name LIKE ?");
        params.add("%" + paginator.getFilters().get("name") + "%");
      }
      if (!conditions.isEmpty()) {
        String whereClause = " WHERE " + String.join(" AND ", conditions);
        baseQuery.append(whereClause);
        countQuery.append(whereClause);
      }
    }

    String orderBy = paginator.getSortKey() != null ? paginator.getSortKey() : "id";
    String orderDirection = paginator.getSortDirection() != null ? paginator.getSortDirection() : "ASC";
    baseQuery.append(" ORDER BY ").append(orderBy).append(" ").append(orderDirection);

    int offset = (paginator.getPageNumber() - 1) * paginator.getPageSize();
    baseQuery.append(" LIMIT ? OFFSET ?");
    params.add(paginator.getPageSize());
    params.add(offset);

    List<File> files = new ArrayList<>();
    try (PreparedStatement stmt = connection.prepareStatement(baseQuery.toString())) {
      for (int i = 0; i < params.size(); i++) {
        stmt.setObject(i + 1, params.get(i));
      }
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          File file = new File();
          file.setId(rs.getLong("id"));
          file.setName(rs.getString("name"));
          file.setContentType(rs.getString("content_type"));
          file.setPayload(rs.getString("payload"));
          file.setCreatedAt(rs.getTimestamp("created_at"));
          files.add(file);
        }
      }
    }

    long totalCount = 0;
    try (PreparedStatement stmt = connection.prepareStatement(countQuery.toString())) {
      for (int i = 0; i < params.size() - 2; i++) {
        stmt.setObject(i + 1, params.get(i));
      }
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          totalCount = rs.getLong(1);
        }
      }
    }

    PaginatedList result = new PaginatedList<>();
    result.setPage(files);
    result.setTotalCount((int) totalCount);
    return result;
  }

  public List<File> searchByName(String name) throws SQLException {
    String sql = "SELECT id, name, content_type, payload, created_at FROM files WHERE name LIKE ? ORDER BY name";
    List<File> files = new ArrayList<>();
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, "%" + name + "%");
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          File file = new File();
          file.setId(rs.getLong("id"));
          file.setName(rs.getString("name"));
          file.setContentType(rs.getString("content_type"));
          file.setPayload(rs.getString("payload"));
          file.setCreatedAt(rs.getTimestamp("created_at"));
          files.add(file);
        }
      }
    }
    return files;
  }
}
