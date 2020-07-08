package com.xa.shushu.upload.datasource.config;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
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
    private Map<String, Integer> fields = new HashMap<>();

    // 字段名称对应的数据类型
    private Map<String, Class<?>> types = new HashMap<>();

    // 默认配置数据
    private Map<String, Object> defaultValue = new HashMap<>();

    public void setNameOnce(String name) {
        if (StringUtils.isEmpty(this.name)) {
            this.name = name;
        }
    }

    public void setDescribeOnce(String describe) {
        if (StringUtils.isEmpty(this.describe)) {
            this.describe = describe;
        }
    }

    public void setEventSourceOnce(String type, String name) {
        if (this.source == null) {
            this.source = new EventSource(type, name);
        }
    }

    public void putDefaultValue(String key, Object value) {
        defaultValue.put(key, value);
    }

    public void putDefaultValueIfAbsent(String key, Object value) {
        defaultValue.putIfAbsent(key, value);
    }

    public void putField(String key, Integer value) {
        fields.put(key, value);
    }

    public void putType(String key, Class<?> value) {
        types.put(key, value);
    }

    public String toUniqueName() {
        String eventName = (String) defaultValue.getOrDefault("#event_name", StringUtils.EMPTY);
        return source.getName() + "_" + uploadType + "_" + eventName;
    }


    public void merge(EventConfig config) {
        fields.putAll(config.getFields());
        types.putAll(config.getTypes());
        defaultValue.putAll(config.getDefaultValue());
    }

    public static EventConfig of(Map<String, Integer> fields, Map<String, Class<?>> types) {
        EventConfig config = new EventConfig();
        config.fields = new HashMap<>(fields);
        config.types = new HashMap<>(types);
        return config;
    }
}
