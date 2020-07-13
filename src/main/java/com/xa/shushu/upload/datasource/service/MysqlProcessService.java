package com.xa.shushu.upload.datasource.service;

import com.xa.shushu.upload.datasource.config.EventConfig;
import com.xa.shushu.upload.datasource.config.MysqlConfig;
import com.xa.shushu.upload.datasource.config.ServerConfig;
import com.xa.shushu.upload.datasource.config.SystemConfig;
import com.xa.shushu.upload.datasource.entity.MysqlPosition;
import com.xa.shushu.upload.datasource.repository.MysqlPositionRepository;
import com.xa.shushu.upload.datasource.service.mysql.MysqlTask;
import com.xa.shushu.upload.datasource.service.mysql.SqlExecutor;
import com.xa.shushu.upload.datasource.utils.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MysqlProcessService {

    private final SystemConfig systemConfig;
    private final MysqlPositionRepository mysqlPositionRepository;
    private final EventPublishService eventPublishService;

    private final Map<String, List<MysqlTask>> tasks = new HashMap<>();

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("mysql查询线程"));

    public MysqlProcessService(SystemConfig systemConfig, MysqlPositionRepository mysqlPositionRepository, EventPublishService eventPublishService) {
        this.systemConfig = systemConfig;
        this.mysqlPositionRepository = mysqlPositionRepository;
        this.eventPublishService = eventPublishService;
    }

    public void process(ServerConfig serverConfig, MysqlConfig mysqlConfig, List<EventConfig> eventConfigs) {
        int operator = serverConfig.getOperator();
        int server = serverConfig.getServer();
        String key = operator + server + mysqlConfig.getName();
        Optional<MysqlPosition> optional = mysqlPositionRepository.findById(key);
        MysqlPosition mysqlPosition = optional.orElseGet(() -> {
            MysqlPosition position = MysqlPosition.of(operator, server, mysqlConfig.getName());
            mysqlPositionRepository.save(position);
            return position;
        });

        LogEventDataConsumer logEventDataConsumer = new LogEventDataConsumer(eventConfigs, eventPublishService.get());
        MysqlTask task = new MysqlTask(mysqlPosition,
                mysqlConfig,
                (this::buildConnect),
                mysqlPositionRepository::save,
                logEventDataConsumer);

        List<MysqlTask> logTasks = this.tasks.computeIfAbsent(mysqlConfig.getName(), k -> new ArrayList<>());
        logTasks.add(task);

        task.start();

        Runnable command = () -> {
            try {
                task.start();
            } catch (Exception e) {
                log.error("执行mysql数据查询错误[{}]", mysqlPosition, e);
            }
        };
        scheduledExecutorService.scheduleAtFixedRate(command, 0, mysqlConfig.getInterval(), TimeUnit.MINUTES);
    }

    private Connection buildConnect(int operator, int server) {
        String databaseName = buildDatabaseName(operator, server);
        try {
            return SqlExecutor.connect(databaseName, systemConfig.getDatabase().getUserName(), systemConfig.getDatabase().getPassword());
        } catch (SQLException throwables) {
            log.error("建立数据库连接失败[{}]", databaseName, throwables);
            throw new RuntimeException(throwables);
        }
    }

    private String buildDatabaseName(int operator, int server) {
        return systemConfig.getDatabase().getNamePrefix() + "_" + operator + "_" + server;
    }
}
