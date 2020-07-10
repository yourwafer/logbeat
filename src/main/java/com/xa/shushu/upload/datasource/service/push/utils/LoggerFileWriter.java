package com.xa.shushu.upload.datasource.service.push.utils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LoggerFileWriter {
    //当前输出的文件名
    private final String fileName;
    //当前文件输出流
    private final FileOutputStream outputStream;
    //写入时需要加锁的输出流 默认为自身
    private final FileOutputStream lockStream;
    //引用计数器
    private int refCount;

    //所以LoggerFileWriter 持有者
    private static final Map<String, LoggerFileWriter> instances = new HashMap<>();

    public static LoggerFileWriter getInstance(final String fileName) throws FileNotFoundException {
        return getInstance(fileName, null);
    }

    public static LoggerFileWriter getInstance(final String fileName, final String lockFileName) throws FileNotFoundException {
        synchronized (instances) {
            if (!instances.containsKey(fileName)) {
                instances.put(fileName, new LoggerFileWriter(fileName, lockFileName));
            }
            LoggerFileWriter writer = instances.get(fileName);
            writer.refCount++;
            return writer;
        }
    }

    public static void removeInstance(final LoggerFileWriter writer) {
        synchronized (instances) {
            writer.refCount--;
            if (writer.refCount == 0) {
                writer.close();
                instances.remove(writer.fileName);
            }
        }
    }

    LoggerFileWriter(final String fileName, final String lockFileName) throws FileNotFoundException {
        this.outputStream = new FileOutputStream(fileName, true);
        if (lockFileName != null) {
            this.lockStream = new FileOutputStream(lockFileName, true);
        } else {
            this.lockStream = this.outputStream;
        }

        this.fileName = fileName;
        this.refCount = 0;
    }

    private void close() {
        try {
            outputStream.close();
        } catch (Exception e) {
            throw new RuntimeException("fail to close tga outputStream.", e);
        }
    }

    public String getFileName() {
        return this.fileName;
    }

    public boolean write(final String sb) {
        synchronized (this.lockStream) {
            FileLock lock = null;
            try {
                final FileChannel channel = lockStream.getChannel();
                lock = channel.lock(0, Long.MAX_VALUE, false);
                outputStream.write(sb.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException("failed to write tga file.", e);
            } finally {
                if (lock != null) {
                    try {
                        lock.release();
                    } catch (IOException e) {
                        throw new RuntimeException("failed to release tga file lock.", e);
                    }
                }
            }
        }
        return true;
    }
}
