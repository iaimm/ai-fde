import java.util.concurrent.CompletableFuture;

public class CfDemoRefactored {
    public static void main(String[] args) {
        // 链1：串行 创建→转换→消费
        CompletableFuture.supplyAsync(() -> query("张三"))
                .thenApply(user -> "你好," + user)
                .thenAccept(greeting -> System.out.println("链1: " + greeting))
                .join();

        // 链2：并行两个任务后合并
        CompletableFuture<String> order  = CompletableFuture.supplyAsync(() -> query("A123"));
        CompletableFuture<String> coupon = CompletableFuture.supplyAsync(() -> query("9折券"));
        order.thenCombine(coupon, (o, c) -> "订单=" + o + ", 券=" + c)
                .thenAccept(result -> System.out.println("链2: " + result))
                .join();

        // 链3：异常兜底
        CompletableFuture.<String>supplyAsync(() -> { throw new RuntimeException("下游挂了"); })
                .exceptionally(ex -> "兜底值")
                .thenAccept(v -> System.out.println("链3: " + v))
                .join();
    }

    static String query(String data) { sleep(); return data; }
    static void sleep() { try { Thread.sleep(300); } catch (InterruptedException e) {} }
}