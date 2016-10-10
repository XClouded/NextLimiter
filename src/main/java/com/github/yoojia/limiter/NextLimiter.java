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
    private final Set<Object> mJobKeys = new HashSet<Object>();
    private final DelayQueue<DelayedObject<Object>> mDelayQueue = new DelayQueue<DelayedObject<Object>>();
    private final Thread mDaemonThread;

    private final int mDefTimeout;

    public NextLimiter() {
        this(1000);
    }

    public NextLimiter(int defTimeout) {
        mDefTimeout = defTimeout;
        mDaemonThread = new Thread(new Runnable() {

            @Override public void run() {
                while (!Thread.currentThread().isInterrupted()) try{
                    work();
                }catch (InterruptedException e) {
                    break;
                }
            }

            private void work() throws InterruptedException {
                final DelayedObject<Object> task = mDelayQueue.take();
                if(task != null) {
                    final Object key = task.getData();
                    unlock(key);
                }
            }
        });
        mDaemonThread.setDaemon(true);
        mDaemonThread.setName("NextLimiter-DaemonThread");
        mDaemonThread.start();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        try{
            mDaemonThread.interrupt();
        }catch (Exception e){ /*nop*/ }
    }

    public Thread getDaemonThread() {
        return mDaemonThread;
    }

    /**
     * 提交限制任务，并指定限制超时时间。
     * @param key 任务Key
     * @param work 具体任务
     * @param timeoutMS 限制超时时间，单位：毫秒
     */
    public void apply(Object key, Runnable work, int timeoutMS) {
        applyJob(key, work, timeoutMS);
    }

    /**
     * 提交限制任务，并使用默认限制超时时间。
     * @param key 任务Key
     * @param work 具体任务
     */
    public void apply(Object key, Runnable work) {
        applyJob(key, work, mDefTimeout);
    }

    /**
     * 提交限制任务，并指定限制超时时间。
     * @param key 任务Key
     * @param work 具体任务
     * @param timeoutMS 限制超时时间，单位：毫秒
     */
    public void apply(Object key, DelayedRunnable work, int timeoutMS) {
        applyJob(key, work, timeoutMS);
    }

    /**
     * 提交限制任务，并使用默认限制超时时间。
     * @param key 任务Key
     * @param work 具体任务
     */
    public void apply(Object key, DelayedRunnable work) {
        applyJob(key, work, mDefTimeout);
    }

    private void applyJob(final Object key, final Runnable worker, final int timeout){
        invoke(key, worker, new JobHandler(key, timeout, mDelayQueue));
    }

    /**
     * 手动限制任务多次执行，直到手动调用{@see unlock(Object) }来解除限制。
     * @param key 任务Key
     * @param work 具体任务
     */
    public void lock(Object key, Runnable work){
        invoke(key, work, JobHandler.NOP);
    }

    /**
     * 手动限制任务多次执行，直到手动调用{@see unlock(Object) }来解除限制。
     * @param key 任务Key
     * @param work 具体任务
     */
    public void lock(Object key, DelayedRunnable work){
        invoke(key, work, JobHandler.NOP);
    }

    /**
     * 解除指定任务Key的限制
     * @param key 任务Key
     */
    public void unlock(Object key){
        final ReentrantLock lock = mLock;
        lock.lock();
        try{
            mJobKeys.remove(key);
        }finally {
            lock.unlock();
        }
    }

    private void invoke(Object key, Runnable worker, JobHandler jobHandler) {
        final ReentrantLock lock = mLock;
        lock.lock();
        final boolean allow;
        try{
            allow = !mJobKeys.contains(key);
            if(allow) {
                mJobKeys.add(key);
            }
        }finally {
            lock.unlock();
        }
        if(allow) {
            jobHandler.run();
            worker.run();
        }else if(worker instanceof DelayedRunnable) {
            ((DelayedRunnable)worker).onDelayed();
        }
    }

    private static class JobHandler implements Runnable{

        private static JobHandler NOP = new JobHandler(0, 0, null){
            @Override public void run() {}
        };

        private final Object mKey;
        private final long mTimeout;
        private final DelayQueue<DelayedObject<Object>> mDelayQueue;

        private JobHandler(Object key, long timeout, DelayQueue<DelayedObject<Object>> delayQueue) {
            this.mKey = key;
            this.mTimeout = timeout;
            mDelayQueue = delayQueue;
        }

        @Override
        public void run() {
            mDelayQueue.put(new DelayedObject<Object>(mKey, TimeUnit.NANOSECONDS.convert(mTimeout, TimeUnit.MILLISECONDS)));
        }

    }

}
