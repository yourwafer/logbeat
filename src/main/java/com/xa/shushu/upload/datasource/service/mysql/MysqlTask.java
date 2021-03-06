package com.xa.shushu.upload.datasource.service.mysql;

import com.xa.shushu.upload.datasource.config.MysqlConfig;
import com.xa.shushu.upload.datasource.entity.MysqlPosition;
import com.xa.shushu.upload.datasource.service.LogEventDataConsumer;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class MysqlTask {
    private final MysqlPosition mysqlPosition;
    private final MysqlConfig mysqlConfig;

    private final ConncectionBuilder conncectionBuilder;
    private Consumer<MysqlPosition> save;

    private LogEventDataConsumer logEventDataConsumer;

    // 运行状态
    private volatile boolean running = true;

    public MysqlTask(MysqlPosition mysqlPosition,
                     MysqlConfig mysqlConfig,
                     ConncectionBuilder conncectionBuilder,
                     Consumer<MysqlPosition> save,
                     LogEventDataConsumer logEventDataConsumer) {
        this.mysqlPosition = mysqlPosition;
        this.mysqlConfig = mysqlConfig;
        this.conncectionBuilder = conncectionBuilder;
        this.save = save;
        this.logEventDataConsumer = logEventDataConsumer;
    }

    public void start() {
        if (!running) {
            log.info("任务终止[" + this + "]");
            return;
        }
        LocalDateTime earliest = mysqlPosition.getEarliest();
        try (Connection connection = conncectionBuilder.connection(mysqlPosition.getOperator(), mysqlPosition.getServer())) {
            if (earliest == null) {
                LocalDateTime startTime = SqlExecutor.getStartTime(connection);
                if (startTime == null) {
                    log.debug("未找到开始时间，忽视[{}]", mysqlConfig);
                    return;
                }
                int interval = mysqlConfig.getInterval();
                int minute = startTime.getMinute();
                int count = minute / interval;
                LocalTime startMinute = LocalTime.of(startTime.getHour(), interval * count);
                LocalDateTime start = LocalDateTime.of(startTime.toLocalDate(), startMinute);

                LocalDateTime end = LocalDateTime.of(startTime.toLocalDate(), startMinute.plusMinutes(interval));

                LocalDateTime execute = end.plusMinutes(interval);

                mysqlPosition.updateExecute(start, end, execute);
                mysqlPosition.setEarliest(startTime);
                save.accept(mysqlPosition);
            }
            doExecute(connection);

        } catch (SQLException throwables) {
            log.error("执行mysql查询异常[{}]", mysqlPosition, throwables);
            throw new RuntimeException(throwables);
        }
    }

    private void doExecute(Connection connection) throws SQLException {
        if (mysqlPosition.getExecuteTime() == null) {
            log.debug("任务没有确定执行之间，忽视[{}]", mysqlPosition);
            return;
        }
        while (mysqlPosition.getExecuteTime().isBefore(LocalDateTime.now())) {
            if (!running) {
                return;
            }
            // 修复查询时间段，对于初次查询订单，直接查询开始到现在的订单数据，对于停服超过几个小时，则直接查询上次到现在时间之间的数据
            // 默认时间大概3到5分钟，那么初次将会查询太多次，导致mysql通信频繁
            LocalDateTime now = LocalDateTime.now();
            // 精确度到分钟,查询现在一分钟以前的数据
            LocalDateTime end = LocalDateTime.of(now.toLocalDate(), LocalTime.of(now.getHour(), now.getMinute())).plusMinutes(-1);

            // 逻辑上不可能，但还是做个兼容
            if (end.isBefore(mysqlPosition.getStart())) {
                mysqlPosition.setExecuteTime(mysqlPosition.getExecuteTime().plusMinutes(1));
                save.accept(mysqlPosition);
                return;
            }

            LocalDateTime executeTime = end.plusMinutes(mysqlConfig.getInterval());
            mysqlPosition.updateExecute(mysqlPosition.getStart(), end, executeTime);
            save.accept(mysqlPosition);

            if (!running) {
                return;
            }
            log.debug("执行mysql检索数据[{}][{}][{}][{}][{}]", mysqlConfig.getName(),
                    mysqlPosition.getStart(),
                    mysqlPosition.getEnd(),
                    mysqlPosition.getExecuteTime(),
                    mysqlConfig.getSql());
            List<String[]> list = SqlExecutor.list(connection, mysqlConfig.getSql(), mysqlPosition.getStart(), mysqlPosition.getEnd());
            if (!running) {
                return;
            }
            logEventDataConsumer.consumeArray(list);

            LocalDateTime start = mysqlPosition.getEnd();
            end = start.plusMinutes(mysqlConfig.getInterval());
            executeTime = end.plusMinutes(mysqlConfig.getInterval());
            mysqlPosition.updateExecute(start, end, executeTime);
            mysqlPosition.addRow(list.size());
            save.accept(mysqlPosition);
            if (!running) {
                return;
            }
        }
    }

    public void close() {
        this.running = false;
        log.info("mysql任务终止[{}]", mysqlPosition);
    }

    public MysqlPosition getMysqlPosition() {
        return mysqlPosition;
    }
}
