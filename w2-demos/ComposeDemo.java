import java.util.concurrent.CompletableFuture;

public class ComposeDemo {
    public static void main(String[] args) {
        // 版本A：thenApply —— 回调里返回的还是 CompletableFuture，结果嵌套
        CompletableFuture<CompletableFuture<String>> nested =
                CompletableFuture.supplyAsync(() -> queryOrder())
                        .thenApply(order -> queryTracking(order));
        System.out.println("thenApply 结果类型: " + nested.getClass().getSimpleName());
        System.out.println("取值要 join 两次: " + nested.join().join());

        // 版本B：thenCompose —— 自动拍平，只 join 一次
        CompletableFuture<String> flat =
                CompletableFuture.supplyAsync(() -> queryOrder())
                        .thenCompose(order -> queryTracking(order));
        System.out.println("thenCompose 结果类型: " + flat.getClass().getSimpleName());
        System.out.println("取值只 join 一次: " + flat.join());
    }

    static String queryOrder() { sleep(); return "订单A123"; }
    static CompletableFuture<String> queryTracking(String order) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(); return order + " 的物流: 已发出";
        });
    }
    static void sleep() { try { Thread.sleep(200); } catch (InterruptedException e) {} }
}