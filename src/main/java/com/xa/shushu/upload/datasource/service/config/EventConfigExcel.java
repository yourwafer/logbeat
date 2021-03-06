package com.xa.shushu.upload.datasource.service.config;

import com.xa.shushu.upload.datasource.config.EventConfig;
import com.xa.shushu.upload.datasource.config.Field;
import com.xa.shushu.upload.datasource.resource.EventLogSetting;
import com.xa.shushu.upload.datasource.resource.Storage;
import com.xa.shushu.upload.datasource.resource.other.ResourceDefinition;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 事件配置服务
 */
@Slf4j
public class EventConfigExcel {

    public static Map<String, EventConfig> getConfig(Resource resource) {
        Storage<Integer, EventLogSetting> eventLogSettingStorage = new Storage<>();
        eventLogSettingStorage.initialize(new ResourceDefinition(EventLogSetting.class, resource));
        return buildEventMap(eventLogSettingStorage);
    }

    //构建Excel中所有的EventConfig
    private static Map<String, EventConfig> buildEventMap(Storage<Integer, EventLogSetting> eventLogSettingStorage) {
        //获取全局属性
        List<EventLogSetting> index = eventLogSettingStorage.getIndex(EventLogSetting.COMMON, true);
        //公共属性和类型
        Map<String, Field> commonFields = new HashMap<>();
        for (EventLogSetting setting : index) {
            commonFields.put(setting.getName(), new Field(setting.getCsvIndex(), setting.getClazz()));
        }

        //生成Excel中的所有 EventConfig
        Map<String, EventConfig> map = new HashMap<>(eventLogSettingStorage.getAll().size());
        for (EventLogSetting setting : eventLogSettingStorage.getAll()) {
            if (setting.isCommon()) {
                continue;
            }
            EventConfig config = map.computeIfAbsent(setting.toName(), (k) -> EventConfig.of(new HashMap<>(commonFields)));
            //将Excel中的配置放入EventConfig中
            dealWithEventLog(setting, config);
        }
        return map;
    }

    //将Excel中的配置放入EventConfig中
    private static void dealWithEventLog(EventLogSetting setting, EventConfig config) {
        //设置当前属性
        config.putField(setting.getName(), setting.getCsvIndex(), setting.getClazz());
        //设置当前事件别名
        config.setDescribeOnce(setting.getRecordName());
        //设置当前事件名
        config.setNameOnce(setting.getEventName());
        //设置当前事件名
        config.setEventSourceOnce(setting.getLogType(), setting.getRecordName());
        //设置数数后台处理类型
        config.setUploadType(setting.getSsType());
    }
}
