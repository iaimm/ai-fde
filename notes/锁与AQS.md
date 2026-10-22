# 锁与 AQS

## synchronized vs ReentrantLock

| 维度 | synchronized | ReentrantLock |
|---|---|---|
| 释放 | JVM 自动释放 | 必须手动 unlock（配 finally） |
| 等待可中断 | 不可 | lockInterruptibly() 可以 |
| 公平锁 | 不支持 | 支持（构造传 true，吞吐下降） |
| 条件队列 | 一个 wait 集合 | 多个 Condition 精准唤醒 |
| 尝试获取 | 不能 | tryLock() 带超时 |

结论：默认用 synchronized（JVM 优化好、不会忘解锁）；
需要中断、超时、公平、多条件时再上 ReentrantLock。

## CAS（Compare-And-Swap）

- 语义：比较内存值与预期值，相等则替换，整个过程由 CPU 原子指令保证
- 三大问题：ABA（加版本号解决）、自旋开销、只能保证单个变量
- 应用：AtomicInteger、ConcurrentHashMap 的计数、AQS 的 state 更新

## AQS 一句话

AQS = 一个 volatile int state + 一个 FIFO 双端队列（CLH 变体）；
获取锁失败就入队自旋/挂起，释放锁就唤醒后继。
ReentrantLock、CountDownLatch、Semaphore 都是它的子类实现。

## 公平锁为什么慢

公平锁每次都要检查队列里有没有等待者，有就排队，
导致线程频繁挂起唤醒（上下文切换）；非公平锁允许"插队"，
刚释放锁时新线程可能直接获得，减少空转。

## 自测

1. ABA 问题是什么？AtomicStampedReference 怎么解决？
2. AQS 的 state 在 ReentrantLock 里表示什么？（重入次数）
3. 什么时候必须用 ReentrantLock 而不是 synchronized？说出三个场景。