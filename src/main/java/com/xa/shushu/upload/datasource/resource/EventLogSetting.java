package com.xa.shushu.upload.datasource.resource;

import com.xa.shushu.upload.datasource.resource.anno.Id;
import com.xa.shushu.upload.datasource.resource.anno.Index;
import com.xa.shushu.upload.datasource.resource.anno.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Resource
@Getter
@Slf4j
public class EventLogSetting implements Validate {
    public static final String COMMON = "common";

    @Id
    private int id;

    //上传数据的标识名
    private String name;

    //对应的日志下标
    private int csvIndex;

    //对应的java类型
    private String type;

    //对应的数数处理的type类型
    private String ssType;

    //事件名
    private String eventName = StringUtils.EMPTY;

    //后台日志名
    private String recordName;

    @Index(name = COMMON)
    //是否为全局公共属性
    private boolean common;

    //日志类型
    private String logType;

    //当前属性的对应类型
    private Class<?> clazz;

    public String toName() {
        return recordName + "_" + ssType + "_" + eventName;
    }

    @Override
    public boolean isValid() {
        if (type == null) {
            return false;
        }
        try {
            clazz = Class.forName(type);
        } catch (ClassNotFoundException e) {
            log.error("无法找到当前id为[{}]属性为[{}]的classType", id, type, e);
            return false;
        }

        return true;
    }
}
