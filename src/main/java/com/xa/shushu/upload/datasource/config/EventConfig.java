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
    private Map<String, Field> fields = new HashMap<>();

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

    public void putField(String key, Integer index, Class<?> type) {
        fields.put(key, new Field(index, type));
    }

    public String toUniqueName() {
        return name + "_" + uploadType + "_" + source.getName();
    }

    public static EventConfig of(Map<String, Field> commonFields) {
        EventConfig eventConfig = new EventConfig();
        eventConfig.fields = commonFields;
        return eventConfig;
    }
}
