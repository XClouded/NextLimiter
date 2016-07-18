package com.github.yoojia.limiter;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class DelayedObject<T> implements Delayed {

    private static final long START_TIME = System.nanoTime();

    private static final AtomicLong ID_POOL = new AtomicLong(0);

    private final long id;
    private final long deadTime;

    public final T data;

    public DelayedObject(T data, long timeout) {
        this.id = ID_POOL.getAndIncrement();
        this.deadTime = now() + timeout;
        this.data = data;
    }

    public T getData() {
        return this.data;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(deadTime - now(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        if (other == this)
            return 0;
        if (other instanceof DelayedObject) {
            DelayedObject x = (DelayedObject) other;
            long diff = deadTime - x.deadTime;
            if (diff < 0)
                return -1;
            else if (diff > 0)
                return 1;
            else if (id < x.id)
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