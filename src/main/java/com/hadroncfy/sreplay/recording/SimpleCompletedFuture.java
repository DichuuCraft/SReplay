package com.hadroncfy.sreplay.recording;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

class SimpleCompletedFuture implements Future<Void> {
    // private final T val;

    public SimpleCompletedFuture(){
        // this.val = val;
    }

    @Override
    public Void get() throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public boolean isCancellable() {
        return false;
    }

    @Override
    public Throwable cause() {
        return null;
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return true;
    }

    @Override
    public boolean await(long timeoutMillis) throws InterruptedException {
        return true;
    }

    @Override
    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        return true;
    }

    @Override
    public boolean awaitUninterruptibly(long timeoutMillis) {
        return true;
    }

    @Override
    public Void getNow() {
        return null;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return true;
    }

    @Override
    public Future<Void> addListener(GenericFutureListener<? extends Future<? super Void>> listener) {
        return this;
    }

    @Override
    public Future<Void> addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners) {
        return this;
    }

    @Override
    public Future<Void> removeListener(GenericFutureListener<? extends Future<? super Void>> listener) {
        return this;
    }

    @Override
    public Future<Void> removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners) {
        return this;
    }

    @Override
    public Future<Void> sync() throws InterruptedException {
        return this;
    }

    @Override
    public Future<Void> syncUninterruptibly() {
        return this;
    }

    @Override
    public Future<Void> await() throws InterruptedException {
        return this;
    }

    @Override
    public Future<Void> awaitUninterruptibly() {
        return this;
    }

}