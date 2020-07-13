package com.xa.shushu.upload.datasource.config;

import lombok.Data;
import lombok.ToString;

import java.util.Objects;

@Data
@ToString
public class MysqlConfig {

    // 数据源名称
    private String name;

    // 查询sql
    private String sql;

    // 查询间隔（分钟）
    private int interval;

    // 参数个数，默认日期开始时间和当前时间
    private int params = 2;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MysqlConfig that = (MysqlConfig) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
