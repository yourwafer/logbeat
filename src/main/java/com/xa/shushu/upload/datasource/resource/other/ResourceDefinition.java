package com.xa.shushu.upload.datasource.resource.other;


import com.xa.shushu.upload.datasource.resource.Validate;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.springframework.core.io.Resource;

/**
 * 资源定义信息对象
 *
 * @author frank
 */
public class ResourceDefinition {

    /**
     * 资源类
     */
    private final Class<?> clz;
    /**
     * 资源路径
     */
    private String resolverLocation;
    /**
     * 资源路径
     */
    private final org.springframework.core.io.Resource resource;

    /**
     * 构造方法
     */
    public ResourceDefinition(Class<?> clz, org.springframework.core.io.Resource resource) {
        this.clz = clz;
        this.resource = resource;
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

    public Resource getResource() {
        return resource;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
