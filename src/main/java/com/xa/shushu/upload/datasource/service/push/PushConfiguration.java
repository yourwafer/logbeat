package com.xa.shushu.upload.datasource.service.push;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration(proxyBeanMethods = false)
public class PushConfiguration {

    public final static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "xa.config.pushType.console", havingValue = "true")
    @Order(Ordered.HIGHEST_PRECEDENCE)
    ConsoleEventPush console() {
        return new ConsoleEventPush();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "xa.config.pushType.fileDebug", havingValue = "true")
    @Order(Ordered.HIGHEST_PRECEDENCE)
    FileDebugEventPush fileDebug() {
        return new FileDebugEventPush();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "xa.config.pushType.file", havingValue = "true")
    @Order(Ordered.HIGHEST_PRECEDENCE)
    FileEventPush file() {
        return new FileEventPush();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "xa.config.pushType.http", havingValue = "true")
    @Order(Ordered.HIGHEST_PRECEDENCE)
    HttpEventPush http() {
        return new HttpEventPush();
    }

    @Bean
    @ConditionalOnMissingBean(EventPush.class)
    @Order
    ConsoleEventPush consoleDefault() {
        return new ConsoleEventPush();
    }
}
