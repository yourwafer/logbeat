package com.xa.shushu.upload.datasource.service;

import com.xa.shushu.upload.datasource.config.EventConfig;
import com.xa.shushu.upload.datasource.config.MysqlConfig;
import com.xa.shushu.upload.datasource.config.ServerConfig;
import com.xa.shushu.upload.datasource.config.SystemConfig;
import com.xa.shushu.upload.datasource.service.mysql.MysqlTask;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MysqlProcessService {

    private SystemConfig systemConfig;

    private Map<String, MysqlTask> tasks = new HashMap<>();

    public MysqlProcessService(SystemConfig systemConfig) {
        this.systemConfig = systemConfig;
    }

    public void process(ServerConfig serverConfig, MysqlConfig mysqlConfig, List<EventConfig> eventConfigs) {
        String key = serverConfig.getOperator() + serverConfig.getServer() + mysqlConfig.getName();

    }
}
