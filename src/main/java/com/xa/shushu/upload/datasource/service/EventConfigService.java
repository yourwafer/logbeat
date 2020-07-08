package com.xa.shushu.upload.datasource.service;

import com.alibaba.fastjson.JSON;
import com.xa.shushu.upload.datasource.config.EventConfig;
import com.xa.shushu.upload.datasource.config.UploadConfig;
import com.xa.shushu.upload.datasource.resource.EventLogSetting;
import com.xa.shushu.upload.datasource.resource.Storage;
import com.xa.shushu.upload.datasource.resource.other.ResourceDefinition;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 事件配置服务
 */
@Service
@Slf4j
public class EventConfigService implements ApplicationContextAware {

    private Storage<Integer, EventLogSetting> eventLogSettingStorage;

    @Value("${upload-config-path:}")
    private String configPath;

    @Value("${excel-path:classpath:excel}")
    private String excelPath;

    //当前配置缓存
    private UploadConfig config;


    public UploadConfig getConfig() {
        if (config == null) {
            buildConfig();
        }
        return config;
    }

    private void buildConfig() {
        Map<String, EventConfig> map = buildEventMap();
        //默认配置属性大于Excel 如有相同EventConfig则取配置文件属性的相同属性将替换Excel中配置的
        UploadConfig uploadConfig = readFromConfig();
        for (EventConfig event : uploadConfig.getEvents()) {
            EventConfig config = map.get(event.toUniqueName());
            if (config != null) {
                config.merge(event);
                continue;
            }
            map.put(event.toUniqueName(), event);
        }
        uploadConfig.setEvents(new ArrayList<>(map.values()));
        this.config = uploadConfig;
    }

    //构建Excel中所有的EventConfig
    private Map<String, EventConfig> buildEventMap() {
        //获取全局属性
        List<EventLogSetting> index = eventLogSettingStorage.getIndex(EventLogSetting.COMMON, true);
        //公共属性和类型
        Map<String, Integer> commonFields = new HashMap<>();
        Map<String, Class<?>> commonTypes = new HashMap<>();
        for (EventLogSetting setting : index) {
            commonFields.put(setting.getName(), setting.getCsvIndex());
            commonTypes.put(setting.getName(), setting.getClazz());
        }

        //生成Excel中的所有 EventConfig
        Map<String, EventConfig> map = new HashMap<>(eventLogSettingStorage.getAll().size());
        for (EventLogSetting setting : eventLogSettingStorage.getAll()) {
            if (setting.isCommon()) {
                continue;
            }
            EventConfig config = map.computeIfAbsent(setting.toName(), (k) -> EventConfig.of(commonFields, commonTypes));
            //将Excel中的配置放入EventConfig中
            dealWithEventLog(setting, config);
        }
        return map;
    }

    //将Excel中的配置放入EventConfig中
    private void dealWithEventLog(EventLogSetting setting, EventConfig config) {
        //设置当前属性
        config.putField(setting.getName(), setting.getCsvIndex());
        //设置当前属性的Type类型
        config.putType(setting.getName(), setting.getClazz());
        //设置当前事件别名
        config.setDescribeOnce(setting.getRecordName());
        //设置当前事件名
        config.setNameOnce(setting.getRecordName());
        //设置当前事件名
        config.setEventSourceOnce(setting.getLogType(), setting.getRecordName());
        //设置数数后台处理类型
        config.setUploadType(setting.getType());
        //当为track类型时 设置event_name属性
        if (StringUtils.equals("track", setting.getSsType())) {
            if (setting.getEventName() == null) {
                log.error("当前ID[{}]属性[{}]的事件名配置不存在 请检查！！！", setting.getId(), setting.getName());
            }
            config.putDefaultValueIfAbsent("#event_name", setting.getEventName());
        }
    }

    private UploadConfig readFromConfig() {
        if (configPath == null || configPath.isEmpty()) {
            configPath = "classpath:config.json";
        }
        Path path;
        try {
            if (configPath.startsWith("classpath:") || !configPath.startsWith("/")) {
                path = applicationContext.getResource(configPath).getFile().toPath();
            } else {
                path = Paths.get(configPath);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return JSON.parseObject(bytes, UploadConfig.class);
    }

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        eventLogSettingStorage = new Storage<>();
        eventLogSettingStorage.initialize(new ResourceDefinition(EventLogSetting.class, excelPath), applicationContext);
        this.applicationContext = applicationContext;
    }
}
