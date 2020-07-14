package com.xa.shushu.upload.datasource.service.file;

import com.xa.shushu.upload.datasource.entity.LogPosition;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * 数据文件，一行不可超过 默认缓存大小(4K*2)，如果超过4K*2，则说明日志打印输出，存在问题，解决办法需要手动清理这一行
 */
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

    // 运行状态
    private volatile boolean running = true;

    private final int BUF_SIZE = 1024 * 16;
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
        if (!running) {
            log.info("任务终止[" + this + "]");
            return;
        }
        LocalDate lastExecute = logPosition.getLastExecute();
        LocalDate now = LocalDate.now();
        // 如果是前一天
        for (; !lastExecute.isAfter(now); lastExecute = lastExecute.plusDays(1)) {
            if (!running) {
                log.info("任务终止[" + this + "]");
                return;
            }
            String path = initAndClosePreFile(lastExecute);
            if (path != null) {
                log.info("日志文件不存在，忽视[{}]", path);
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
                log.info("重置位置[{}],开始解析处理文件[{}],", filePath, position);
            } catch (IOException e) {
                log.error("文件位置异常[{}]", position, e);
                break;
            }
            ByteBuffer byteBuffer = ByteBuffer.allocate(BUF_SIZE * 2);
            do {
                if (!running) {
                    log.info("任务终止[" + this + "]");
                    return;
                }
                Arrays.fill(buffer, (byte) 0);
                long curFilePosition = 0;
                try {
                    curFilePosition = randomAccessFile.getFilePointer();
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
                    if (byteBuffer.position() > 0) {
                        size += byteBuffer.position();
                    }
                    if (size == 0) {
                        // 排除当前字节（因为是换行符）
                        start = i + 1;
                        continue;
                    }
                    String line;
                    if (byteBuffer.position() > 0) {
                        byteBuffer.put(buffer, start, (cur - start));
                        byteBuffer.flip();
                        byte[] bytes = new byte[byteBuffer.remaining()];
                        byteBuffer.get(bytes);
                        line = new String(bytes, StandardCharsets.UTF_8);
                        byteBuffer.clear();
                    } else {
                        line = new String(buffer, start, (cur - start), StandardCharsets.UTF_8);
                    }

                    start = i + 1;

                    lineConsumer.accept(line);

                    int newPos = start;
                    if ((i + 1) < read) {
                        if (buffer[i + 1] == N || buffer[i + 1] == R) {
                            newPos += 1;
                        }
                    }

                    logPosition.setPosition(curFilePosition + newPos);
                    log.trace("变更文件位置[{}][{}]", logPosition.getPosition(), filePath);
                    save.accept(logPosition);

                    if (!running) {
                        log.info("任务终止[" + this + "]");
                        return;
                    }
                }

                if (start < read) {
                    byteBuffer.put(buffer, start, read - start);
                }
            } while (read > 0);
            if (byteBuffer.position() > 0 && !lastExecute.equals(now)) {
                byteBuffer.flip();
                int remaining = byteBuffer.remaining();
                byte[] remainBytes = new byte[remaining];
                byteBuffer.get(remainBytes);
                String line = new String(remainBytes, StandardCharsets.UTF_8);
                log.info("[{}]日志文件[{}]最后一行[{}]没有换行符", filePath, remaining, line);
                lineConsumer.accept(line);
            }
        }
    }

    private String initAndClosePreFile(LocalDate lastExecute) {
        String path = pathBuilder.buildFilePath(logPosition.getLog(),
                logPosition.getType(),
                logPosition.getOperator(),
                logPosition.getServer(),
                lastExecute);
        if (randomAccessFile != null && path.equals(this.filePath)) {
            return null;
        }

        if (randomAccessFile != null) {
            try {
                randomAccessFile.close();
            } catch (IOException e) {
                log.error("关闭文件[{}]异常", filePath, e);
            }
        }
        randomAccessFile = null;

        File file = new File(path);
        if (!file.exists()) {
            // 找不到文件，则认定今天日志已经消费完成
            logPosition.updateTime(lastExecute);
            logPosition.setPosition(-1);
            save.accept(logPosition);

            return path;
        }

        try {
            this.randomAccessFile = new RandomAccessFile(path, "r");
            this.filePath = path;
        } catch (FileNotFoundException e) {
            log.debug("日志[{}]不存在", path);
            logPosition.updateTime(lastExecute);
            logPosition.setPosition(-1);
            save.accept(logPosition);

            return path;
        }
        LocalDate pre = logPosition.getLastExecute();
        if (!pre.equals(lastExecute)) {
            logPosition.updateTime(lastExecute);
        }

        return null;
    }

    RandomAccessFile getRandomAccessFile() {
        return randomAccessFile;
    }

    // for test
    void close() {
        if (randomAccessFile != null) {
            try {
                randomAccessFile.close();
                randomAccessFile = null;
            } catch (IOException e) {
                log.error("关闭日志文件[{}]句柄异常", filePath, e);
            }
        }
    }

    @Override
    public String toString() {
        return "LogTask{" +
                "logPosition=" + logPosition +
                ", filePath='" + filePath + '\'' +
                '}';
    }

    public void shutdown() {
        this.running = false;
        log.info("日志任务终止[{}]", logPosition);
    }
}