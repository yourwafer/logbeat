package com.xa.shushu.upload.datasource.service;

import com.xa.shushu.upload.datasource.config.EventConfig;
import com.xa.shushu.upload.datasource.config.LogFileConfig;
import com.xa.shushu.upload.datasource.config.ServerConfig;
import com.xa.shushu.upload.datasource.config.UploadConfig;
import com.xa.shushu.upload.datasource.entity.LogPosition;
import com.xa.shushu.upload.datasource.repository.LogPositionRepository;
import com.xa.shushu.upload.datasource.service.file.LogTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class FileProcessService {

    private final LogPositionRepository logPositionRepository;

    // 日志开始处理时间
    private LocalDate startDay;

    // 全局配置
    private UploadConfig uploadConfig;

    // 服务器配置
    private Map<String, ServerConfig> serverConfigs;

    private final ConcurrentHashMap<String, List<LogTask>> logTasks = new ConcurrentHashMap<>();

    public FileProcessService(LogPositionRepository logPositionRepository) {
        this.logPositionRepository = logPositionRepository;
    }

    public void process(String logName, String type, ServerConfig serverConfig, List<EventConfig> eventConfigs) {
        EventDataConsumer eventDataConsumer = new EventDataConsumer(eventConfigs);

        String logKey = LogPosition.toKey(serverConfig.getOperator(), serverConfig.getServer(), logName);

        Optional<LogPosition> recordOptional = logPositionRepository.findById(logKey);

        LogPosition logPosition = recordOptional.orElseGet(() -> {
            LogPosition curLog = LogPosition.of(serverConfig.getOperator(), serverConfig.getServer(), logName, type, startDay, 0);
            logPositionRepository.save(curLog);
            return curLog;
        });

        LogTask logTask = new LogTask(logPosition,
                eventDataConsumer::consume,
                logPositionRepository::save,
                this::buildFilePath);

        logTask.start();

        List<LogTask> logTasks = this.logTasks.computeIfAbsent(logName, k -> new ArrayList<>());
        logTasks.add(logTask);
    }


    String buildFilePath(String log, String type, int operator, int server, LocalDate time) {
        ServerConfig serverConfig = serverConfigs.get(operator + "_" + server);
        LogFileConfig logSource = uploadConfig.getLogSource();
        StringJoiner joiner = new StringJoiner(File.separator);
        joiner.add(logSource.getRootDirs())
                .add(serverConfig.getPort())
                .add(logSource.getLogDir())
                .add(type);

        String timeFormat = time.format(DateTimeFormatter.ISO_LOCAL_DATE);
        joiner.add(timeFormat).add(operator + "_" + server + "_" + log + "." + timeFormat);

        return joiner.toString();
    }

    public void init(LocalDate startDay, UploadConfig uploadConfig, Map<String, ServerConfig> serverConfigs) {
        this.startDay = startDay;
        this.uploadConfig = uploadConfig;
        this.serverConfigs = serverConfigs;
    }

}
