package com.myrpc.leafe.Handlers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CompletableFuturedemo {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        //CompletableFuture用来处理异步任务的结果
        //可以获取异步任务的结果或者变量并在主线程中阻塞等待其完成
        CompletableFuture<Integer> completableFuture = new CompletableFuture<>();
        new Thread(()->{
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int i=9;
            completableFuture.complete(i);
        }).start();
        System.out.println(completableFuture.get());
    }
}
