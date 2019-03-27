package com.jimmy.prototype.quasar;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolDemo {
    ExecutorService executorService = Executors.newFixedThreadPool(100);

    public void runWithThreadPool(int taskNum) {
        CountDownLatch countDownLatch = new CountDownLatch(taskNum);
        for (int i=0;i<taskNum;i++) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    
                }
            })
        }
    }
}
