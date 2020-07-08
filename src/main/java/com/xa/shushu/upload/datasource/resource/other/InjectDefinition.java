package com.xa.shushu.upload.datasource.resource.other;

import com.xa.shushu.upload.datasource.resource.anno.InjectBean;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;

/**
 * 注入信息定义
 *
 * @author frank
 */
public class InjectDefinition {

    /**
     * 被注入的属性
     */
    private final Field field;
    /**
     * 注入配置
     */
    private final InjectBean inject;
    /**
     * 注入类型
     */
    private final InjectType type;

    public InjectDefinition(Field field) {
        if (field == null) {
            throw new IllegalArgumentException("被注入属性域不能为null");
        }
        if (!field.isAnnotationPresent(InjectBean.class)) {
            throw new IllegalArgumentException("被注入属性域" + field.getName() + "的注释配置缺失");
        }
        field.setAccessible(true);

        this.field = field;
        this.inject = field.getAnnotation(InjectBean.class);
        if (StringUtils.isEmpty(this.inject.value())) {
            this.type = InjectType.CLASS;
        } else {
            this.type = InjectType.NAME;
        }
    }

    /**
     * 获取注入值
     *
     * @param applicationContext
     * @return
     */
    public Object getValue(ApplicationContext applicationContext) {
        if (InjectType.NAME.equals(type)) {
            String value = inject.value();
            boolean containsBean = applicationContext.containsBean(value);
            if (!containsBean) {
                return null;
            }
            return applicationContext.getBean(value);
        } else {
            Class<?> type = field.getType();
            String[] beanNamesForType = applicationContext.getBeanNamesForType(type);
            if (ArrayUtils.isEmpty(beanNamesForType)) {
                return null;
            }
            return applicationContext.getBean(type);
        }
    }

    // Getter and Setter ...

    public InjectType getType() {
        return type;
    }

    public Field getField() {
        return field;
    }

    public InjectBean getInject() {
        return inject;
    }

}
