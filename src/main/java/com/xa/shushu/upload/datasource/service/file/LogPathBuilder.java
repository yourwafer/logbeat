package com.xa.shushu.upload.datasource.service.file;

import java.time.LocalDate;

public interface LogPathBuilder {
    String buildFilePath(String log, String type, int operator, int server, LocalDate time);
}
