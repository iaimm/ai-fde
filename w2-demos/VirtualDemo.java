import java.util.concurrent.Executors;

public class VirtualDemo {
    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 1000; i++) {
                executor.submit(() -> {
                    try { Thread.sleep(10); } catch (InterruptedException e) {}
                });
            }
        } // try-with-resources 会自动等所有任务完成，然后关闭
        System.out.println("1000个虚拟线程任务完成, 耗时: "
                + (System.currentTimeMillis() - start) + "ms");
    }
}