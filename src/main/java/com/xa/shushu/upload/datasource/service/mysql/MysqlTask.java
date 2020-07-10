package com.xa.shushu.upload.datasource.service.mysql;

import com.xa.shushu.upload.datasource.config.MysqlConfig;
import com.xa.shushu.upload.datasource.config.ServerConfig;

public class MysqlTask {
    private final ServerConfig serverConfig;
    private final MysqlConfig mysqlConfig;

    public MysqlTask(ServerConfig serverConfig, MysqlConfig mysqlConfig) {
        this.serverConfig = serverConfig;
        this.mysqlConfig = mysqlConfig;
    }

    public void start() {

    }
}
