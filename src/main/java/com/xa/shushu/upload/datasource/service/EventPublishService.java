package com.xa.shushu.upload.datasource.service;

import com.xa.shushu.upload.datasource.service.push.ConsoleEventPush;
import com.xa.shushu.upload.datasource.service.push.FileDebugEventPush;
import com.xa.shushu.upload.datasource.service.push.FileEventPush;
import com.xa.shushu.upload.datasource.service.push.HttpEventPush;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class EventPublishService implements ApplicationContextAware {
    public final static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    private ApplicationContext applicationContext;

    private Map<Class<? extends EventPush>, EventPush> eventPushMap = new HashMap<>();

    public EventPush getEventPush(String type) {
        type = type.toUpperCase();
        switch (type) {
            case "FILE_DEBUG":
                return getOrCreateBean(FileDebugEventPush.class);
            case "FILE":
                return getOrCreateBean(FileEventPush.class);
            case "CONSOLE":
                return getOrCreateBean(ConsoleEventPush.class);
            case "HTTP":
                return getOrCreateBean(HttpEventPush.class);
            default:
                throw new IllegalStateException("unsupport type " + type);
        }
    }

    private EventPush getOrCreateBean(Class<? extends EventPush> clazz) {
        EventPush eventPush = eventPushMap.get(clazz);
        if (eventPush != null) {
            return eventPush;
        }
        AutowireCapableBeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();
        EventPush bean = beanFactory.createBean(clazz);
        eventPushMap.put(clazz, bean);
        return bean;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
