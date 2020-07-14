package com.xa.shushu.upload.datasource.service.push;

import com.alibaba.fastjson.JSON;
import com.xa.shushu.upload.datasource.config.EventConfig;
import com.xa.shushu.upload.datasource.service.EventPublishService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class ConsoleEventPush implements EventPush {
    @Override
    public void push(EventConfig eventConfig, List<String> values) {
        log.debug("上传[{}]数据[{}]", eventConfig.getName(), JSON.toJSONString(values));
    }
}
