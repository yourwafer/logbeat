package com.xa.shushu.upload.datasource.service;

import com.alibaba.fastjson.JSON;
import com.xa.shushu.upload.datasource.config.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class UploadScheduler {

    @Value("${upload-config-path:}")
    private String configPath;

    private UploadConfig uploadConfig;

    // 服配置
    private Set<ServerConfig> serverConfigs = new HashSet<>();

    // 数据源对应事件配置
    private final Map<String, List<EventConfig>> typeMapping = new HashMap<>();

    // 激活的mysql配置
    private final Set<MysqlConfig> activeMysql = new HashSet<>();

    private final Set<String> activeLog = new HashSet<>();

    @PostConstruct
    public void init() throws BeansException {
        uploadConfig = buildConfig();

        // 初始化游戏服配置
        initServerConfig();

        // 解析并初始化配置
        parseAndInitConfig(uploadConfig);

        // 开始调度任务
        startScheduler();
    }

    private void startScheduler() {
        startLogScheduler();
    }

    private void startLogScheduler() {
        for (String log : activeLog) {
            for (ServerConfig serverConfig : serverConfigs) {
                String logKey = serverConfig.getOperator() + "_" + serverConfig.getServer() + "_" + log;

            }
        }
    }

    private void initServerConfig() {
        Path path = Paths.get(uploadConfig.getServerListFile());
        List<String> lines;
        try {
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
            int port = Integer.parseInt(split[2]);
            ServerConfig serverConfig = new ServerConfig(operator, server, port);
            serverConfigs.add(serverConfig);
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
                activeLog.add(name);
            }
            List<EventConfig> eventConfigs = typeMapping.computeIfAbsent(name, k -> new ArrayList<>());
            eventConfigs.add(eventConfig);
        }
    }

    private UploadConfig buildConfig() {
        if (configPath == null || configPath.isEmpty()) {
            configPath = "classpath:config.json";
        }
        Path path;
        try {
            if (configPath.startsWith("classpath:") || !configPath.startsWith("/")) {
                ClassPathResource classPathResource = new ClassPathResource(configPath);
                path = classPathResource.getFile().toPath();
            } else {
                path = Paths.get(configPath);
            }
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
