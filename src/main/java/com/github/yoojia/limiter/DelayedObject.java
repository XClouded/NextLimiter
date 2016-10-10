package com.github.yoojia.limiter;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class DelayedObject<T> implements Delayed {

    private static final long START_TIME = System.nanoTime();

    private static final AtomicLong ID_POOL = new AtomicLong(0);

    private final long mId;
    private final long mDeadTime;
    private final T mData;

    public DelayedObject(T data, long timeout) {
        this.mId = ID_POOL.getAndIncrement();
        this.mDeadTime = now() + timeout;
        this.mData = data;
    }

    public T getData() {
        return this.mData;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(mDeadTime - now(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        if (other == this)
            return 0;
        if (other instanceof DelayedObject) {
            DelayedObject x = (DelayedObject) other;
            long diff = mDeadTime - x.mDeadTime;
            if (diff < 0)
                return -1;
            else if (diff > 0)
                return 1;
            else if (mId < x.mId)
                return -1;
            else
                return 1;
        }
        long d = (getDelay(TimeUnit.NANOSECONDS) - other.getDelay(TimeUnit.NANOSECONDS));
        return (d == 0) ? 0 : ((d < 0) ? -1 : 1);
    }

    private static long now() {
        return System.nanoTime() - START_TIME;
    }

}