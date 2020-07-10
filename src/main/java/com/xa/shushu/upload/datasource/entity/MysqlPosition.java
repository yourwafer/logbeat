package com.xa.shushu.upload.datasource.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity
@Data
public class MysqlPosition {

    @Id
    private String id;

    // 运营商
    private int operator;

    // 服务器
    private int server;

    // mysql名字
    private String name;

    // 查询开始时间
    private LocalDateTime start;

    // 查询结束时间
    private LocalDateTime end;

    // 执行时间
    private LocalDateTime executeTime;
}
