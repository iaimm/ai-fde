import java.util.concurrent.CompletableFuture;

public class AllOfDemo {
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

        CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> { sleep(); return "[用户]"; });
        CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> { sleep(); return "[订单]"; });
        CompletableFuture<String> f3 = CompletableFuture.supplyAsync(() -> { sleep(); return "[券]"; });

        CompletableFuture<Void> all = CompletableFuture.allOf(f1, f2, f3);
        all.join();   // 等全部完成——但 all 本身是 Void，什么都取不出来！

        // 正确姿势：回原 CF 逐个 join（此刻它们都已完成，join 不耗时）
        String result = f1.join() + f2.join() + f3.join();
        System.out.println("聚合结果: " + result);
    }

    static String queryOrder() { sleep(); return "订单A123"; }
    static CompletableFuture<String> queryTracking(String order) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(); return order + " 的物流: 已发出";
        });
    }
    static void sleep() { try { Thread.sleep(200); } catch (InterruptedException e) {} }
}