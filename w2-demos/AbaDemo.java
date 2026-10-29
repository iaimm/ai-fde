import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

public class AbaDemo {
    public static void main(String[] args) throws Exception {
        // 场景：t1 读到 A，想改成 X；但它读和改之间，值被偷偷换了两次又换回来
        AtomicReference<String> ref = new AtomicReference<>("A");

        Thread t1 = new Thread(() -> {
            String expect = ref.get();          // 读到 "A"
            sleep(200);                          // 给 t2 留作案时间
            boolean ok = ref.compareAndSet(expect, "X");
            System.out.println("朴素CAS(A->X): " + ok);   // true —— 完全没察觉中间发生过 A->B->A
        });

        Thread t2 = new Thread(() -> {
            ref.set("B"); sleep(50); ref.set("A");
            System.out.println("t2 完成 A->B->A");
        });

        t2.start(); t1.start(); t1.join();

        // 对照组：带版本号，同样的作案手法会被识破
        AtomicStampedReference<String> stamped = new AtomicStampedReference<>("A", 1);
        int oldStamp = stamped.getStamp();
        stamped.compareAndSet("A", "B", 1, 2);
        stamped.compareAndSet("B", "A", 2, 3);            // 值回来了，但版本已经到 3
        boolean ok = stamped.compareAndSet("A", "X", oldStamp, oldStamp + 1);
        System.out.println("带版本CAS(用旧的版本号1): " + ok);  // false —— 识破
    }

    static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) {} }
}