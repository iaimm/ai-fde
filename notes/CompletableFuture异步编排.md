# CompletableFuture 异步编排

## 为什么用它

- Future.get() 阻塞；CompletableFuture（CF）支持回调链式组合
- 避免"回调地狱"：thenCompose 扁平化嵌套
- 多任务并行聚合：allOf / anyOf

## 常用 API（10 个先够用）

创建：supplyAsync(有返回值) / runAsync(无返回值)
串行：thenApply(转换) / thenAccept(消费) / thenRun(纯动作)
并行组合：thenCombine(两结果合并) / thenCompose(链式拍平)
聚合：allOf(全部完成) / anyOf(任意一个完成)
异常：exceptionally(兜底) / handle(成功失败都处理)
超时（Java 9+）：orTimeout / completeOnTimeout

## 核心代码模板

// 三个任务并行，结果合并
CompletableFuture&lt;String&gt; a = CompletableFuture.supplyAsync(() -&gt; queryUser());
CompletableFuture&lt;String&gt; b = CompletableFuture.supplyAsync(() -&gt; queryOrder());
CompletableFuture&lt;String&gt; c = CompletableFuture.supplyAsync(() -&gt; queryCoupon());

String result = a.thenCombine(b, (ra, rb) -&gt; ra + rb)
                 .thenCombine(c, (rab, rc) -&gt; rab + rc)
                 .exceptionally(ex -&gt; "fallback")
                 .join();          // join 抛 unchecked 异常，get 抛 checked

// 等全部完成再做汇总
CompletableFuture.allOf(a, b, c).join();

## 线程池坑点

- 不传 Executor 默认用 ForkJoinPool.commonPool，
  全局共享、可能被其他任务占满
- IO 密集任务务必传自己的线程池：
  supplyAsync(() -&gt; query(), myExecutor)

## vs 虚拟线程（JEP 444 之后）

纯 IO 等待型并发可以直接开虚拟线程，代码更直白；
CF 的价值仍在：任务编排、超时控制、结果聚合。
两者不冲突，可以 combine。

## 自测

1. thenCombine 和 thenCompose 的区别是什么？
2. 为什么生产代码不建议用默认 commonPool？
3. allOf 之后怎么拿到每个子任务的结果？（join 各自 CF）