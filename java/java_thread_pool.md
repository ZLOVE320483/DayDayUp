## Java线程池线程复用原理

### 什么是线程池复用

在线程池中，通过同一个线程去执行不同的任务，这就是线程复用。

假设现在有 100 个任务，我们创建一个固定线程的线程池（FixedThreadPool），核心线程数和最大线程数都是 3，那么当这个 100 个任务执行完，都只会使用三个线程。

示例：

```
public class FixedThreadPoolDemo {

    static ExecutorService executorService = Executors.newFixedThreadPool(3);

    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            executorService.execute(() -> {
                System.out.println(Thread.currentThread().getName() + " -> Running...");
            });
        }
    }

}
```

运行结果：
```
pool-1-thread-1 -> Running...
pool-1-thread-3 -> Running...
pool-1-thread-2 -> Running...
pool-1-thread-3 -> Running...
pool-1-thread-1 -> Running...
pool-1-thread-3 -> Running...
pool-1-thread-2 -> Running...
pool-1-thread-3 -> Running...
```

### 线程复用原理
