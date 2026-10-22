import java.util.concurrent.locks.ReentrantLock;

public class FairDemo {
    public static void main(String[] args) throws InterruptedException {
        test(new ReentrantLock(true));   // 传 false 对比
    }
    static void test(ReentrantLock lock) throws InterruptedException {
        System.out.println((lock.isFair() ? "公平" : "非公平") + "锁获取顺序:");
        for (int i = 0; i < 1000; i++) {
            int id = i;
            Thread t = new Thread(() -> {
                lock.lock();
                System.out.println("线程" + id + " 获取到锁");
                try { Thread.sleep(10); } catch (InterruptedException e) {}
                lock.unlock();
            });
            t.start();
            Thread.sleep(5);  // 保证按顺序启动
        }
        Thread.sleep(500);
    }
}