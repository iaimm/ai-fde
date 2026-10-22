import java.util.concurrent.CompletableFuture;

public class CfDemo {
    public static void main(String[] args) throws Exception {
        // 链 1：创建 → 转换 → 消费
        CompletableFuture.supplyAsync(() -> queryUser())          // 查用户
                .thenApply(user -> "你好," + user)                // 转换
                .thenAccept(greeting -> System.out.println("链1: " + greeting))
                .join();                                          // 等完成

        // 链 2：并行两个任务，合并结果
        CompletableFuture<String> order = CompletableFuture.supplyAsync(() -> queryOrder());
        CompletableFuture<String> coupon = CompletableFuture.supplyAsync(() -> queryCoupon());
        String result = order.thenCombine(coupon, (o, c) -> "订单=" + o + ", 券=" + c)
                .join();
        System.out.println("链2: " + result);

        // 链 3：异常兜底
        CompletableFuture.supplyAsync(() -> { if (true) throw new RuntimeException("下游挂了"); return "ok"; })
                .exceptionally(ex -> "兜底值")
                .thenAccept(v -> System.out.println("链3: " + v))
                .join();
    }
    static String queryUser()   { sleep(); return "张三"; }
    static String queryOrder()  { sleep(); return "A123"; }
    static String queryCoupon() { sleep(); return "9折券"; }
    static void sleep() { try { Thread.sleep(300); } catch (InterruptedException e) {} }
}