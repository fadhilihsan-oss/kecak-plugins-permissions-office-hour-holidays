package com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.holiday.repository;

import com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.holiday.client.HolidayApiClient;
import com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.holiday.model.NationalHoliday;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Locale;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class HolidayRepository {
    private static final Pattern SQL_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_$]*");
    private static final String FIND_NATIONAL_HOLIDAY =
            "SELECT 1 FROM app_holiday_calendar "
                    + "WHERE holiday_date = ? AND holiday_type = ? AND year = ?";
    private static final String FIND_SYNCED_YEAR =
            "SELECT 1 FROM app_holiday_calendar "
                    + "WHERE year = ? AND holiday_type = ? AND source = ? LIMIT 1";
    private static final String DELETE_API_HOLIDAYS_FOR_YEAR =
            "DELETE FROM app_holiday_calendar WHERE year = ? AND holiday_type = ? AND source = ?";
    private static final String INSERT_NATIONAL_HOLIDAY =
            "INSERT INTO app_holiday_calendar "
                    + "(id, holiday_date, holiday_name, holiday_type, year, source) "
                    + "VALUES (?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE holiday_name = VALUES(holiday_name), "
                    + "year = VALUES(year), source = VALUES(source)";

    private final DataSource dataSource;

    public HolidayRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean containsNationalHoliday(LocalDate date) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_NATIONAL_HOLIDAY)) {
            statement.setDate(1, Date.valueOf(date));
            statement.setString(2, "NATIONAL");
            statement.setInt(3, date.getYear());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Unable to read Indonesian national holidays from app_holiday_calendar", e);
        }
    }

    public boolean containsHoliday(LocalDate date, String tableName, String dateColumn) {
        validateIdentifier(tableName, "holiday table");
        validateIdentifier(dateColumn, "holiday date column");
        String sql;
        try (Connection connection = dataSource.getConnection()) {
            Integer columnType = getColumnType(connection, tableName, dateColumn);
            if (columnType == null) {
                throw new IllegalArgumentException(
                        "The selected holiday table or date column is not available in the active database");
            }
            if (columnType != Types.DATE && columnType != Types.CHAR
                    && columnType != Types.VARCHAR && columnType != Types.LONGVARCHAR) {
                throw new IllegalArgumentException(
                        "The selected holiday date column must be a DATE or text column");
            }
            String quote = connection.getMetaData().getIdentifierQuoteString().trim();
            sql = "SELECT 1 FROM " + quoteIdentifier(tableName, quote)
                    + " WHERE " + quoteIdentifier(dateColumn, quote) + " = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                if (columnType == Types.DATE) {
                    statement.setDate(1, Date.valueOf(date));
                } else {
                    statement.setString(1, date.toString());
                }
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Unable to check holiday date in table " + tableName
                            + " and column " + dateColumn, e);
        }
    }

    public Boolean getHolidayStatus(LocalDate date, String tableName,
                                    String dateColumn, String statusColumn) {
        validateIdentifier(tableName, "holiday table");
        validateIdentifier(dateColumn, "holiday date column");
        validateIdentifier(statusColumn, "holiday status column");
        try (Connection connection = dataSource.getConnection()) {
            Integer dateType = getColumnType(connection, tableName, dateColumn);
            Integer statusType = getColumnType(connection, tableName, statusColumn);
            if (dateType == null || statusType == null) {
                throw new IllegalArgumentException(
                        "The selected holiday table, date column, or status column is not available "
                                + "in the active database");
            }
            if (!isDateColumnType(dateType)) {
                throw new IllegalArgumentException(
                        "The selected holiday date column must be a DATE or text column");
            }
            if (!isStatusColumnType(statusType)) {
                throw new IllegalArgumentException(
                        "The selected holiday status column must be a boolean, numeric, or text column");
            }

            String identifierQuote = connection.getMetaData().getIdentifierQuoteString();
            String quote = identifierQuote == null ? "" : identifierQuote.trim();
            String sql = "SELECT " + quoteIdentifier(statusColumn, quote)
                    + " FROM " + quoteIdentifier(tableName, quote)
                    + " WHERE " + quoteIdentifier(dateColumn, quote) + " = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                setDateParameter(statement, date, dateType);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    boolean active = parseActiveStatus(resultSet.getString(1));
                    if (resultSet.next()) {
                        throw new IllegalStateException(
                                "More than one holiday record exists for date " + date);
                    }
                    return active;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Unable to check holiday status in table " + tableName
                            + " and columns " + dateColumn + " and " + statusColumn, e);
        }
    }

    static boolean parseActiveStatus(String statusValue) {
        if (statusValue == null || statusValue.trim().isEmpty()) {
            return false;
        }
        String normalized = statusValue.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[\\s_-]+", "");
        if ("active".equals(normalized) || "aktif".equals(normalized)
                || "true".equals(normalized) || "1".equals(normalized)
                || "yes".equals(normalized) || "y".equals(normalized)
                || "on".equals(normalized) || "checked".equals(normalized)) {
            return true;
        }
        if ("inactive".equals(normalized) || "inaktif".equals(normalized)
                || "nonaktif".equals(normalized) || "nonactive".equals(normalized)
                || "false".equals(normalized) || "0".equals(normalized)
                || "no".equals(normalized) || "n".equals(normalized)
                || "off".equals(normalized) || "unchecked".equals(normalized)
                || "tidakaktif".equals(normalized)) {
            return false;
        }
        throw new IllegalArgumentException(
                "Unsupported holiday status value: " + statusValue
                        + ". Use Active/Inactive or a boolean value.");
    }

    private boolean isDateColumnType(int columnType) {
        return columnType == Types.DATE || columnType == Types.CHAR
                || columnType == Types.VARCHAR || columnType == Types.LONGVARCHAR;
    }

    private boolean isStatusColumnType(int columnType) {
        return columnType == Types.BOOLEAN || columnType == Types.BIT
                || columnType == Types.TINYINT || columnType == Types.SMALLINT
                || columnType == Types.INTEGER || columnType == Types.CHAR
                || columnType == Types.VARCHAR || columnType == Types.LONGVARCHAR;
    }

    private void setDateParameter(PreparedStatement statement, LocalDate date, int columnType)
            throws SQLException {
        if (columnType == Types.DATE) {
            statement.setDate(1, Date.valueOf(date));
        } else {
            statement.setString(1, date.toString());
        }
    }

    private Integer getColumnType(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String catalog = connection.getCatalog();
        try (ResultSet tables = metadata.getTables(catalog, null, tableName, new String[]{"TABLE"})) {
            if (!tables.next()) {
                return null;
            }
        }
        try (ResultSet columns = metadata.getColumns(catalog, null, tableName, columnName)) {
            while (columns.next()) {
                if (tableName.equals(columns.getString("TABLE_NAME"))
                        && columnName.equals(columns.getString("COLUMN_NAME"))) {
                    return columns.getInt("DATA_TYPE");
                }
            }
        }
        return null;
    }

    public static void validateIdentifier(String identifier, String description) {
        if (identifier == null || !SQL_IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Invalid " + description + ": " + identifier);
        }
    }

    private String quoteIdentifier(String identifier, String quote) {
        if (quote.isEmpty()) {
            return identifier;
        }
        return quote + identifier.replace(quote, quote + quote) + quote;
    }

    public boolean hasApiSyncedYear(int year) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_SYNCED_YEAR)) {
            statement.setInt(1, year);
            statement.setString(2, "NATIONAL");
            statement.setString(3, HolidayApiClient.SOURCE);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to check national holiday synchronization status", e);
        }
    }

    public void replaceApiHolidaysForYear(int year, List<NationalHoliday> holidays) {
        Connection connection = null;
        IllegalStateException failure = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            try (PreparedStatement delete = connection.prepareStatement(DELETE_API_HOLIDAYS_FOR_YEAR)) {
                delete.setInt(1, year);
                delete.setString(2, "NATIONAL");
                delete.setString(3, HolidayApiClient.SOURCE);
                delete.executeUpdate();
            }

            try (PreparedStatement insert = connection.prepareStatement(INSERT_NATIONAL_HOLIDAY)) {
                for (NationalHoliday holiday : holidays) {
                    insert.setString(1, UUID.randomUUID().toString());
                    insert.setDate(2, Date.valueOf(holiday.getDate()));
                    insert.setString(3, holiday.getName());
                    insert.setString(4, "NATIONAL");
                    insert.setInt(5, year);
                    insert.setString(6, HolidayApiClient.SOURCE);
                    insert.addBatch();
                }
                insert.executeBatch();
            }
            connection.commit();
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
            }
            failure = new IllegalStateException("Unable to save Indonesian national holidays", e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    if (failure == null) {
                        failure = new IllegalStateException("Unable to close holiday database connection", e);
                    } else {
                        failure.addSuppressed(e);
                    }
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
