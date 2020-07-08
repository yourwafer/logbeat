package com.xa.shushu.upload.datasource.resource.anno;

import java.lang.annotation.*;

/**
 * 标识符属性/方法注释
 * @author frank
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Id {
}
