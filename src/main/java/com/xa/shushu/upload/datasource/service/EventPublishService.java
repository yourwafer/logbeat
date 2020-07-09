package com.xa.shushu.upload.datasource.service;

import com.alibaba.fastjson.JSON;
import com.xa.shushu.upload.datasource.config.EventConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class EventPublishService implements EventPush {
    @Override
    public void push(EventConfig eventConfig, Map<String, Object> values) {
        String json = JSON.toJSONString(values);
        log.debug("上传[{}]数据[{}]", eventConfig.getName(), json);
    }
}
