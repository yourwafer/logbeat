package com.xa.shushu.upload;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.time.LocalDateTime;
import java.util.TimeZone;

@SpringBootApplication
@Slf4j
@EnableMongoRepositories("com.xa.shushu.upload.datasource.repository")
public class UploadApplication {
    public static void main(String[] args) {
        TimeZone timeZone = TimeZone.getDefault();
        log.info("当前系统时区:{},时区名称:{},系统时间:{}", timeZone.getID(), timeZone.getDisplayName(), LocalDateTime.now());
        SpringApplication.run(UploadApplication.class, args);
    }
}
