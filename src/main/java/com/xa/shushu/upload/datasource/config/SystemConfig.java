package com.xa.shushu.upload.datasource.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Data
@ConfigurationProperties(prefix = "xa.config")
public class SystemConfig {

    // 事件配置资源
    private Resource configPath;

    // serverlist配置
    private Resource serverListFile;

    // 游戏服数据库配置
    private DatabaseConfig database;

    // 数据开始时间
    private String startDay;

    // 数据开始时间(使用startDay解析获得)
    private LocalDate start;

    // 日志文件路径
    private LogFileConfig logSource;

    //日志输出类型
    private String pushType;

    public LocalDate getStart() {
        if (start != null) {
            return start;
        }
        if (startDay != null) {
            start = LocalDate.parse(startDay, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return start;
    }
}
