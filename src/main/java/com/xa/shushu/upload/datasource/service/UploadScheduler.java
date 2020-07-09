package com.xa.shushu.upload.datasource.service;

import com.alibaba.fastjson.JSON;
import com.xa.shushu.upload.datasource.config.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class UploadScheduler {

    @Autowired
    private SystemConfig systemConfig;

    @Autowired
    private FileProcessService fileProcessService;

    @Autowired
    private ApplicationContext applicationContext;

    // 服配置
    private final Map<String, ServerConfig> serverConfigs = new HashMap<>();

    // 数据源对应事件配置
    private final Map<String, List<EventConfig>> typeMapping = new HashMap<>();

    // 激活的mysql配置
    private final Set<MysqlConfig> activeMysql = new HashSet<>();

    private final Map<String, String> activeLog = new HashMap<>();

    @PostConstruct
    public void init() throws BeansException {
        UploadConfig uploadConfig = buildConfig();
        Resource resource = applicationContext.getResource("classpath:EventLogSetting.xlsx");

        Map<String, EventConfig> map = EventConfigExcel.getConfig(resource);

        for (EventConfig event : uploadConfig.getEvents()) {
            EventConfig config = map.get(event.toUniqueName());
            if (config != null) {
                config.merge(event);
                continue;
            }
            map.put(event.toUniqueName(), event);
        }
        uploadConfig.setEvents(new ArrayList<>(map.values()));

        // 初始化游戏服配置
        initServerConfig();

        // 解析并初始化配置
        parseAndInitConfig(uploadConfig);

        fileProcessService.init(systemConfig, this.serverConfigs);

        // 开始调度任务
        startScheduler();
    }

    private void startScheduler() {
        startLogScheduler();
        startMysqlScheduler();
    }

    private void startMysqlScheduler() {
//        for (MysqlConfig mysqlConfig : activeMysql) {
        // TODO
//        }
    }

    private void startLogScheduler() {
        for (Map.Entry<String, String> entry : activeLog.entrySet()) {
            String log = entry.getKey();
            String type = entry.getValue();
            for (ServerConfig serverConfig : serverConfigs.values()) {
                fileProcessService.process(log, type, serverConfig, typeMapping.get(log));
            }
        }
    }

    private void initServerConfig() {
        Resource resource = systemConfig.getServerListFile();
        List<String> lines;
        try {
            Path path = resource.getFile().toPath();
            lines = Files.readAllLines(path);
        } catch (IOException e) {
            throw new RuntimeException("读取serverlist异常");
        }
        for (String line : lines) {
            if (StringUtils.isBlank(line)) {
                continue;
            }
            if (line.startsWith("#")) {
                continue;
            }
            String[] split = line.split("\\s");
            int operator = Integer.parseInt(split[0]);
            int server = Integer.parseInt(split[1]);
            String port = split[2];

            ServerConfig serverConfig = new ServerConfig(operator, server, port);
            serverConfigs.put(operator + "_" + server, serverConfig);
        }
    }

    private void parseAndInitConfig(UploadConfig uploadConfig) {
        List<EventConfig> events = uploadConfig.getEvents();

        Map<String, MysqlConfig> mysqlSources = uploadConfig.getMysqlSources();
        for (EventConfig eventConfig : events) {
            EventSource source = eventConfig.getSource();
            String type = source.getType();

            String name = source.getName();
            if (type.equals("mysql")) {
                MysqlConfig mysqlConfig = mysqlSources.get(name);
                if (mysqlConfig == null) {
                    throw new RuntimeException(source + " 没有找到指定mysql数据源配置");
                }
                mysqlConfig.setName(name);
                activeMysql.add(mysqlConfig);

            } else {
                if (!type.equals("tlog") && !type.equals("flog")) {
                    throw new RuntimeException(source + " type类型不符合规则，仅仅允许tlog,flog,mysql");
                }
                activeLog.put(name, type);
            }
            List<EventConfig> eventConfigs = typeMapping.computeIfAbsent(name, k -> new ArrayList<>());
            eventConfigs.add(eventConfig);
        }
    }

    private UploadConfig buildConfig() {
        Path path;
        try {
            Resource resource = systemConfig.getConfigPath();
            path = resource.getFile().toPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return JSON.parseObject(bytes, UploadConfig.class);
    }
}
