package com.xa.shushu.upload.datasource.service.push;

import com.xa.shushu.upload.datasource.config.EventConfig;

import java.util.Map;

public interface EventPush {
    void push(EventConfig eventConfig, Map<String, Object> values);
}