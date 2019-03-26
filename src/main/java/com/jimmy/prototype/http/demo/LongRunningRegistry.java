package com.jimmy.prototype.http.demo;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpPost;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class LongRunningRegistry {
    private List<HttpPost> longRunningRequests;

    private static volatile  LongRunningRegistry instance;
    public CountDownLatch latch;

    public LongRunningRegistry() {
        longRunningRequests = new ArrayList<>();
        latch = new CountDownLatch(10);
    }

    public static LongRunningRegistry getInstance(){
        if(instance == null) {
            synchronized (LongRunningRegistry.class) {
                if(instance == null) {
                    instance = new LongRunningRegistry();
                }
            }
        }
        return instance;
    }

    public synchronized void registerLongRunningRequest(HttpPost post) {
        log.info("adding long running task");
        longRunningRequests.add(post);
        latch.countDown();
    }

    public void cancelAllLongRunningTask() {
        log.info("start Aborting running task");
        for(HttpPost post : longRunningRequests) {
            log.info("Abort running task");
            post.abort();
        }
    }
}
