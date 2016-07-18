package com.github.yoojia.limiter;

/**
 * @author Yoojia Chen (yoojiachen@gmail.com)
 * @since 1.0
 */
public interface DelayedRunnable extends Runnable{

    /**
     * 任务尚不能被执行，需要延迟
     */
    void delayed();
}
