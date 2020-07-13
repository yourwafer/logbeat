package com.xa.shushu.upload.datasource.service.push;

import com.alibaba.fastjson.JSON;
import com.xa.shushu.upload.datasource.config.EventConfig;
import com.xa.shushu.upload.datasource.service.EventPublishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public class FileDebugEventPush implements EventPush {
    @Value("${xa.config.eventPush.filePath:.}")
    private String filePath;

    @Override
    public void push(EventConfig eventConfig, Map<String, Object> values) {
        String data = JSON.toJSONStringWithDateFormat(values, PushConfiguration.DEFAULT_DATE_FORMAT) + "\r\n";
        try (FileOutputStream outputStream = new FileOutputStream(getFileName(), true)) {
            outputStream.write(data.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("文件读取/写入异常", e);
            throw new RuntimeException(e);
        }
    }

    private String getFileName() {
        return filePath.endsWith(File.separator) ? filePath + "debugPush.txt" : filePath + File.separator + "debugPush.txt";
    }
}
