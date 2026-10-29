import java.util.concurrent.*;

public class PoolDemo {
    public static void main(String[] args) {
        // 版本A：不传线程池 → 默认走 ForkJoinPool.commonPool
        CompletableFuture.supplyAsync(() -> {
                    System.out.println("默认池线程: " + Thread.currentThread().getName());
                    return "done";
                })
                .join();

        // 版本B：传自定义线程池
        ExecutorService pool = Executors.newFixedThreadPool(4);
        CompletableFuture.supplyAsync(() -> {
                    System.out.println("自定义池线程: " + Thread.currentThread().getName());
                    return "done";
                }, pool)                                    // ← 第二个参数就是池
                .join();
        pool.shutdown();                                    // 用完记得关，否则进程不退出
    }
}