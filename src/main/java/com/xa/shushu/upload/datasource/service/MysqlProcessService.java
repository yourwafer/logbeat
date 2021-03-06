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
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MysqlProcessService {

    private final SystemConfig systemConfig;
    private final MysqlPositionRepository mysqlPositionRepository;
    private final EventPublishService eventPublishService;

    private volatile boolean running = true;
    private final AtomicInteger error = new AtomicInteger();
    private final ConfigurableApplicationContext applicationContext;

    private final Map<String, List<MysqlTask>> tasks = new HashMap<>();

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("mysql查询线程"));

    public MysqlProcessService(SystemConfig systemConfig,
                               MysqlPositionRepository mysqlPositionRepository,
                               EventPublishService eventPublishService,
                               ConfigurableApplicationContext applicationContext) {
        this.systemConfig = systemConfig;
        this.mysqlPositionRepository = mysqlPositionRepository;
        this.eventPublishService = eventPublishService;
        this.applicationContext = applicationContext;
    }

    public void process(ServerConfig serverConfig, MysqlConfig mysqlConfig, List<EventConfig> eventConfigs) {
        int operator = serverConfig.getOperator();
        int server = serverConfig.getServer();
        String key = operator + "_" + server + "_" + mysqlConfig.getName();
        Optional<MysqlPosition> optional = mysqlPositionRepository.findById(key);
        MysqlPosition mysqlPosition = optional.orElseGet(() -> {
            MysqlPosition position = MysqlPosition.of(operator, server, mysqlConfig.getName());
            mysqlPositionRepository.save(position);
            return position;
        });

        LogEventDataConsumer logEventDataConsumer = new LogEventDataConsumer(eventConfigs, eventPublishService.get());
        MysqlTask task = new MysqlTask(mysqlPosition,
                mysqlConfig,
                this::buildConnect,
                mysqlPositionRepository::save,
                logEventDataConsumer);

        List<MysqlTask> logTasks = this.tasks.computeIfAbsent(mysqlConfig.getName(), k -> new ArrayList<>());
        logTasks.add(task);

        Runnable command = new Runnable() {
            @Override
            public void run() {
                if (!running) {
                    log.info("mysql任务队列已终止，忽视任务");
                    return;
                }
                try {
                    task.start();
                    error.set(0);
                    if (!running) {
                        return;
                    }
                } catch (Exception e) {
                    int times = error.incrementAndGet();
                    log.error("[{}]执行mysql数据查询错误[{}]", times, mysqlPosition, e);
                    if (times >= 10) {
                        log.error("错误次数大于10次，任务终止");
                        applicationContext.close();
                        return;
                    }
                    if (!running) {
                        return;
                    }
                    scheduledExecutorService.schedule(this, mysqlConfig.getInterval(), TimeUnit.MINUTES);
                    LocalDateTime now = LocalDateTime.now();
                    log.debug("mysql任务[{}]下次执行时间[{}][{}]",
                            mysqlConfig.getName(),
                            mysqlConfig.getInterval(),
                            now.plusMinutes(mysqlConfig.getInterval())
                    );
                    return;
                }
                long delay = mysqlConfig.getInterval();
                TimeUnit unit = TimeUnit.MINUTES;
                if (mysqlPosition.getExecuteTime() != null) {
                    delay = Duration.between(LocalDateTime.now(), mysqlPosition.getExecuteTime()).toMillis();
                    unit = TimeUnit.MILLISECONDS;
                }
                scheduledExecutorService.schedule(this, delay, unit);
                LocalDateTime now = LocalDateTime.now();
                log.debug("mysql任务[{}]下次执行时间[{}][{}]",
                        mysqlConfig.getName(),
                        mysqlConfig.getInterval(),
                        now.plusMinutes(mysqlConfig.getInterval())
                );
            }
        };
        scheduledExecutorService.schedule(command, mysqlConfig.getInterval(), TimeUnit.MINUTES);
        LocalDateTime now = LocalDateTime.now();
        log.debug("mysql任务[{}]下次执行时间[{}][{}]",
                mysqlConfig.getName(),
                mysqlConfig.getInterval(),
                now.plusMinutes(mysqlConfig.getInterval())
        );
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
        return systemConfig.getDatabase().getNamePrefix() + operator + "_" + server;
    }

    @PreDestroy
    public void close() {
        this.running = false;
        for (List<MysqlTask> tasks : tasks.values()) {
            for (MysqlTask task : tasks) {
                task.close();
            }
        }
        scheduledExecutorService.shutdownNow();
        try {
            scheduledExecutorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("不会走到的正常逻辑", e);
        }
    }

    public List<MysqlPosition> getMysqls() {
        return tasks.values().stream().flatMap(ts -> ts.stream().map(MysqlTask::getMysqlPosition)).collect(Collectors.toList());
    }

    public boolean isRunning() {
        return running;
    }
}
