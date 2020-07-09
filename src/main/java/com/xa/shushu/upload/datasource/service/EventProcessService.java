package com.xa.shushu.upload.datasource.service;

import com.xa.shushu.upload.datasource.config.EventConfig;
import com.xa.shushu.upload.datasource.config.MysqlConfig;
import com.xa.shushu.upload.datasource.config.ServerConfig;
import com.xa.shushu.upload.datasource.config.SystemConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class EventProcessService {

    @Autowired
    private FileProcessService fileProcessService;

    @Autowired
    private MysqlProcessService mysqlProcessService;

    public void init(SystemConfig systemConfig, Map<String, ServerConfig> serverConfigs) {
        fileProcessService.init(systemConfig, serverConfigs);
    }

    public void log(String log, String type, ServerConfig serverConfig, List<EventConfig> eventConfigs) {
        fileProcessService.process(log, type, serverConfig, eventConfigs);
    }

    public void mysql(ServerConfig serverConfig, MysqlConfig mysqlConfig, List<EventConfig> eventConfigs) {
        mysqlProcessService.process(serverConfig, mysqlConfig, eventConfigs);
    }
}
