import java.util.concurrent.CompletableFuture;

public class SyncDemo {
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        String user = query("用户", 400);    // 各 sleep 400ms
        String order = query("订单", 400);
        String coupon = query("券", 400);
        System.out.println("结果: " + user + order + coupon);
        System.out.println("总耗时: " + (System.currentTimeMillis() - start) + "ms");  // 预期 ~1200ms

        start = System.currentTimeMillis();
        CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> query("用户", 400));
        CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> query("订单", 400));
        CompletableFuture<String> f3 = CompletableFuture.supplyAsync(() -> query("券", 400));
        String result = f1.thenCombine(f2, (a, b) -> a + b)
                        .thenCombine(f3, (ab, c) -> ab + c)
                        .join();
        System.out.println("结果: " + result);
        System.out.println("总耗时: " + (System.currentTimeMillis() - start) + "ms");  // 预期 ~400ms
    }
    static String query(String name, int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
        return "[" + name + "]";
    }
}