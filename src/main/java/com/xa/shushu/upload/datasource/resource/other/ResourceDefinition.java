package com.xa.shushu.upload.datasource.resource.other;


import com.xa.shushu.upload.datasource.resource.Validate;
import com.xa.shushu.upload.datasource.resource.anno.InjectBean;
import com.xa.shushu.upload.datasource.resource.anno.Resource;
import com.xa.shushu.upload.datasource.resource.utils.ReflectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.ReflectionUtils.FieldFilter;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

/**
 * 资源定义信息对象
 *
 * @author frank
 */
public class ResourceDefinition {

    public final static String FILE_SPLIT = ".";
    public final static String FILE_PATH = File.separator;

    /**
     * 注入属性域过滤器
     */
    private final static FieldFilter INJECT_FILTER = new FieldFilter() {
        @Override
        public boolean matches(Field field) {
            if (field.isAnnotationPresent(InjectBean.class)) {
                return true;
            }
            return false;
        }
    };

    /**
     * 资源类
     */
    private final Class<?> clz;
    /**
     * 资源路径
     */
    private final String location;
    /**
     * 资源路径
     */
    private String resolverLocation;
    /**
     * 资源路径
     */
    private final String path;

    /**
     * 资源的注入信息
     */
    private final Set<InjectDefinition> injects = new HashSet<InjectDefinition>();

    /**
     * 构造方法
     */
    public ResourceDefinition(Class<?> clz, String location) {
        this.clz = clz;
        this.location = location;
        Resource anno = clz.getAnnotation(Resource.class);
        StringBuilder builder = new StringBuilder();
        // builder.append(format.getLocation());
        // builder.append(FILE_PATH);
        if (ArrayUtils.isNotEmpty(anno.value())) {
            String[] dirs = anno.value();
            StringJoiner joiner = new StringJoiner(FILE_PATH);
            for (CharSequence path : dirs) {
                if (StringUtils.isBlank(path)) {
                    continue;
                }
                joiner.add(path);
            }
            String dir = joiner.toString();
            if (StringUtils.isNotEmpty(dir)) {
                int start = 0;
                int end = dir.length();
                if (StringUtils.startsWith(dir, FILE_PATH)) {
                    start++;
                }
                if (StringUtils.endsWith(dir, FILE_PATH)) {
                    end--;
                }
                builder.append(dir.substring(start, end)).append(FILE_PATH);
            }
        }
        builder.append(clz.getSimpleName()).append(FILE_SPLIT).append("xlsx");
        this.path = builder.toString();
        ReflectionUtils.doWithDeclaredFields(clz, new FieldCallback() {
            @Override
            public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                InjectDefinition definition = new InjectDefinition(field);
                injects.add(definition);
            }
        }, INJECT_FILTER);
    }

    /**
     * 获取静态属性注入定义
     *
     * @return
     */
    public Set<InjectDefinition> getStaticInjects() {
        HashSet<InjectDefinition> result = new HashSet<InjectDefinition>();
        for (InjectDefinition definition : this.injects) {
            Field field = definition.getField();
            if (Modifier.isStatic(field.getModifiers())) {
                result.add(definition);
            }
        }
        return result;
    }

    /**
     * 获取非静态属性注入定义
     *
     * @return
     */
    public Set<InjectDefinition> getInjects() {
        HashSet<InjectDefinition> result = new HashSet<InjectDefinition>();
        for (InjectDefinition definition : this.injects) {
            Field field = definition.getField();
            if (!Modifier.isStatic(field.getModifiers())) {
                result.add(definition);
            }
        }
        return result;
    }

    /**
     * 资源是否需要校验
     *
     * @return
     */
    public boolean isNeedValidate() {
        if (Validate.class.isAssignableFrom(clz)) {
            return true;
        }
        return false;
    }

    /**
     * @param location
     */
    public void resolveLocation(String location) {
        this.resolverLocation = location;
    }

    // Getter and Setter ...

    public Class<?> getClz() {
        return clz;
    }


    public String getLocation() {
        if (resolverLocation != null) {
            return resolverLocation;
        }
        return location;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
