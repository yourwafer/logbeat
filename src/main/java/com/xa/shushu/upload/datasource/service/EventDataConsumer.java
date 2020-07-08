package com.xa.shushu.upload.datasource.service;

import com.xa.shushu.upload.datasource.config.EventConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class EventDataConsumer {

    private List<EventConfig> eventConfig;

    public EventDataConsumer(List<EventConfig> eventConfig) {
        this.eventConfig = eventConfig;
    }

    public void consume(String line) {
        log.debug("解析行数据[{}]", line);
        // TODO
    }
}
