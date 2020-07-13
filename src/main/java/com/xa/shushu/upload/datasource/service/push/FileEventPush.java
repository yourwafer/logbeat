package com.xa.shushu.upload.datasource.service.push;

import com.alibaba.fastjson.JSON;
import com.xa.shushu.upload.datasource.config.EventConfig;
import com.xa.shushu.upload.datasource.service.EventPublishService;
import com.xa.shushu.upload.datasource.service.push.utils.LoggerFileWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Slf4j
public class FileEventPush implements EventPush {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Value("${ca.config.eventPush.filePath:.}")
    private String filePath;

    //输出的文件地址前缀
    private String filePrefix;

    //当前正在写入的LoggerFileWriter
    private LoggerFileWriter fileWriter;

    @PostConstruct
    void init() {
        if (StringUtils.isEmpty(filePath)) {
            throw new RuntimeException("filePath is empty");
        }
        filePrefix = filePath.endsWith(File.separator) ? filePath + "log." : filePath + File.separator + "log.";
    }

    @Override
    public void push(EventConfig eventConfig, Map<String, Object> values) {
        String data = JSON.toJSONStringWithDateFormat(values, PushConfiguration.DEFAULT_DATE_FORMAT);
        String file_name = getFileName();

        //当日期变更时 关闭之前写入的文件输出流
        if (fileWriter != null && !fileWriter.getFileName().equals(file_name)) {
            LoggerFileWriter.removeInstance(fileWriter);
            fileWriter = null;
        }

        if (fileWriter == null) {
            try {
                fileWriter = LoggerFileWriter.getInstance(file_name);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        fileWriter.write(data);
    }

    private String getFileName() {
        return filePrefix + dateFormat.format(new Date());
    }

}
