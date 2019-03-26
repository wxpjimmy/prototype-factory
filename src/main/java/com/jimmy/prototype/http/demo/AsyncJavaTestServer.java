package com.jimmy.prototype.http.demo;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class AsyncJavaTestServer {
    public static void main(String[] args) throws InterruptedException {
        ForkJoinPool pool = new ForkJoinPool(50);
        runOnce(pool, 0, false);
        runOnce(pool, 100, true);
        //runOnce(pool, 200, false);
        pool.shutdownNow();
        pool.awaitTermination(1000, TimeUnit.MILLISECONDS);
    }

    static void runOnce(ForkJoinPool pool, int startId, boolean cancel) throws InterruptedException {
        System.out.println("=======================================start run===========================================");
        LongRunningRegistry registry = new LongRunningRegistry();
        CombineTask task = new CombineTask(registry, startId);
        long start = System.currentTimeMillis();
        pool.submit(task);

        if(cancel) {
            registry.latch.await();
            registry.cancelAllLongRunningTask();
        }

        List<String> res = null;
        try {
            res = task.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        for(String re : res) {
            System.out.println("[" + System.currentTimeMillis() + "] Result: " + re);
        }
        System.out.println("[" + System.currentTimeMillis() + "] Total cost: " + (System.currentTimeMillis() - start));
    }
}
