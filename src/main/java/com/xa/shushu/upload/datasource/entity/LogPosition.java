package com.xa.shushu.upload.datasource.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document
@Data
public class LogPosition {

    @Id
    private String id;

    // 上次处理时间
    private Date lastExecute;

    // 上次读取位置
    private int lastLine;

}
