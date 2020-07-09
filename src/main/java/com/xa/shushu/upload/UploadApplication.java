package com.xa.shushu.upload;


import com.xa.shushu.upload.datasource.config.SystemConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.LocalDateTime;
import java.util.TimeZone;

@SpringBootApplication
@Slf4j

@EnableJpaRepositories("com.xa.shushu.upload.datasource.repository")
@EnableConfigurationProperties(SystemConfig.class)
public class UploadApplication {
    public static void main(String[] args) {
        TimeZone timeZone = TimeZone.getDefault();
        log.info("当前系统时区:{},时区名称:{},系统时间:{}", timeZone.getID(), timeZone.getDisplayName(), LocalDateTime.now());
        SpringApplication.run(UploadApplication.class, args);
    }
}
