package com.jimmy.prototype.quasar;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableCallable;
import com.jimmy.prototype.http.async.KeepAliveFiberHttpAsyncClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpPost;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class QuasarDemo {
    private String host = "http://localhost:8080/cost/short";
    KeepAliveFiberHttpAsyncClient client = KeepAliveFiberHttpAsyncClient.getInstance();

    public void runWithFiber(int taskNum) throws InterruptedException {
        long start = System.currentTimeMillis();
        List<Fiber<String>> fibers = new ArrayList<Fiber<String>>();
        for (int i=0;i<taskNum;i++) {
            fibers.add(
            new Fiber<String>(new SuspendableCallable<String>() {
                @Override
                public String run() throws SuspendExecution, InterruptedException {
                    return runOnce();
                }
            }).start());
        }
        for(Fiber<String> fb : fibers) {
            try {
                fb.get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Fiber mode total cost: " +  (System.currentTimeMillis() - start));
    }


    @Suspendable
    private String runOnce()  throws SuspendExecution {
        HttpPost post = client.buildHttpPost(host, "test");
        Future<String> future = client.execute("ttt", host, post);
        try {
           return future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void main(String[] args) throws InterruptedException {
        QuasarDemo demo = new QuasarDemo();
        demo.runWithFiber(10);
    }
}
