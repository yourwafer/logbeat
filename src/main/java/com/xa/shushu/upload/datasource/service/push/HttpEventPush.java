package com.xa.shushu.upload.datasource.service.push;

import com.xa.shushu.upload.datasource.config.EventConfig;
import com.xa.shushu.upload.datasource.service.report.ReportUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
public class HttpEventPush implements EventPush {

    @Value("${xa.config.eventPush.retryTimes:3}")
    private Integer retryTimes = 3;
    @Value("${xa.config.eventPush.serverUrl:}")
    private String serverUrl;
    @Value("${xa.config.eventPush.appId:}")
    private String appId;
    @Value("${xa.config.eventPush.debug:false}")
    private boolean debug;

    //上传服务
    private HttpService httpService;

    @PostConstruct
    void init() throws URISyntaxException {
        if (StringUtils.isEmpty(serverUrl) || StringUtils.isEmpty(appId)) {
            throw new IllegalArgumentException("参数ServerUrl或appId未配置");
        }

        URI uri = new URI(serverUrl);
        URI url = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(),
                "/sync_server", uri.getQuery(), uri.getFragment());
        if (debug) {
            this.httpService = new HttpService(url, appId, true, false);
        } else {
            this.httpService = new HttpService(url, appId);
        }
    }

    @Override
    public void push(EventConfig eventConfig, String data) {
        try {
            long start = System.currentTimeMillis();
            httpService.send(data);
            ReportUtils.http(data.length(), (System.currentTimeMillis() - start));
        } catch (HttpService.ServiceUnavailableException e) {
            for (int i = 0; i < retryTimes; i++) {
                try {
                    httpService.send(data);
                } catch (HttpService.ServiceUnavailableException | IOException ex) {
                    continue;
                }
                return;
            }

            throw new RuntimeException("数据上传异常", e);
        } catch (IOException e) {
            log.error("数据上传异常", e);
            throw new RuntimeException("数据上传异常", e);
        }
    }


}
