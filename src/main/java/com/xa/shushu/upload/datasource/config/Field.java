package com.xa.shushu.upload.datasource.config;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Field {

    // 数据列下标
    private int index;

    // 字段类型
    private Class<?> type;
}
