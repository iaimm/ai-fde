# volatile 与 Java 内存模型

## JMM 核心概念

- 每个线程有自己的工作内存（CPU 缓存抽象），共享变量在主内存
- 线程读：从主内存拷贝到工作内存；写：刷回主内存
- 没有同步时，一个线程的修改对另一个线程不可见

## volatile 保证两件事

1. 可见性：写 volatile 立即刷主内存，读 volatile 立即使工作内存副本失效
2. 有序性：禁止指令重排序（内存屏障），经典应用是单例 DCL

## volatile 不保证原子性

i++ 是三步：读 → 加 → 写。两个线程同时读 0，
都加 1 都写回 1，丢了一次自增。
计数场景用 AtomicInteger 或加锁。

## happens-before（判断可见性的规则）

- 程序次序规则：同线程内前面的操作 happens-before 后面
- 锁规则：解锁 happens-before 后续对同一锁的加锁
- volatile 规则：写 volatile happens-before 后续读该变量
- 传递性：A hb B，B hb C ⇒ A hb C

## DCL 单例（volatile 为什么必须）

public class Singleton {
    private static volatile Singleton instance;  // 不能省
    public static Singleton get() {
        if (instance == null) {                  // 第一次检查
            synchronized (Singleton.class) {
                if (instance == null)            // 第二次检查
                    instance = new Singleton();
            }
        }
        return instance;
    }
}
没有 volatile，"new"的三步（分配内存、初始化、赋值引用）
可能被重排序，其他线程拿到未初始化完成的引用。

## 自测

1. volatile 能替代 synchronized 吗？什么场景能、什么场景不能？
2. 为什么 DCL 单例的 instance 必须加 volatile？
3. happens-before 的传递性怎么用于推导可见性？