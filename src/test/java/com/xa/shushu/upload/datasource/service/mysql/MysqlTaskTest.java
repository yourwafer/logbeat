package com.xa.shushu.upload.datasource.service.mysql;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.Assert.assertTrue;

public class MysqlTaskTest {
    @Test
    public void test_execute_interval() {
        int interval = 4;
        LocalDateTime startTime = LocalDateTime.now();
        int minute = startTime.getMinute();
        int count = minute / interval;
        LocalTime startMinute = LocalTime.of(startTime.getHour(), interval * count);
        LocalDateTime start = LocalDateTime.of(startTime.toLocalDate(), startMinute);
        LocalDateTime end = LocalDateTime.of(startTime.toLocalDate(), startMinute.plusMinutes(interval));
        LocalDateTime execute = end.plusMinutes(interval);
        assertTrue(start.isBefore(startTime));
        assertTrue(end.isAfter(startTime));
        assertTrue(execute.isAfter(end));
    }
}