# 虚拟线程与 Structured Concurrency

## 虚拟线程（Virtual Thread，JDK 21 正式）

- 一句话：JVM 在用户态实现的"轻量线程"，由 JVM 调度到少量平台线程上
- 创建成本极低：内存约几百字节 vs 平台线程约 1MB 栈
- 阻塞即让出：IO 阻塞时虚拟线程自动脱离平台线程，不占用它

## 怎么用

Thread.startVirtualThread(() -&gt; doIoTask());
// 或
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -&gt; doIoTask());
}   // try-with-resources 自动等待全部任务完成

## 适用与不适用的场景

适合：IO 密集型（HTTP 调用、DB 查询、文件读写）
不适合：
- CPU 密集（计算密集场景调度开销反而可能变差）
- 池化资源（DB 连接池就 20 个，开 10000 虚拟线程也得排队）

## synchronized 的坑

虚拟线程在 synchronized 块内阻塞时不能优雅让出平台线程
（JDK 24 前）。IO 密集代码建议用 ReentrantLock 替代 synchronized，
否则虚拟线程的收益打折。

## StructuredTaskScope（JDK 21 预览，结构化并发）

- 思想：任务的父子关系显式化，子任务生命周期不超父作用域
- 代码骨架：

try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Subtask&lt;String&gt; user  = scope.fork(() -&gt; queryUser());
    Subtask&lt;String&gt; order = scope.fork(() -&gt; queryOrder());
    scope.join();            // 等所有子任务
    scope.throwIfFailed();   // 任一失败整体抛异常
    return user.get() + order.get();
}   // 出作用域：未完成的子任务自动取消

对比"裸开线程+手动管理"：泄漏、忘记取消、异常吞掉三大问题被结构消除。

## 对后端架构的影响

- Tomcat / Jetty 已支持虚拟线程模式：吞吐量"免费"提升
- 但 DB 连接池、下游限流仍是瓶颈——并发模型解决了线程成本，
  解决不了资源成本

## 自测

1. 虚拟线程为什么适合 IO 密集不适合 CPU 密集？
2. StructuredTaskScope 解决了裸用线程池的哪三个问题？
3. 你的项目里哪个接口最适合先试虚拟线程？说出理由。