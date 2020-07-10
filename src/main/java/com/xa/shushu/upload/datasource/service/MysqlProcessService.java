package com.xa.shushu.upload.datasource.service;

import com.xa.shushu.upload.datasource.config.EventConfig;
import com.xa.shushu.upload.datasource.config.MysqlConfig;
import com.xa.shushu.upload.datasource.config.ServerConfig;
import com.xa.shushu.upload.datasource.config.SystemConfig;
import com.xa.shushu.upload.datasource.entity.MysqlPosition;
import com.xa.shushu.upload.datasource.repository.MysqlPositionRepository;
import com.xa.shushu.upload.datasource.service.mysql.MysqlTask;
import com.xa.shushu.upload.datasource.service.mysql.SqlExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class MysqlProcessService {

    private SystemConfig systemConfig;
    private final MysqlPositionRepository mysqlPositionRepository;

    private final Map<String, MysqlTask> tasks = new HashMap<>();

    public MysqlProcessService(SystemConfig systemConfig, MysqlPositionRepository mysqlPositionRepository) {
        this.systemConfig = systemConfig;
        this.mysqlPositionRepository = mysqlPositionRepository;
    }

    public void process(ServerConfig serverConfig, MysqlConfig mysqlConfig, List<EventConfig> eventConfigs) {
        int operator = serverConfig.getOperator();
        int server = serverConfig.getServer();
        String key = operator + server + mysqlConfig.getName();
        Optional<MysqlPosition> optional = mysqlPositionRepository.findById(key);
        MysqlPosition mysqlPosition = optional.orElseGet(() -> {
            try (Connection connection = buildConnect(operator, server)) {
                LocalDateTime startTime = SqlExecutor.getStartTime(connection);

            } catch (SQLException throwables) {
                log.error("创建mysql查询异常[{}][{}]", serverConfig, mysqlConfig, throwables);
                throw new RuntimeException(throwables);
            }
            return null;
        });
        MysqlTask task = new MysqlTask(serverConfig, mysqlConfig);
        task.start();
    }

    private Connection buildConnect(int operator, int server) throws SQLException {
        String databaseName = buildDatabaseName(operator, server);
        return SqlExecutor.connect(databaseName, systemConfig.getDatabase().getUserName(), systemConfig.getDatabase().getPassword());
    }

    private String buildDatabaseName(int operator, int server) {
        return systemConfig.getDatabase().getNamePrefix() + "_" + operator + "_" + server;
    }
}
