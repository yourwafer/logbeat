package com.xa.shushu.upload.datasource.config;

import lombok.Data;

@Data
public class DatabaseConfig {
    // 数据库名称前缀
    private String namePrefix;

    // 账号
    private String userName;

    // 密码
    private String password;

}
