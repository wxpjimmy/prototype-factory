package com.jimmy.prototype.http.demo;

import com.jimmy.prototype.http.async.KeepAliveHttpAsyncClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpPost;

import java.util.concurrent.*;

@Slf4j
public class TestTask extends RecursiveTask<String> {
    private String host;
    private boolean needRegister;
    private int id;
    private HttpPost post;
    private LongRunningRegistry registry;

    public TestTask(String host, int id, LongRunningRegistry registry) {
        this(host, false, id, registry);
    }

    public TestTask(String host, boolean needRegister, int id, LongRunningRegistry registry) {
        this.host = host;
        this.needRegister = needRegister;
        this.id = id;
        this.registry = registry;
    }

    public int getId() {
        return  this.id;
    }

    @Override
    protected String compute() {
        log.info("Start running task: {}",  id);
        post = KeepAliveHttpAsyncClient.buildHttpPost(host, "test");
        if(needRegister && registry != null) {
            registry.registerLongRunningRequest(post);
            log.info("Register finished! {}", id);
        }
        Future<String> stringFuture =  KeepAliveHttpAsyncClient.getInstance().execute("ddd-" + id, host, post);
        String res = "";
        long start = System.currentTimeMillis();
        try {
           res = stringFuture.get();
        } catch (Exception e) {
          //  e.printStackTrace();
            log.error("[{}] Request failed! total cost: {}", id, (System.currentTimeMillis() - start));
        }
        log.info("[{}] Request result: {}", id, res);
        return res;
    }

    public void cancel() {
        if(post != null) {
            post.abort();
        }
    }
}
