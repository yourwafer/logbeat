package com.xa.shushu.upload.datasource.resource.anno;

import java.lang.annotation.*;

/**
 * 资源数据对象声明注释
 * @author frank
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Resource {
	
	/** 资源位置 */
	String[] value() default "";
}
