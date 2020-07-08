package com.xa.shushu.upload.datasource.service;

import com.xa.shushu.upload.datasource.config.EventConfig;

import java.util.List;

public class EventDataConsumer {

    private List<EventConfig> eventConfig;

    public EventDataConsumer(List<EventConfig> eventConfig) {
        this.eventConfig = eventConfig;
    }

    public void consume(String line) {
        // TODO
    }
}
