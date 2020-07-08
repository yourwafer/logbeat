package com.xa.shushu.upload.datasource.service.file;


import com.xa.shushu.upload.datasource.entity.LogPosition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class LogTaskTest {

    private Path path;
    private Map<Integer, String> rows;

    @Test
    public void test_single_buffer() {
        append(1024);
        LogPosition logPosition = LogPosition.of(1, 1, "testlog", "", LocalDate.now(), 0);
        LogTask logTask = new LogTask(logPosition, l -> {
            String row = l[0].toString();
            int rowNum = Integer.parseInt(row.split("\t")[0]);
            String remove = rows.remove(rowNum);
            assertEquals(remove, row);
        }, l -> {
        }, this::buildFilePath, line -> new Object[]{line});
        logTask.start();
        assertThat(rows.size(), is(0));
    }

    String buildFilePath(String log, String type, int operator, int server, LocalDate time) {
        return path.toFile().getAbsolutePath();
    }

    @Before
    public void append() {
        LocalDate now = LocalDate.now();
        this.path = Paths.get("1_1_testlog." + now.format(DateTimeFormatter.ISO_DATE));
        File file = path.toFile();
        if (file.exists()) {
            boolean delete = file.delete();
            System.out.println(delete + "删除测试文件" + path.toString());
        }
        boolean newFile = false;
        try {
            newFile = file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(newFile + "创建测试文件" + path.toString());
    }

    @After
    public void tearDown() throws Exception {
        if (path == null) {
            return;
        }
        File file = path.toFile();
        if (file.exists()) {
            file.delete();
        }
    }

    void append(int size) {
        File file = path.toFile();
        String row = "a\tb\t中文\t1\t2\t3\t\r\n";
        byte[] bytes = row.getBytes(StandardCharsets.UTF_8);
        int count = size / (bytes.length + 2);
        rows = new HashMap<>(count);
        try {
            FileOutputStream out = new FileOutputStream(file);
            for (int i = 1; i <= count; ++i) {
                String newRow = i + "\t" + row;
                out.write(newRow.getBytes(StandardCharsets.UTF_8));
                rows.put(i, newRow.substring(0, newRow.length() - 2));
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}