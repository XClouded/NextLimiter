package com.github.yoojia.limiter;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Yoojia Chen (yoojiachen@gmail.com)
 * @since 1.0
 */
public class NextLimiter {

    private final ReentrantLock mLock = new ReentrantLock();
    private final Set<Object> mKeyMap = new HashSet<>();
    private final DelayQueue<DelayedObject<Object>> mDelayQueue = new DelayQueue<>();
    private final Thread mDaemonThread;

    private final int mDefTimeout;

    public NextLimiter() {
        this(1000);
    }

    public NextLimiter(int defTimeout) {
        mDefTimeout = defTimeout;
        mDaemonThread = new Thread(new Runnable() {
            @Override public void run() {
                while (true) {
                    try{
                        DelayedObject<Object> task = mDelayQueue.take();
                        if(task != null) {
                            final Object key = task.getData();
                            final ReentrantLock removeLock = mLock;
                            removeLock.lock();
                            try{
                                mKeyMap.remove(key);
                            }finally {
                                removeLock.unlock();
                            }
                        }
                    }catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });
        mDaemonThread.setDaemon(true);
        mDaemonThread.setName("NextLimiter-daemon");
        mDaemonThread.start();
    }

    public Thread getDaemonThread() {
        return mDaemonThread;
    }

    public void apply(Object key, Runnable work, int timeoutMS) {
        finallyWork(key, work, timeoutMS);
    }

    public void apply(Object key, Runnable work) {
        finallyWork(key, work, mDefTimeout);
    }

    public void apply(Object key, DelayedRunnable work, int timeoutMS) {
        finallyWork(key, work, timeoutMS);
    }

    public void apply(Object key, DelayedRunnable work) {
        finallyWork(key, work, mDefTimeout);
    }

    private void finallyWork(Object key, Runnable work, int timeoutMS) {
        boolean allow;
        final ReentrantLock checkAndAddLock = mLock;
        checkAndAddLock.lock();
        try{
            allow = !mKeyMap.contains(key);
            if(allow) {
                mKeyMap.add(key);
            }
        }finally {
            checkAndAddLock.unlock();
        }
        if(allow) {
            mDelayQueue.put(new DelayedObject<>(key, TimeUnit.NANOSECONDS.convert(timeoutMS, TimeUnit.MILLISECONDS)));
            work.run();
        }else if(work instanceof DelayedRunnable) {
            ((DelayedRunnable)work).delayed();
        }
    }

}
