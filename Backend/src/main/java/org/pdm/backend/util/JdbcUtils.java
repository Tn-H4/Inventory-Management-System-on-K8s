package org.pdm.backend.util;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Set;

public class JdbcUtils {

    /**
     * Sets a timestamp in the PreparedStatement if the LocalDateTime is not null;
     * otherwise, sets the column to NULL.
     */
    public static void setTimestampOrNull(PreparedStatement ps, int index, LocalDateTime value) throws SQLException {
        if (value != null) {
            ps.setTimestamp(index, Timestamp.valueOf(value));
        } else {
            ps.setNull(index, Types.TIMESTAMP);
        }
    }

    /**
     * Sets a long in the PreparedStatement if the Long is not null;
     * otherwise, sets the column to NULL.
     */
    public static void setLongOrNull(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value != null) {
            ps.setLong(index, value);
        } else {
            ps.setNull(index, Types.BIGINT);
        }
    }

    /**
     * Reads a nullable DATETIME column from the ResultSet and returns a LocalDateTime,
     * or null if the column value is SQL NULL.
     */
    public static LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toLocalDateTime() : null;
    }

    /**
     * Generates a safe ORDER BY clause based on a whitelist of allowed columns.
     * Returns " ORDER BY [column] [direction]" or a default order if the requested column is invalid.
     */
    public static String buildOrderClause(String sortBy, String sortDir, Set<String> allowedColumns) {
        String column = (sortBy == null || !allowedColumns.contains(sortBy)) ? "id" : sortBy;
        String direction = "DESC".equalsIgnoreCase(sortDir) ? "DESC" : "ASC";
        return " ORDER BY " + column + " " + direction;
    }
}
