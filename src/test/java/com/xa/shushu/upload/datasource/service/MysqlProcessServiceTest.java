package com.xa.shushu.upload.datasource.service;

import org.junit.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;

public class MysqlProcessServiceTest {

    @Test
    public void test_duration() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime localDateTime = now.plusMinutes(3);
        Duration between = Duration.between(now, localDateTime);
        assertEquals(3L, between.toMinutes());
        Duration neg = Duration.between(localDateTime, now);
        assertEquals(-3L, neg.toMinutes());

    }
}