package com.xa.shushu.upload.datasource.service.push;

import com.xa.shushu.upload.datasource.config.EventConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsoleEventPush implements EventPush {
    @Override
    public void push(EventConfig eventConfig, String values) {
        System.out.println(values);
    }
}
