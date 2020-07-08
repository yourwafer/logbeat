package com.xa.shushu.upload.datasource.service;

import org.junit.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class FileProcessServiceTest {

    @Test
    public void test_buildFilePath() {
        StringJoiner joiner = new StringJoiner("/");
        joiner.add("/root/dir")
                .add("8001")
                .add("logs")
                .add("flog");
        LocalDate time = LocalDate.parse("2020-07-07", DateTimeFormatter.ISO_LOCAL_DATE);
        String timeFormat = time.format(DateTimeFormatter.ISO_LOCAL_DATE);
        joiner.add(timeFormat).add(20 + "_" + 2 + "_" + "CurrencyLog" + "." + timeFormat);

        assertEquals(joiner.toString(), "/root/dir/8001/logs/flog/2020-07-07/20_2_CurrencyLog.2020-07-07");
    }

    @Test
    public void test_line_end() {
        String str = "AB中文AB\n\r";
        byte[] bytes = str.getBytes(Charset.defaultCharset());
        StringJoiner joiner = new StringJoiner(",");
        for (byte b : bytes) {
            joiner.add(String.valueOf(b));
        }
        assertArrayEquals(bytes, new byte[]{65,66,-28,-72,-83,-26,-106,-121,65,66,10,13});
    }
}