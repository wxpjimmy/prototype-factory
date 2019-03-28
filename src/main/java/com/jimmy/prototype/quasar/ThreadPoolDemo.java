package com.jimmy.prototype.quasar;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableCallable;
import com.jimmy.prototype.http.async.KeepAliveFiberHttpAsyncClient;
import com.jimmy.prototype.http.async.KeepAliveHttpAsyncClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpPost;

import java.util.concurrent.*;

@Slf4j
public class ThreadPoolDemo {
    ExecutorService executorService = Executors.newFixedThreadPool(100);
    private String host = "http://localhost:8080/cost/short";

    public void runWithThreadPool(int taskNum) throws InterruptedException {
        long start = System.currentTimeMillis();
        CountDownLatch countDownLatch = new CountDownLatch(taskNum);
        for (int i=0;i<taskNum;i++) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                   runOnce(host, countDownLatch);
                }
            });
        }
        countDownLatch.await();
        log.info("ThreadPool mode total cost: {}", (System.currentTimeMillis() - start));
    }

    public void runWithThread(int taskNum) throws InterruptedException {
        long start = System.currentTimeMillis();
        CountDownLatch countDownLatch = new CountDownLatch(taskNum);
        for (int i=0;i<taskNum;i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                   runOnce(host, countDownLatch);
                }
            }).start();
        }
        countDownLatch.await();
        log.info("Thread mode total cost: {}", (System.currentTimeMillis() - start));
    }


    public static void runOnce(String host, CountDownLatch countDownLatch) {
        HttpPost post = KeepAliveHttpAsyncClient.buildHttpPost(host, "test");
        Future<String> future = KeepAliveHttpAsyncClient.getInstance().execute("ttt", host, post);
        try {
            String result = future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } finally {
            countDownLatch.countDown();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ThreadPoolDemo target = new ThreadPoolDemo();
        target.runWithThreadPool(10000);
        target.runWithThreadPool(10000);
        target.runWithThreadPool(10000);
        target.runWithThreadPool(10000);

      //  target.runWithThread(1000);
    }

}
