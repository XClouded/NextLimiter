# NextLimiter - 请求频率限制

> 基于Key和DelayQueue实现的请求频率限制器，应用于Android开发。

# 使用

> compile 'com.github.yoojia:next-limiter:1.a.2'

```java

// 设定默认限制频率为1秒
NextLimiter limiter = new NextLimiter(1000);

Runnable loginAction = new Runnable{
   @Override public void run(){
       // 当apply(...)调用，并且允许执行时，此方法被回调。
   }
};
// 请求频率被限制为1秒：在1秒内，无论以下代码被调用多少次，loginAction只会被调用一次。
limiter.apply("login-request-key", loginAction);

// 覆盖默认频率，自定义为3秒限制：
limiter.apply("login-request-key", loginAction, 3 * 1000);
```

## 限制回调

```java
// 如果需要处理被限制请求时的操作，可以使用DelayedRunnable：
DelayedRunnable delayed = new DelayedRunnable() {
  @Override public void onDelayed() {
      // 当apply(...)调用，但被限制时，此方法被回调。
  }

  @Override public void run() {
        // 当apply(...)调用，并且允许执行时，此方法被回调。
  }
};
```
