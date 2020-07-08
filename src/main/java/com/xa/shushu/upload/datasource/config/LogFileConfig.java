package com.xa.shushu.upload.datasource.config;

import lombok.Data;

/**
 * 完整的日志路径为 {rootDirs}/{serverlist 配置的port}/{logDir}/
 */
@Data
public class LogFileConfig {

    // 日志目录路径
    private String rootDirs;

    // 日志相对路径，默认是logs
    private String logDir;

    // 每次读取时间间隔
    private int intervalSecond;

}
