package com.xa.shushu.upload.datasource.service.mysql;

import java.sql.Connection;

public interface ConncectionBuilder {
    Connection connection(int operator, int server);
}
