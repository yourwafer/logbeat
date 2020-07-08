package com.xa.shushu.upload.datasource.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 数据来源配置
 */
@Data
@EqualsAndHashCode
@ToString
public class EventSource {
    // 来源类型，目前支持flog,tlog,mysql
    private String type;

    // 如果是flog或者tlog，那么就是日志类型
    private String name;

    public EventSource() {
    }

    public EventSource(String type, String name) {
        this.type = type;
        this.name = name;
    }
}
