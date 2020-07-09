package com.xa.shushu.upload.datasource.entity;

import lombok.Data;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDate;

@Entity
@Data
@ToString
public class LogPosition {

    @Id
    private String id;

    //运营商
    private int operator;

    // 服务器
    private int server;

    // 日志名称
    private String log;

    // 日志类型tlog，flog
    private String type;

    // 上次处理时间
    private LocalDate lastExecute;

    // 上次读取位置
    private long position;

    public static LogPosition of(int operator, int server, String log, String type, LocalDate time, int position) {
        LogPosition p = new LogPosition();
        p.id = toKey(operator, server, log);
        p.operator = operator;
        p.server = server;
        p.log = log;
        p.type = type;
        p.lastExecute = time;
        p.position = position;
        return p;
    }

    public static String toKey(int operator, int server, String log) {
        return operator + "_" + server + "_" + log;
    }

    public void updateTime(LocalDate lastExecute) {
        this.lastExecute = lastExecute;
        this.position = 0;
    }
}
