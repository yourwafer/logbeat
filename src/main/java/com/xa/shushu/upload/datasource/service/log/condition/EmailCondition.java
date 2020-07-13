package com.xa.shushu.upload.datasource.service.log.condition;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class EmailCondition implements Condition {
    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        String property = conditionContext.getEnvironment().getProperty("xa.config.errorUploader.type");
        if (StringUtils.equals("email", property.toLowerCase())) {
            return true;
        }
        return false;
    }
}
