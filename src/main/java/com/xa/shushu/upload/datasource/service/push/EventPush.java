package com.xa.shushu.upload.datasource.service.push;

import com.xa.shushu.upload.datasource.config.EventConfig;

import java.util.List;

public interface EventPush {
    void push(EventConfig eventConfig, String rows);
}
