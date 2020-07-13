package com.xa.shushu.upload.datasource.service.mysql;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SqlExecutor {

    private static final String urlPrefix = "jdbc:mysql://localhost/";
    private static final String subFix = "?useUnicode=true&characterEncoding=utf-8";

    private static final String START_TIME_SQL = "SELECT c.createAt from ChargeLog c where c.`id` in (select min(id) from ChargeLog)";

    public static Connection connect(String database, String userName, String password) throws SQLException {
        String url = urlPrefix + database + subFix;
        return DriverManager.getConnection(url, userName, password);
    }

    public static LocalDateTime getStartTime(Connection connection) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(START_TIME_SQL);
        ResultSet resultSet = preparedStatement.executeQuery();
        boolean next = resultSet.next();
        if (!next) {
            return null;
        }
        Timestamp timestamp = resultSet.getTimestamp(1);
        return LocalDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault());
    }

    public static List<String[]> list(Connection connection, String sql, LocalDateTime start, LocalDateTime end) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        Instant instant = start.atZone(ZoneId.systemDefault()).toInstant();
        preparedStatement.setTimestamp(1, new Timestamp(instant.toEpochMilli()));
        Instant endInstant = end.atZone(ZoneId.systemDefault()).toInstant();
        preparedStatement.setTimestamp(2, new Timestamp(endInstant.toEpochMilli()));

        ResultSet resultSet = preparedStatement.executeQuery();
        int columnCount = resultSet.getMetaData().getColumnCount();
        List<String[]> values = new ArrayList<>();
        while (resultSet.next()) {
            String[] row = new String[columnCount];
            for (int i = 1; i <= columnCount; ++i) {
                Object object = resultSet.getObject(i);
                if (object == null) {
                    row[i - 1] = null;
                } else {
                    if (object instanceof Date) {
                        row[i - 1] = String.valueOf(((Date) object).getTime());
                    } else {
                        row[i - 1] = object.toString();
                    }
                }
            }
            values.add(row);
        }
        return values;
    }
}
