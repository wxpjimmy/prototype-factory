package com.jimmy.prototype.http.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class CombineTask extends RecursiveTask<List<String>> {

    private LongRunningRegistry registry;
    private int startId;

    public CombineTask(LongRunningRegistry registry, int startId) {
        this.registry = registry;
        this.startId = startId;
    }

    @Override
    protected List<String> compute() {
        List<TestTask> tasks = new ArrayList<>();
        for(int i=0;i<10;i++) {
            TestTask testTask = new TestTask("http://localhost:8080/cost/long", true, startId + i, registry);
            testTask.fork();
            tasks.add(testTask);
        }

        for(int i=0;i<10;i++) {
            TestTask testTask = new TestTask("http://localhost:8080/cost/short", startId+i+15, registry);
            testTask.fork();
            tasks.add(testTask);
        }
        System.out.println("[" + System.currentTimeMillis() + "] submit finished!");


        List<String> result = new ArrayList<>();
        for(TestTask testTask: tasks) {
            try {
                String content = testTask.get();
                result.add(content);
            }catch (CancellationException ex) {
                System.out.println(String.format("[" + System.currentTimeMillis() + "] ID [%d] request cancelled!", testTask.getId()));
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println(String.format("[" + System.currentTimeMillis() + "] ID [%d] request timeout!", testTask.getId()));
                testTask.cancel();
            }
        }

        return result;
    }
}
