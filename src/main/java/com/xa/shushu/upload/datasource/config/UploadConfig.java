package com.xa.shushu.upload.datasource.config;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UploadConfig {

    // 运维使用的配置文件serverlist
    private String serverListFile;

    // 日志数据源配置
    private LogFileConfig logSource;

    // 数据库数据源
    private Map<String, MysqlConfig> mysqlSources;

    // 事件配置
    private List<EventConfig> events;
}
