package com.xa.shushu.upload.datasource.config;

import lombok.Data;

import java.util.Map;

/**
 * 事件配置
 */
@Data
public class EventConfig {

    // 事件名称
    private String name;

    // 描述信息
    private String describe;

    // 数据来源
    private EventSource source;

    // 上报类型 track,user_set,user_del
    private String uploadType;

    // 字段名称对应数据数组下表
    private Map<String, Integer> fields;

    // 默认配置数据
    private Map<String, Object> defaultValue;
}
