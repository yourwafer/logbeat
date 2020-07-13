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
            // 修复查询时间段，对于初次查询订单，直接查询开始到现在的订单数据，对于停服超过几个小时，则直接查询上次到现在时间之间的数据
            // 默认时间大概3到5分钟，那么初次将会查询太多次，导致mysql通信频繁
            LocalDateTime now = LocalDateTime.now();
            // 精确度到分钟
            LocalDateTime end = LocalDateTime.of(now.toLocalDate(), LocalTime.of(now.getHour(), now.getMinute()));
            LocalDateTime executeTime = end.plusMinutes(mysqlConfig.getInterval());
            mysqlPosition.updateExecute(mysqlPosition.getStart(), end, executeTime);
            save.accept(mysqlPosition);

            List<String[]> list = SqlExecutor.list(connection, mysqlConfig.getSql(), mysqlPosition.getStart(), mysqlPosition.getEnd());
            for (String[] row : list) {
                logEventDataConsumer.consume(row);
            }

            LocalDateTime start = mysqlPosition.getEnd();
            end = start.plusMinutes(mysqlConfig.getInterval());
            executeTime = end.plusMinutes(mysqlConfig.getInterval());
            mysqlPosition.updateExecute(start, end, executeTime);
            save.accept(mysqlPosition);
        }
    }
}
