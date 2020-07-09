package com.xa.shushu.upload.datasource.service.file;

import com.alibaba.fastjson.JSON;
import com.xa.shushu.upload.datasource.config.EventConfig;
import com.xa.shushu.upload.datasource.service.EventPush;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class LogEventDataConsumer {

    private List<EventConfig> eventConfigs;

    private EventPush eventPush;

    public LogEventDataConsumer(List<EventConfig> eventConfigs, EventPush eventPush) {
        this.eventConfigs = eventConfigs;
        this.eventPush = eventPush;
    }

    public void consume(String line) {
        log.debug("解析行数据[{}]", line);
        String[] cols = line.split("\t");
        for (EventConfig eventConfig : eventConfigs) {
            Map<String, Object> values = parse(eventConfig, cols);
            eventPush.push(eventConfig, values);
        }
    }

    private Map<String, Object> parse(EventConfig eventConfig, String[] cols) {
        Map<String, Integer> fields = eventConfig.getFields();
        Map<String, Class<?>> types = eventConfig.getTypes();
        Map<String, Object> values = new HashMap<>();
        Map<String, Object> properties = new HashMap<>(fields.size());
        for (Map.Entry<String, Integer> entry : fields.entrySet()) {
            String name = entry.getKey();
            Integer index = entry.getValue();
            if (index >= cols.length) {
                log.debug("日志[{}]列[{}]下标[{}]大约最大值[{}]", eventConfig.getName(), name, index, cols.length);
                continue;
            }
            Class<?> type = types.getOrDefault(name, String.class);
            Object value;
            if (type == String.class) {
                value = cols[index];
            } else {
                value = JSON.parseObject(cols[index], type);
            }
            if (name.startsWith("#")) {
                values.put(name, value);
            } else {
                properties.put(name, value);
            }
        }
        if (!properties.isEmpty()) {
            values.put("properties", properties);
        }
        return values;
    }
}