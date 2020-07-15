package com.xa.shushu.upload.datasource.service.push.utils;

import com.alibaba.fastjson.JSON;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;


public class HttpRequestUtil {
    private static CloseableHttpClient httpClient = null;

    static {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setDefaultMaxPerRoute(80);
        cm.setMaxTotal(100);
        RequestConfig globalConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                .setConnectTimeout(10000)
                .setConnectionRequestTimeout(30000)
                .setSocketTimeout(10000).build();
        httpClient = HttpClients.custom()
                .disableAutomaticRetries()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(globalConfig)
                .evictExpiredConnections()
                .build();
    }

    public static CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public static String post(String url, Map<String, String> param, Charset charset) throws IOException {
        CloseableHttpClient client = getHttpClient();
        String stringParam = JSON.toJSONString(param);
        HttpPost post = new HttpPost(url);
        StringEntity entity = new StringEntity(stringParam, "UTF-8");
        post.setEntity(entity);
        post.setHeader("Content-Type", "application/json;charset=utf8");
        try {
            CloseableHttpResponse execute = client.execute(post);
            String response = EntityUtils.toString(execute.getEntity(), charset);
            EntityUtils.consume(execute.getEntity());
            return response;
        } finally {
            post.releaseConnection();
        }
    }

}
