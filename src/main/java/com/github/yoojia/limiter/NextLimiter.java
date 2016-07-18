package com.github.yoojia.limiter;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Yoojia Chen (yoojiachen@gmail.com)
 * @since 0.1
 */
public class NextLimiter {

    private final Set<Object> keyMap = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final DelayQueue<DelayedObject<Object>> queue = new DelayQueue<>();
    private final Thread daemonThread;

    public NextLimiter() {
        daemonThread = new Thread(new Runnable() {
            @Override public void run() {
                while (true) {
                    try{
                        DelayedObject<Object> task = queue.take();
                        if(task != null) {
                            final Object key = task.getData();
                            keyMap.remove(key);
                        }
                    }catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });
        daemonThread.setDaemon(true);
        daemonThread.setName("NextLimiter-daemon");
        daemonThread.start();
    }

    public Thread getDaemonThread() {
        return daemonThread;
    }

    public void apply(Object key, Runnable work, int limitedMillis) {
        finallyWork(key, work, limitedMillis);
    }

    public void apply(Object key, DelayedRunnable work, int limitedMillis) {
        finallyWork(key, work, limitedMillis);
    }

    private void finallyWork(Object key, Runnable work, int limitedMillis) {
        boolean allow;
        synchronized (keyMap) {
            allow = !keyMap.contains(key);
            if(allow) {
                keyMap.add(key);
            }
        }
        if(allow) {
            long deadAfter = TimeUnit.NANOSECONDS.convert(limitedMillis, TimeUnit.MILLISECONDS);
            queue.put(new DelayedObject<>(key, deadAfter));
            work.run();
        }else if(work instanceof DelayedRunnable) {
            ((DelayedRunnable)work).delayed();
        }
    }

}
