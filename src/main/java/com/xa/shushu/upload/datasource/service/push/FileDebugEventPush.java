package com.xa.shushu.upload.datasource.service.push;

import com.xa.shushu.upload.datasource.config.EventConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
public class FileDebugEventPush implements EventPush {
    @Value("${xa.config.eventPush.filePath:.}")
    private String filePath;

    @Override
    public void push(EventConfig eventConfig, String data) {
        try (FileOutputStream outputStream = new FileOutputStream(getFileName(eventConfig.getName()), true)) {
            outputStream.write(data.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("文件读取/写入异常", e);
            throw new RuntimeException(e);
        }
    }

    private String getFileName(String event) {
        return filePath.endsWith(File.separator) ? filePath + event + "-debugPush.txt" : filePath + File.separator + event + "-debugPush.txt";
    }
}
