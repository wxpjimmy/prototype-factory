package com.jimmy.prototype.quasar;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableCallable;
import com.jimmy.prototype.http.async.KeepAliveFiberHttpAsyncClient;
import org.apache.http.HttpResponse;

import java.util.ArrayList;
import java.util.List;
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
                    Future<HttpResponse> res = runOnce();
                    try {
                        HttpResponse response =  res.get();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                    return "";
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
    private Future<HttpResponse> runOnce() throws SuspendExecution, InterruptedException {
        return client.execute(host, "test");
    }

    public static void main(String[] args) throws InterruptedException {
        QuasarDemo demo = new QuasarDemo();
        demo.runWithFiber(100);
//        demo.runWithFiber(100000);
//        demo.runWithFiber(100000);
//        demo.runWithFiber(100000);
    }
}
