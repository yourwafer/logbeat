package com.xa.shushu.upload.datasource.service.mysql;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import java.sql.*;
import java.time.*;
import java.util.List;

import static org.junit.Assert.assertThat;

public class SqlExecutorTest {

    private final String url = "jdbc:h2:mem:testdb;MODE=MYSQL;DB_CLOSE_DELAY=-1";

    @Before
    public void setUp() throws ClassNotFoundException, SQLException {
        String driver = "org.h2.Driver";
        Class.forName(driver);
        Connection connection = DriverManager.getConnection(url);
        Statement statement = connection.createStatement();
        String sql = "CREATE TABLE `log` (\n" +
                "  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,\n" +
                "  `create` datetime DEFAULT NULL,\n" +
                "  PRIMARY KEY (`id`)\n" +
                ") ";
        statement.execute(sql);
        statement.close();
        String insertSql = "insert into log values(?,?)";
        PreparedStatement preparedStatement = connection.prepareStatement(insertSql);
        int i = 1;
        LocalDate now = LocalDate.now();
        for (int hour = 0; hour < 24; ++hour) {
            for (int minute = 0; minute < 60; ++minute) {
                LocalTime localTime = LocalTime.of(hour, minute);
                preparedStatement.setInt(1, i++);
                Instant instant = LocalDateTime.of(now, localTime).atZone(ZoneId.systemDefault()).toInstant();
                preparedStatement.setTimestamp(2, new Timestamp(instant.toEpochMilli()));
                preparedStatement.executeUpdate();
            }
        }
        preparedStatement.close();
        connection.close();
    }

    @Test
    public void name() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        LocalDate now = LocalDate.now();
        LocalTime begin = LocalTime.of(0, 0);
        LocalDateTime start = LocalDateTime.of(now, begin);
        String sql = "select id,`create` from log where `create` >= ? and `create` < ?";
        LocalDateTime end = start.plusDays(1);
        for (; start.isBefore(end); start = start.plusMinutes(1)) {
            List<String[]> list = SqlExecutor.list(connection, sql, start, start.plusMinutes(1));
            assertThat(list.size(), CoreMatchers.is(1));
        }
    }
}