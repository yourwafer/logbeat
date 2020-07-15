package com.xa.shushu.upload.datasource.service.push;

import com.alibaba.fastjson.JSONObject;
import com.xa.shushu.upload.datasource.service.push.utils.HttpRequestUtil;
import lombok.extern.slf4j.Slf4j;
import net.jpountz.lz4.LZ4BlockOutputStream;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import org.anarres.lzo.LzoAlgorithm;
import org.anarres.lzo.LzoCompressor;
import org.anarres.lzo.LzoLibrary;
import org.anarres.lzo.LzoOutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

@Slf4j
public class HttpService implements Closeable {

    private final URI serverUri;
    private final String appId;
    private Boolean writeData = true;
    private Boolean debug = false;
    private String compress = "gzip";
    private Integer connectTimeout = null;
    private CloseableHttpClient httpClient = null;

    public HttpService(URI server_uri, String appId, Integer timeout) {
        this(server_uri, appId);
        this.connectTimeout = timeout;
    }

    public HttpService(URI server_uri, String appId, boolean debug, boolean writeData) {
        this(server_uri, appId);
        this.writeData = writeData;
    }

    HttpService(URI server_uri, String appId) {
        this.httpClient = HttpRequestUtil.getHttpClient();
        this.serverUri = server_uri;
        this.appId = appId;
    }

    public synchronized void send(final String data) throws IOException {
        HttpPost httpPost = new HttpPost(serverUri);
        HttpEntity params = debug ? getDebugHttpEntity(data) : getBatchHttpEntity(data);
        httpPost.setEntity(params);
        httpPost.addHeader("appid", this.appId);
        httpPost.addHeader("compress", compress);
        if (this.connectTimeout != null) {
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(connectTimeout + 10000).setConnectTimeout(connectTimeout).build();
            httpPost.setConfig(requestConfig);
        }
        try (CloseableHttpResponse response = this.httpClient.execute(httpPost)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode < 200 || statusCode > 300) {
                throw new ServiceUnavailableException("Cannot post message to " + this.serverUri);
            }
            String result = EntityUtils.toString(response.getEntity(), "UTF-8");
            JSONObject resultJson = JSONObject.parseObject(result);
            checkingRetCode(resultJson);
        } catch (IOException e) {
            throw new ServiceUnavailableException("Cannot post message to " + this.serverUri, e);
        } finally {
            httpPost.releaseConnection();
        }
    }

    UrlEncodedFormEntity getDebugHttpEntity(final String data) throws IOException {
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("source", "server"));
        nameValuePairs.add(new BasicNameValuePair("appid", this.appId));
        nameValuePairs.add(new BasicNameValuePair("data", data));
        if (!this.writeData) {
            nameValuePairs.add(new BasicNameValuePair("dryRun", String.valueOf(1)));
        }
        return new UrlEncodedFormEntity(nameValuePairs, "UTF-8");
    }

    HttpEntity getBatchHttpEntity(final String data) throws IOException {
        byte[] dataCompressed = null;
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        if ("gzip".equalsIgnoreCase(this.compress)) {
            dataCompressed = gzipCompress(dataBytes);
        } else if ("lzo".equalsIgnoreCase(this.compress)) {
            dataCompressed = lzoCompress(dataBytes);
        } else if ("lz4".equalsIgnoreCase(this.compress)) {
            dataCompressed = lz4Compress(dataBytes);
        } else if ("none".equalsIgnoreCase(this.compress)) {
            dataCompressed = dataBytes;
        } else {
            throw new IllegalArgumentException("compress input error.");
        }
        return new ByteArrayEntity(dataCompressed);
    }

    private static byte[] lzoCompress(byte[] srcBytes) throws IOException {
        LzoCompressor compressor = LzoLibrary.getInstance().newCompressor(
                LzoAlgorithm.LZO1X, null);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        LzoOutputStream cs = new LzoOutputStream(os, compressor);
        cs.write(srcBytes);
        cs.close();
        return os.toByteArray();
    }

    private static byte[] lz4Compress(byte[] srcBytes) throws IOException {
        LZ4Factory factory = LZ4Factory.fastestInstance();
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        LZ4Compressor compressor = factory.fastCompressor();
        LZ4BlockOutputStream compressedOutput = new LZ4BlockOutputStream(
                byteOutput, 2048, compressor);
        compressedOutput.write(srcBytes);
        compressedOutput.close();
        return byteOutput.toByteArray();
    }

    private static byte[] gzipCompress(byte[] srcBytes) throws IOException {
        GZIPOutputStream gzipOut = null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            gzipOut = new GZIPOutputStream(out);
            gzipOut.write(srcBytes);
            gzipOut.close();
            return out.toByteArray();
        } finally {
            if (gzipOut != null) {
                gzipOut.close();
            }
        }

    }

    private void checkingRetCode(JSONObject resultJson) {
        int retCode = resultJson.getInteger("code");
        if (retCode != 0) {
            if (retCode == -1) {
                throw new IllegalStateException(resultJson.containsKey("msg") ? resultJson.getString("msg") : "invalid data format");
            } else if (retCode == -2) {
                throw new IllegalStateException(resultJson.containsKey("msg") ? resultJson.getString("msg") : "APP ID doesn't exist");
            } else if (retCode == -3) {
                throw new IllegalStateException(resultJson.containsKey("msg") ? resultJson.getString("msg") : "invalid ip transmission");
            } else {
                throw new IllegalStateException("Unexpected response return code: " + retCode);
            }
        }
    }

    static class ServiceUnavailableException extends RuntimeException {
        public ServiceUnavailableException(String message) {
            super(message);
        }

        ServiceUnavailableException(String message, Throwable throwable) {
            super(message, throwable);
        }
    }

    @Override
    public void close() {
        if (this.httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                log.warn("关闭client失败", e);
            }
        }
    }

}