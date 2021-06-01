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

![类加载过程](https://github.com/ZLOVE320483/DayDayUp/blob/main/pic/java_thread_pool_1.png)

- 线程创建的流程

当任务提交之后，线程池首先会检查当前线程数，如果当前的线程数小于核心线程数（corePoolSize），比如最开始创建的时候线程数为 0，则新建线程并执行任务。
当提交的任务不断增加，创建的线程数等于核心线程数（corePoolSize），新增的任务会被添加到 workQueue 任务队列中，等待核心线程执行完当前任务后，重新从 workQueue 中获取任务执行。
假设任务非常多，达到了 workQueue 的最大容量，但是当前线程数小于最大线程数（maximumPoolSize），线程池会在核心线程数（corePoolSize）的基础上继续创建线程来执行任务。

假设任务继续增加，线程池的线程数达到最大线程数（maximumPoolSize），如果任务继续增加，这个时候线程池就会采用拒绝策略来拒绝这些任务。

在任务不断增加的过程中，线程池会逐一进行以下 4 个方面的判断

- 核心线程数（corePoolSize）
- 任务队列（workQueue）
- 最大线程数（maximumPoolSize）
- 拒绝策略

### ThreadPoolExecutor#execute 源码分析

``` java.util.concurrent.ThreadPoolExecutor#execute ```

```
 public void execute(Runnable command) {
     // 如果传入的Runnable的空，就抛出异常
     if (command == null)
         throw new NullPointerException();
     int c = ctl.get();
     // 线程池中的线程比核心线程数少 
     if (workerCountOf(c) < corePoolSize) {
         // 新建一个核心线程执行任务
         if (addWorker(command, true))
             return;
         c = ctl.get();
     }
     // 核心线程已满，但是任务队列未满，添加到队列中
     if (isRunning(c) && workQueue.offer(command)) {
         int recheck = ctl.get();
         // 任务成功添加到队列以后，再次检查是否需要添加新的线程，因为已存在的线程可能被销毁了
         if (! isRunning(recheck) && remove(command))
             // 如果线程池处于非运行状态，并且把当前的任务从任务队列中移除成功，则拒绝该任务
             reject(command);
         else if (workerCountOf(recheck) == 0)
             // 如果之前的线程已经被销毁完，新建一个非核心线程
             addWorker(null, false);
     }
     // 核心线程池已满，队列已满，尝试创建一个非核心新的线程
     else if (!addWorker(command, false))
         // 如果创建新线程失败，说明线程池关闭或者线程池满了，拒绝任务
         reject(command);
 }
```

- 逐行代码分析

```
//如果传入的Runnable的空，就抛出异常        
if (command == null)
   throw new NullPointerException();

```
execute 方法中通过 if 语句判断 command ，也就是 Runnable 任务是否等于 null，如果为 null 就抛出异常。

```
if (workerCountOf(c) < corePoolSize) { 
    if (addWorker(command, true)) 
        return;
        c = ctl.get();
}
```

判断当前线程数是否小于核心线程数，如果小于核心线程数就调用 addWorker() 方法增加一个 Worker，这里的 Worker 就可以理解为一个线程。

addWorker 方法的主要作用是在线程池中创建一个线程并执行传入的任务，如果返回 true 代表添加成功，如果返回 false 代表添加失败。

第一个参数表示传入的任务

第二个参数是个布尔值，如果布尔值传入 true 代表增加线程时判断当前线程是否少于 corePoolSize，小于则增加新线程（核心线程），大于等于则不增加；同理，如果传入 false 代表增加线程时判断当前线程是否少于 maximumPoolSize，小于则增加新线程（非核心线程），大于等于则不增加，所以这里的布尔值的含义是以核心线程数为界限还是以最大线程数为界限进行是否新增非核心线程的判断。

这一段判断相关源码如下

```
    private boolean addWorker(Runnable firstTask, boolean core) {     
                ...
                int wc = workerCountOf(c);//当前工作线程数
                //判断当前工作线程数>=最大线程数 或者 >=核心线程数(当core = true)
                if (wc >= CAPACITY ||
                    wc >= (core ? corePoolSize : maximumPoolSize))
                    return false;
                ...
```

最核心的就是 core ? corePoolSize : maximumPoolSize 这个三目运算。

```
      // 核心线程已满，但是任务队列未满，添加到队列中
      if (isRunning(c) && workQueue.offer(command)) {
          int recheck = ctl.get();
          // 任务成功添加到队列以后，再次检查是否需要添加新的线程，因为已存在的线程可能被销毁了
          if (! isRunning(recheck) && remove(command))
              // 如果线程池处于非运行状态，并且把当前的任务从任务队列中移除成功，则拒绝该任务
              reject(command);
          else if (workerCountOf(recheck) == 0)
              // 如果之前的线程已经被销毁完，新建一个非核心线程
              addWorker(null, false);
      }
```

如果代码执行到这里，说明当前线程数大于或等于核心线程数或者 addWorker 失败了，那么就需要通过

if (isRunning(c) && workQueue.offer(command)) 检查线程池状态是否为 Running，如果线程池状态是 Running 就通过 workQueue.offer(command) 将任务放入任务队列中，

任务成功添加到队列以后，再次检查线程池状态，如果线程池不处于 Running 状态，说明线程池被关闭，那么就移除刚刚添加到任务队列中的任务，并执行拒绝策略，代码如下：

```
            if (! isRunning(recheck) && remove(command))
                // 如果线程池处于非运行状态，并且把当前的任务从任务队列中移除成功，则拒绝该任务
                reject(command);
```

下面我们再来看后一个 else 分支：

```
            else if (workerCountOf(recheck) == 0)
                // 如果之前的线程已经被销毁完，新建一个非核心线程
                addWorker(null, false);
```

进入这个 else 说明前面判断到线程池状态为 Running，那么当任务被添加进来之后就需要防止没有可执行线程的情况发生（比如之前的线程被回收了或意外终止了），所以此时如果检查当前线程数为 0，也就是 workerCountOf(recheck) == 0，那就执行 addWorker() 方法新建一个非核心线程。

我们再来看最后一部分代码：

```
        // 核心线程池已满，队列已满，尝试创建一个非核心新的线程
        else if (!addWorker(command, false))
            // 如果创建新线程失败，说明线程池关闭或者线程池满了，拒绝任务
            reject(command);
```
执行到这里，说明线程池不是 Running 状态，又或者线程数 >= 核心线程数并且任务队列已经满了，根据规则，此时需要添加新线程，直到线程数达到“最大线程数”，所以此时就会再次调用 addWorker 方法并将第二个参数传入 false，传入 false 代表增加非核心线程。

addWorker 方法如果返回 true 代表添加成功，如果返回 false 代表任务添加失败，说明当前线程数已经达到 maximumPoolSize，然后执行拒绝策略 reject 方法。

如果执行到这里线程池的状态不是 Running，那么 addWorker 会失败并返回 false，所以也会执行拒绝策略 reject 方法。

### 线程复用源码分析

java.util.concurrent.ThreadPoolExecutor#runWorker 省略掉部分和复用无关的代码之后，代码如下：

```
    final void runWorker(Worker w) {
        Thread wt = Thread.currentThread();
        Runnable task = w.firstTask;
        w.firstTask = null;
        w.unlock(); // 释放锁 设置work的state=0 允许中断
        boolean completedAbruptly = true;
        try {
            //一直执行 如果task不为空 或者 从队列中获取的task不为空
            while (task != null || (task = getTask()) != null) {
                    task.run();//执行task中的run方法
                }
            }
            completedAbruptly = false;
        } finally {
            //1.将 worker 从数组 workers 里删除掉
            //2.根据布尔值 allowCoreThreadTimeOut 来决定是否补充新的 Worker 进数组 workers
            processWorkerExit(w, completedAbruptly);
        }
    }
```

可以看到，实现线程复用的逻辑主要在一个不停循环的 while 循环体中。

通过获取 Worker 的 firstTask 或者通过 getTask 方法从 workQueue 中获取待执行的任务

直接通过 task.run() 来执行具体的任务（而不是新建线程）

在这里，我们找到了线程复用最终的实现，通过取 Worker 的 firstTask 或者 getTask 方法从 workQueue 中取出了新任务，并直接调用 Runnable 的 run 方法来执行任务，也就是如之前所说的，每个线程都始终在一个大循环中，反复获取任务，然后执行任务，从而实现了线程的复用。
