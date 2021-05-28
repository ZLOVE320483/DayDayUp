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

线程池将线程和任务进行解耦，线程是线程，任务是任务，摆脱了之前通过 Thread 创建线程时的一个线程必须对应一个任务的限制。

在线程池中，同一个线程可以从阻塞队列中不断获取新任务来执行，其核心原理在于线程池对 Thread 进行了封装，并不是每次执行任务都会调用 Thread.start() 来创建新线程，而是让每个线程去执行一个“循环任务”，在这个“循环任务”中不停的检查是否有任务需要被执行，如果有则直接执行，也就是调用任务中的 run 方法，将 run 方法当成一个普通的方法执行，通过这种方式将只使用固定的线程就将所有任务的 run 方法串联起来。

### 线程池执行流程

- 流程图

![类加载过程](https://github.com/ZLOVE320483/DayDayUp/blob/main/pic/java_thread_pool_1.jpeg)