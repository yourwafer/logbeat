package com.xa.shushu.upload.datasource.service;

import com.alibaba.fastjson.JSON;
import com.xa.shushu.upload.datasource.config.EventConfig;
import com.xa.shushu.upload.datasource.config.Field;
import com.xa.shushu.upload.datasource.service.push.EventPush;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class LogEventDataConsumer {

    private final List<EventConfig> eventConfigs;

    private final EventPush eventPush;

    public LogEventDataConsumer(List<EventConfig> eventConfigs, EventPush eventPush) {
        this.eventConfigs = eventConfigs;
        this.eventPush = eventPush;
    }

    public void consume(String line) {
        log.debug("解析行数据[{}]", line);
        String[] cols = line.split("\t");
        consume(cols);
    }

    public void consume(String[] cols) {
        for (EventConfig eventConfig : eventConfigs) {
            Map<String, Object> values;
            try {
                values = parse(eventConfig, cols);

                processDefaultProperties(eventConfig, values);
            } catch (Exception e) {
                log.error("[{}]解析数据[{}]异常", eventConfig, JSON.toJSONString(cols), e);
                throw new RuntimeException(e);
            }
            eventPush.push(eventConfig, values);
        }
    }

    private void processDefaultProperties(EventConfig eventConfig, Map<String, Object> values) {
        String uploadType = eventConfig.getUploadType();
        if ("track".equals(uploadType)) {
            String eventName = eventConfig.getName();
            values.put("#event_name", eventName);
            values.put("#type", "track");
        } else {
            values.put("#type", uploadType);
        }

        String account = (String) values.get("#account_id");
        if (account == null) {
            return;
        }
        account = account.trim();
        int index = account.indexOf(".");
        if (index >= 0) {
            account = account.substring(0, index);
        }
        String[] userIdChannel = account.split("_");
        String userId = userIdChannel[0];
        String channelId;
        if (userIdChannel.length >= 2) {
            channelId = userIdChannel[1];
        } else {
            channelId = "115076";
        }
        //noinspection unchecked
        Map<String, Object> properties = (Map<String, Object>) values.computeIfAbsent("properties", k -> new HashMap<>());
        properties.put("channel", channelId);
        properties.put("userId", userId);
    }

    private Map<String, Object> parse(EventConfig eventConfig, String[] cols) {
        Map<String, Field> fields = eventConfig.getFields();
        Map<String, Object> values = new HashMap<>();

        values.put("#type", eventConfig.getUploadType());

        Map<String, Object> properties = new HashMap<>(fields.size());
        for (Map.Entry<String, Field> entry : fields.entrySet()) {
            String name = entry.getKey();
            Field field = entry.getValue();
            int index = field.getIndex();
            if (index > cols.length) {
                log.debug("日志[{}]列[{}]下标[{}]大约最大值[{}]", eventConfig.getName(), name, index, cols.length);
                continue;
            }
            Class<?> type = field.getType();
            Object value;
            String strValue = cols[index - 1];
            if (type == String.class) {
                value = strValue;
            } else {
                value = JSON.parseObject(strValue, type);
            }
            if (name.startsWith("#")) {
                values.put(name, value);
            } else if (name.startsWith("${")) {
                properties.put(getRealName(name, cols), value);
            } else {
                properties.put(name, value);
            }
        }

        if (!properties.isEmpty()) {
            values.put("properties", properties);
        }

        return values;
    }

    private String getRealName(String name, String[] cols) {
        String index = name.substring(name.indexOf("{") + 1, name.indexOf("}"));
        int indexValue = Integer.parseInt(index);
        return cols[indexValue - 1];
    }
}
