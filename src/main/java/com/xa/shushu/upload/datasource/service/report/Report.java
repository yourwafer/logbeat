package com.xa.shushu.upload.datasource.service.report;

import lombok.Data;

import java.util.concurrent.atomic.LongAdder;

@Data
public class Report {
    // 上次更新时间
    long timestamp = System.currentTimeMillis() / 1000;

    // 此次运行共读取字节数
    LongAdder totalByte = new LongAdder();

    // 累计耗时
    long nanoTime;

    // 当前每秒读取数量
    long secondReadByte;

    // 历史最高读取速度
    long maxReadByte;

    // 累计读取行数
    long rows;

    // 累计post数据
    int totalPost;

    // 累计耗时
    int totalCost;
}
