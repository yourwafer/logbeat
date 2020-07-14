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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class LogTaskTest {

    private Path path;
    private Map<Integer, String> rows;
    private List<Path> paths = new ArrayList<>();

    /**
     * 测试所有数据小于4K
     */
    @Test
    public void test_single_buffer() {
        append(1024);
        LogPosition logPosition = LogPosition.of(1, 1, "testlog", "", LocalDate.now(), 0);
        LogTask logTask = new LogTask(logPosition, rows -> {
            for (String row : rows) {
                int rowNum = Integer.parseInt(row.split("\t")[0]);
                String remove = rows.remove(rowNum);
                assertEquals(remove, row);
            }
        }, l -> {
        }, this::buildFilePath);

        logTask.start();
        assertThat(rows.size(), is(0));

        append(1024);
        logTask.close();

        logTask.start();
        assertThat(rows.size(), is(0));
        logTask.close();
    }

    /**
     * 测试数据大于4K
     */
    @Test
    public void test_big_buffer() {
        append(1024 * 10);
        LogPosition logPosition = LogPosition.of(1, 1, "testlog", "", LocalDate.now(), 0);
        LogTask logTask = new LogTask(logPosition, rows -> {
            for (String row : rows) {
                int rowNum = Integer.parseInt(row.split("\t")[0]);
                String remove = rows.remove(rowNum);
                assertEquals(remove, row);
            }
        }, l -> {
        }, this::buildFilePath);

        logTask.start();
        assertThat(rows.size(), is(0));
    }

    /**
     * 测试大文件，同时修改文件
     */
    @Test
    public void test_big_buffer_append_file() {
        append(1024 * 10);
        LogPosition logPosition = LogPosition.of(1, 1, "testlog", "", LocalDate.now(), 0);
        LogTask logTask = new LogTask(logPosition, rows -> {
            for (String row : rows) {
                int rowNum = Integer.parseInt(row.split("\t")[0]);
                String remove = rows.remove(rowNum);
                assertEquals(remove, row);
            }
        }, l -> {
        }, this::buildFilePath);

        logTask.start();
        logTask.close();
        assertThat(rows.size(), is(0));

        append(1024 * 10);

        logTask.start();
        assertThat(rows.size(), is(0));
    }

    /**
     * 测试数据处理异常，回到开始位置继续处理
     */
    @Test
    public void test_exception_continue_read() {
        append(1024 * 10);
        AtomicBoolean consumerError = new AtomicBoolean(true);
        int lineNum = 100;
        LogPosition logPosition = LogPosition.of(1, 1, "testlog", "", LocalDate.now(), 0);
        LogTask logTask = new LogTask(logPosition, rows -> {
            for (String row : rows) {
                int rowNum = Integer.parseInt(row.split("\t")[0]);
                String remove = rows.remove(rowNum);
                assertEquals(remove, row);
            }
        }, l -> {
        }, this::buildFilePath);

        try {
            logTask.start();
        } catch (IllegalStateException e) {
            // ignore
        }
        logTask.start();
        assertThat(rows.size(), is(0));
    }

    /**
     * 测试多天日志数据
     */
    @Test
    public void test_mul_day() {
        append(1024 * 8);
        LocalDate now = LocalDate.now();

        LocalDate beforeOneDay = now.plusDays(-1);
        Path beforeOnePath = deleteAndCreateFile(beforeOneDay);
        Map<Integer, String> beforeOneRows = new HashMap<>();
        append(1024 * 8, beforeOnePath, beforeOneRows);

        LocalDate beforeTwoDay = beforeOneDay.plusDays(-1);
        Path beforeTwoPath = deleteAndCreateFile(beforeTwoDay);
        Map<Integer, String> beforeTwoRows = new HashMap<>();
        append(1024 * 8, beforeTwoPath, beforeTwoRows);

        LogPosition logPosition = LogPosition.of(1, 1, "testlog", "", beforeTwoDay, 0);
        LogTask logTask = new LogTask(logPosition, rows -> {
            for (String row : rows) {
                System.out.println(logPosition.getLastExecute() + "***" + row + "***");
                String s = row.split("\t")[0];
                int rowNum = Integer.parseInt(s);
                LocalDate current = logPosition.getLastExecute();
                String remove;
                if (current.equals(beforeTwoDay)) {
                    remove = beforeTwoRows.remove(rowNum);
                } else if (current.equals(beforeOneDay)) {
                    remove = beforeOneRows.remove(rowNum);
                } else {
                    remove = rows.remove(rowNum);
                }
                assertEquals(remove, row);
            }
        }, l -> {
        }, this::buildFilePath);
        logTask.start();
        assertThat(rows.size(), is(0));
        assertThat(beforeTwoRows.size(), is(0));
        assertThat(beforeOneRows.size(), is(0));
    }

    /**
     * 测试多天中，非当天最后一行数据没有换行符
     */
    @Test
    public void test_mul_day_with_no_r_n() {
        append(1024 * 8);
        LocalDate now = LocalDate.now();

        LocalDate beforeOneDay = now.plusDays(-1);
        Path beforeOnePath = deleteAndCreateFile(beforeOneDay);
        Map<Integer, String> beforeOneRows = new HashMap<>();
        append(1024 * 8, beforeOnePath, beforeOneRows);
        appendNoRN(beforeOnePath, beforeOneRows);

        LocalDate beforeTwoDay = beforeOneDay.plusDays(-1);
        Path beforeTwoPath = deleteAndCreateFile(beforeTwoDay);
        Map<Integer, String> beforeTwoRows = new HashMap<>();
        append(1024 * 8, beforeTwoPath, beforeTwoRows);
        appendNoRN(beforeTwoPath, beforeTwoRows);

        LogPosition logPosition = LogPosition.of(1, 1, "testlog", "", beforeTwoDay, 0);
        LogTask logTask = new LogTask(logPosition, rows -> {
            for (String row : rows) {
                String s = row.split("\t")[0];
                int rowNum = Integer.parseInt(s);
                LocalDate current = logPosition.getLastExecute();
                String remove;
                if (current.equals(beforeTwoDay)) {
                    remove = beforeTwoRows.remove(rowNum);
                } else if (current.equals(beforeOneDay)) {
                    remove = beforeOneRows.remove(rowNum);
                } else {
                    remove = rows.remove(rowNum);
                }
                assertEquals(remove, row);
            }
        }, l -> {
        }, this::buildFilePath);
        logTask.start();
        assertThat(rows.size(), is(0));
        assertThat(beforeTwoRows.size(), is(0));
        assertThat(beforeOneRows.size(), is(0));
    }

    String buildFilePath(String log, String type, int operator, int server, LocalDate time) {
        return this.path.toAbsolutePath().getParent().resolve("1_1_testlog." + time.format(DateTimeFormatter.ISO_DATE)).toAbsolutePath().toString();
    }

    @Before
    public void beforeCreateFile() {
        LocalDate now = LocalDate.now();
        this.path = deleteAndCreateFile(now);
    }

    private Path deleteAndCreateFile(LocalDate now) {
        Path path = Paths.get("1_1_testlog." + now.format(DateTimeFormatter.ISO_DATE));
        File file = path.toFile();
        if (file.exists()) {
            boolean delete = file.delete();
            System.out.println(delete + "删除测试文件" + path.toString());
        }
        boolean newFile = false;
        try {
            newFile = file.createNewFile();
            paths.add(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(newFile + "创建测试文件" + path.toString());
        return path;
    }

    @After
    public void removeTestFile() {
        for (Path path : paths) {
            del(path);
        }
        paths.clear();
    }

    private void del(Path path) {
        if (path == null) {
            return;
        }
        this.rows = null;
        File file = path.toFile();
        if (file.exists()) {
            boolean delete = file.delete();
            System.out.println(delete + "删除测试文件" + path.toString());
        }
    }

    void append(int size) {
        if (rows == null) {
            rows = new HashMap<>();
        }
        append(size, this.path, rows);
    }

    void append(int size, Path path, Map<Integer, String> rows) {
        File file = path.toFile();
        String row = "a\tb\t中文\t1\t2\t3\t\r\n";
        byte[] bytes = row.getBytes(StandardCharsets.UTF_8);
        int count = size / (bytes.length + 2);
        try {
            FileOutputStream out = new FileOutputStream(file, true);
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

    void appendNoRN(Path path, Map<Integer, String> rows) {
        File file = path.toFile();
        String row = "a\tb\t中文\t1\t2\t3\t";
        int line = rows.size() + 1;
        try {
            FileOutputStream out = new FileOutputStream(file, true);
            String newRow = line + "\t" + row;
            out.write(newRow.getBytes(StandardCharsets.UTF_8));
            rows.put(line, newRow);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}