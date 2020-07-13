package com.xa.shushu.upload.datasource.service.log;

/**
 * 运行错误上报
 */
public interface ErrorUploader {
    void uploadError(String content);
}
