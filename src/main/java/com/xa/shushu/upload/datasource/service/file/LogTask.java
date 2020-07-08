package com.xa.shushu.upload.datasource.service.file;

import com.xa.shushu.upload.datasource.entity.LogPosition;
import com.xa.shushu.upload.datasource.service.RowPhase;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.function.Consumer;

@Slf4j
public class LogTask {

    // 日志记录实体
    private final LogPosition logPosition;

    // 每读取一行数据通知消费
    private final Consumer<String> lineConsumer;

    // 日期读取记录变更通知
    private final Consumer<LogPosition> save;

    // 日志路径构建器
    private final LogPathBuilder pathBuilder;

    // 文件句柄
    private RandomAccessFile randomAccessFile;
    // 文件路径
    private String filePath;
    private final int BUF_SIZE = 1024 * 4;
    private final byte[] buffer = new byte[BUF_SIZE];

    private static final byte N = 10;
    private static final byte R = 13;

    public LogTask(LogPosition logPosition,
                   Consumer<String> lineConsumer,
                   Consumer<LogPosition> save,
                   LogPathBuilder pathBuilder) {
        this.logPosition = logPosition;
        this.lineConsumer = lineConsumer;
        this.save = save;
        this.pathBuilder = pathBuilder;
    }

    /**
     * 每一次，将可以读取的数据全部读取完毕，由外部调度器定时调度
     */
    public void start() {
        LocalDate lastExecute = logPosition.getLastExecute();
        LocalDate now = LocalDate.now();
        // 如果是前一天
        for (; !lastExecute.isAfter(now); lastExecute = lastExecute.plusDays(1)) {
            LocalDate newTime = initAndClosePreFile(lastExecute);
            if (newTime != lastExecute) {
                continue;
            }
            long position = logPosition.getPosition();
            if (position < 0) {
                log.error("异常逻辑代码,文件[{}][{}]", this.filePath, position);
                break;
            }
            int read = -1;
            try {
                randomAccessFile.seek(position);
            } catch (IOException e) {
                log.error("文件位置异常[{}]", position, e);
                break;
            }
            ByteBuffer byteBuffer = null;
            do {
                Arrays.fill(buffer, (byte) 0);
                try {
                    read = randomAccessFile.read(buffer);
                } catch (IOException e) {
                    log.error("读取日志数据异常[{}]", filePath, e);
                }
                if (read == -1) {
                    break;
                }
                int start = 0;
                int cur;
                for (int i = 0; i < read; ++i) {
                    byte b = buffer[i];
                    // \n\r
                    if (b != N && b != R) {
                        continue;
                    }
                    cur = i;

                    int size = cur - start;
                    if (byteBuffer != null) {
                        size += byteBuffer.remaining();
                    }
                    if (size == 0) {
                        // 排除当前字节（因为是换行符）
                        start = i + 1;
                        continue;
                    }
                    String line;
                    if (byteBuffer != null) {
                        byteBuffer.put(buffer, start, (cur - start));
                        byte[] bytes = new byte[byteBuffer.remaining()];
                        byteBuffer.get(bytes);
                        byteBuffer = null;
                        line = new String(bytes, StandardCharsets.UTF_8);
                    } else {
                        line = new String(buffer, start, (cur - start), StandardCharsets.UTF_8);
                    }

                    start = i + 1;

                    lineConsumer.accept(line);

                    int newPos = start + 1;
                    if ((i + 1) < read) {
                        if (buffer[i + 1] == N || buffer[i + 1] == R) {
                            newPos += 1;
                        }
                    }

                    logPosition.setPosition(position + newPos);
                    save.accept(logPosition);
                }
                if (start < read) {
                    byteBuffer = ByteBuffer.allocate(BUF_SIZE);
                    byteBuffer.put(buffer, start, read - start);
                }
            } while (read > 0);
        }
    }

    private LocalDate initAndClosePreFile(LocalDate lastExecute) {
        String path = pathBuilder.buildFilePath(logPosition.getLog(),
                logPosition.getType(),
                logPosition.getOperator(),
                logPosition.getServer(),
                lastExecute);
        if (randomAccessFile != null && path.equals(this.filePath)) {
            return lastExecute;
        }

        if (randomAccessFile != null) {
            try {
                randomAccessFile.close();
            } catch (IOException e) {
                log.error("关闭文件[{}]异常", filePath, e);
                randomAccessFile = null;
            }
        }

        File file = new File(path);
        if (!file.exists()) {
            // 找不到文件，则认定今天日志已经消费完成
            logPosition.updateTime(lastExecute);
            logPosition.setPosition(-1);
            save.accept(logPosition);

            lastExecute = lastExecute.plusDays(1);
            return lastExecute;
        }

        try {
            this.randomAccessFile = new RandomAccessFile(path, "r");
            this.filePath = path;
        } catch (FileNotFoundException e) {
            log.debug("日志[{}]不存在", path);
            logPosition.updateTime(lastExecute);
            logPosition.setPosition(-1);
            save.accept(logPosition);

            lastExecute = lastExecute.plusDays(1);
            return lastExecute;
        }
        LocalDate pre = logPosition.getLastExecute();
        if (!pre.equals(lastExecute)) {
            logPosition.updateTime(lastExecute);
        }

        return lastExecute;
    }
}