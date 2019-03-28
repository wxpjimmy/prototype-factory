package com.jimmy.prototype.http.async;

import co.paralleluniverse.fibers.httpasyncclient.FiberCloseableHttpAsyncClient;
import com.jimmy.prototype.http.async.consumer.BytesResponseConsumer;
import com.jimmy.prototype.http.async.consumer.ResponseConsumer;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by zengdejun on 2017/11/6.
 */
public class KeepAliveFiberHttpAsyncClient {
    private static Logger logger = LoggerFactory.getLogger(KeepAliveFiberHttpAsyncClient.class);
    private CloseableHttpAsyncClient httpClient;

    static class AsyncPosterHolder {
        private final static KeepAliveFiberHttpAsyncClient INSTANCE = new KeepAliveFiberHttpAsyncClient();
    }

    public static KeepAliveFiberHttpAsyncClient getInstance() {
        return AsyncPosterHolder.INSTANCE;
    }

    private KeepAliveFiberHttpAsyncClient() {
        // SSL context for secure connections can be created either based on
        // system or application specific properties.
        SSLContext sslcontext = SSLContexts.createSystemDefault();
        // Use custom hostname verifier to customize SSL hostname verification.
        HostnameVerifier hostnameVerifier = new DefaultHostnameVerifier();
        // Create a registry of custom connection session strategies for supported
        // protocol schemes.
        Registry<SchemeIOSessionStrategy> sessionStrategyRegistry = RegistryBuilder.<SchemeIOSessionStrategy>create()
                .register("http", NoopIOSessionStrategy.INSTANCE)
                .register("https", new SSLIOSessionStrategy(sslcontext, hostnameVerifier))
                .build();

        // Create I/O reactor configuration
        int workerThreadCount = 12;
        IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
                .setIoThreadCount(workerThreadCount)
                .setConnectTimeout(3000) // 3s
                .setSoTimeout(300)      // 300ms
                .setSelectInterval(300)
                .build();

        // Create a custom I/O reactort
        ConnectingIOReactor ioReactor = null;
        try {
            ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);
        } catch (IOReactorException e) {
            logger.error("Fail to create IoReactor");
        }
        // Create a connection manager with custom configuration.
        PoolingNHttpClientConnectionManager cm = new PoolingNHttpClientConnectionManager(
                ioReactor, sessionStrategyRegistry);
        // Configure total max or per route limits for persistent connections
        // that can be kept in the pool or leased by the connection manager.
        cm.setMaxTotal(6000);
        cm.setDefaultMaxPerRoute(150);
        // close all connections that have been idle over 1 minute.
        cm.closeIdleConnections(60, TimeUnit.SECONDS);
        // keep alive strategy
        ConnectionKeepAliveStrategy keepAliveStrat = new DefaultConnectionKeepAliveStrategy() {
            @Override
            public long getKeepAliveDuration(
                    HttpResponse response,
                    HttpContext context) {
                long keepAlive = super.getKeepAliveDuration(response, context);
                if (keepAlive == -1) {
                    // Keep connections alive 60 seconds if a keep-alive value
                    // has not be explicitly set by the server
                    keepAlive = 60000;
                }
                return keepAlive;
            }
        };
//        MonitorThread.getInstance().setConnectionManager(cm);

     /* setConnectTimeout: Client tries to connect to the server. setConnectTimeout denotes the time elapsed
        before the connection established or Server responded to connection request.

        setSoTimeout: After establishing the connection, the client socket waits for response after sending the request.
        setSoTimeout is the elapsed time since the client has sent request to the server before server responds.
        Please note that this is not not same as HTTP Error 408 which the server sends to the client.
        In other words its maximum period inactivity between two consecutive data packets arriving at client
        side after connection is established.*/
        int connectTimeout = 2000;
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(2000)
                .setConnectionRequestTimeout(connectTimeout)
                .setSocketTimeout(connectTimeout)
                .build();

