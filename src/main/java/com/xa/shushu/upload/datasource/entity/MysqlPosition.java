package com.xa.shushu.upload.datasource.entity;

import lombok.Data;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity
@Data
@ToString
public class MysqlPosition {

    @Id
    private String id;

    // 运营商
    private int operator;

    // 服务器
    private int server;

    // mysql名字
    private String name;

    // 当前服最早开始的时间
    private LocalDateTime earliest;

    // 查询开始时间
    private LocalDateTime start;

    // 查询结束时间
    private LocalDateTime end;

    // 执行时间
    private LocalDateTime executeTime;

    public static MysqlPosition of(int operator, int server, String name) {
        MysqlPosition p = new MysqlPosition();
        p.id = toKey(operator, server, name);
        p.operator = operator;
        p.server = server;
        p.name = name;
        return p;
    }

    private static String toKey(int operator, int server, String name) {
        return operator + "_" + server + "_" + name;
    }

    public void updateExecute(LocalDateTime start, LocalDateTime end, LocalDateTime execute) {
        this.start = start;
        this.end = end;
        this.executeTime = execute;
    }
}
