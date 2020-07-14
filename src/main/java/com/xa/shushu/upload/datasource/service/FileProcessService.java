package com.xa.shushu.upload.datasource.service;

import com.xa.shushu.upload.datasource.config.EventConfig;
import com.xa.shushu.upload.datasource.config.LogFileConfig;
import com.xa.shushu.upload.datasource.config.ServerConfig;
import com.xa.shushu.upload.datasource.config.SystemConfig;
import com.xa.shushu.upload.datasource.entity.LogPosition;
import com.xa.shushu.upload.datasource.repository.LogPositionRepository;
import com.xa.shushu.upload.datasource.service.file.LogTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class FileProcessService {

    private final LogPositionRepository logPositionRepository;
    private final EventPublishService eventPublishService;

    // 日志开始处理时间
    private LocalDate startDay;

    // 全局配置
    private SystemConfig systemConfig;

    // 服务器配置
    private Map<String, ServerConfig> serverConfigs;

    private volatile boolean running = true;

    private final Map<String, List<LogTask>> logTasks = new HashMap<>();
    private Thread readThread;

    public FileProcessService(LogPositionRepository logPositionRepository, EventPublishService eventPublishService) {
        this.logPositionRepository = logPositionRepository;
        this.eventPublishService = eventPublishService;
    }

    public void process(String logName, String type, ServerConfig serverConfig, List<EventConfig> eventConfigs) {
        LogEventDataConsumer logEventDataConsumer = new LogEventDataConsumer(eventConfigs, eventPublishService.get());

        String logKey = LogPosition.toKey(serverConfig.getOperator(), serverConfig.getServer(), logName);

        Optional<LogPosition> recordOptional = logPositionRepository.findById(logKey);

        LogPosition logPosition = recordOptional.orElseGet(() -> {
            LogPosition curLog = LogPosition.of(serverConfig.getOperator(), serverConfig.getServer(), logName, type, startDay, 0);
            logPositionRepository.save(curLog);
            return curLog;
        });

        LogTask logTask = new LogTask(logPosition,
                logEventDataConsumer::consume,
                logPositionRepository::save,
                this::buildFilePath);

        log.info("初始化日志读取任务[{}]", logTask);

        List<LogTask> logTasks = this.logTasks.computeIfAbsent(logName, k -> new ArrayList<>());
        logTasks.add(logTask);
    }

    String buildFilePath(String log, String type, int operator, int server, LocalDate time) {
        ServerConfig serverConfig = serverConfigs.get(operator + "_" + server);
        LogFileConfig logSource = systemConfig.getLogSource();
        StringJoiner joiner = new StringJoiner(File.separator);
        joiner.add(logSource.getRootDirs())
                .add(serverConfig.getPort())
                .add(logSource.getLogDir())
                .add(type);

        String timeFormat = time.format(DateTimeFormatter.ISO_LOCAL_DATE);
        joiner.add(operator + "_" + server + "_" + log + "." + timeFormat);

        return joiner.toString();
    }

    public void init(SystemConfig systemConfig, Map<String, ServerConfig> serverConfigs) {
        this.startDay = systemConfig.getStart();
        this.systemConfig = systemConfig;
        this.serverConfigs = serverConfigs;

        readThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(Math.min(60, systemConfig.getLogSource().getIntervalSecond()) * 1000);
                } catch (InterruptedException e) {
                    // 忽视
                    log.debug("日志读取线程被唤醒[{}]", running);
                }
                if (!running) {
                    log.error("日志读取调度线程终止");
                    break;
                }
                execute();
            }
        });
        readThread.start();
    }

    private void execute() {
        for (List<LogTask> tasks : logTasks.values()) {
            for (LogTask logTask : tasks) {
                try {
                    logTask.start();
                } catch (Exception e) {
                    log.error("调度日志任务[{}]异常", logTask, e);
                }
            }
        }
    }

    @PreDestroy
    public void close() {
        running = false;
        readThread.interrupt();
        for (List<LogTask> tasks : logTasks.values()) {
            for (LogTask logTask : tasks) {
                logTask.shutdown();
            }
        }
        while (readThread.isAlive()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // wait for done
            }
        }
    }

}