        // create HttpClient
        httpClient = HttpAsyncClients.custom()
                .setConnectionManager(cm)
                .setKeepAliveStrategy(keepAliveStrat)
                .setDefaultRequestConfig(requestConfig)
                .disableConnectionState()
                .build();
        httpClient = FiberCloseableHttpAsyncClient.wrap(httpClient);
        httpClient.start();
    }


    public static HttpPost buildHttpPost(String host, String requestJsonString) {
        HttpPost httpPost = new HttpPost(host);
        httpPost.setHeader("Content-Type", "application/json; charset=UTF-8");
//        httpPost.setHeader(Constants.MaxRTBKey, Constants.MaxRTBVersion);
        httpPost.setEntity(new StringEntity(requestJsonString, "UTF-8"));
        return httpPost;
    }

    private static HttpPost buildByteHttpPost(String host, String dsp, byte[] requestData) {
        HttpPost httpPost = new HttpPost(host);
        if("baidu".equalsIgnoreCase(dsp)) {
            httpPost.addHeader("Content-Type", "application/x-protobuf; charset=UTF-8");
//            httpPost.setHeader(Constants.MaxRTBKey, Constants.MaxRTBVersion);
            httpPost.addHeader("Accept", "application/x-protobuf; charset=UTF-8");
        }else if("tanx".equalsIgnoreCase(dsp)) {
            httpPost.addHeader("Content-Type", "application/octet-stream");
        }else {
            //so far, default to same as tanx
            httpPost.addHeader("Content-Type", "application/octet-stream");
        }
        ByteArrayInputStream inputStream = new ByteArrayInputStream(requestData);
        InputStreamEntity inputStreamEntity = new InputStreamEntity(inputStream, requestData.length);
        //TODO: for tanx test
        if(host.contains("140.205.241.3")) {
            inputStreamEntity.setChunked(false);
        }
        httpPost.setEntity(inputStreamEntity);
        return httpPost;
    }

    private static HttpGet buildHttpGet(String host, String requestJsonString) {
        String url = host + "" + requestJsonString;
        HttpGet httpGet = new HttpGet(url);
//        httpGet.setHeader(Constants.MaxRTBKey, Constants.MaxRTBVersion);
        return httpGet;
    }

    public Future<String> execute(String dsp, String host, HttpPost httpPost) {
        return httpClient.execute(HttpAsyncMethods.create(httpPost), new ResponseConsumer(dsp, host), new FutureCallback<String>() {
            @Override
            public void completed(String result) {
                //System.out.println("[" + System.currentTimeMillis() + "] Task succeed: " + dsp);
            }

            @Override
            public void failed(Exception ex) {
               // System.out.println("[" + System.currentTimeMillis() + "] Task failed: " + dsp);
            }

            @Override
            public void cancelled() {
                //System.out.println("[" + System.currentTimeMillis() + "] Task cancelled: " + dsp);
            }
        });
    }

    public Future<String> post(String dsp, String host, String requestJsonString) {
        HttpPost httpPost = buildHttpPost(host, requestJsonString);
        return httpClient.execute(HttpAsyncMethods.create(httpPost), new ResponseConsumer(dsp, host), null);
    }


    public String post (String dsp, String host, String requestJsonString, long timeOutNanoSecond) throws Exception {
        Future<String> future = post(dsp, host, requestJsonString);
        return future.get(timeOutNanoSecond, TimeUnit.NANOSECONDS);
    }

    public Future<byte[]> binaryPost(String dsp , String host, byte[] requestBytes) throws  Exception {
        HttpPost httpPost = buildByteHttpPost(host, dsp, requestBytes);
        return httpClient.execute(HttpAsyncMethods.create(httpPost), new BytesResponseConsumer(dsp, host), null);
    }

    public byte[] binaryPost(String dsp, String host, byte[] requestBytes, long timeOutNanoSecond) throws Exception {
        Future<byte[]> future = binaryPost(dsp, host, requestBytes);
        return future.get(timeOutNanoSecond, TimeUnit.NANOSECONDS);
    }

    public Future<String> get(String dsp , String host, String requestJsonString) {
        String url = host + "" + requestJsonString;
        return httpClient.execute(HttpAsyncMethods.createGet(url), new ResponseConsumer(dsp, host), null);
    }

    public String get(String dsp , String host, String requestJsonString, long timeOutNanoSecond) throws Exception {
        Future<String> future = get(dsp, host, requestJsonString);
        return future.get(timeOutNanoSecond, TimeUnit.NANOSECONDS);
    }

    public void close() {
        if(httpClient!=null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
