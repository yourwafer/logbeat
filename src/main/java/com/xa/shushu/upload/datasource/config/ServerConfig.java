package com.xa.shushu.upload.datasource.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode
@ToString
@AllArgsConstructor
public class ServerConfig {

    // 运营商
    private int operator;

    // 服务器
    private int server;

    // 端口
    private int port;
}
