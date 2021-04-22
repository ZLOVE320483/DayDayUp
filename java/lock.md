## Java“锁”事
### synchronized关键字
#### synchronized的作用
关键字synchronized的作用是实现线程间的同步。它的工作是对同步的代码加锁，使得每一次，只能有一个线程进入同步块，从而保证线程间的安全性。

关键字synchronized可以有多张用法，这里做一个简单的整理:

> 指定加锁对象：对给定对象加锁，进入同步代码前要获取给定对象的锁。
>
> 直接作用于实例方法：相当于给当前实例加锁，进入同步代码块前要获得当前实例的锁。
>
>直接作用于静态方法：相当于对当前类加锁，进入同步代码前要获取当前类的锁。

下面分别说一下上面的三点：

假设我们有下面这样一个Runnable,在run方法里对静态成员变量sCount自增10000次:

```
class Count implements Runnable {
    private static int sCount = 0;

    public static int getCount() {
        return sCount;
    }

    @Override
    public void run() {
        for (int i = 0; i < 10000; i++) {
            sCount++;
        }
    }
}
```
假设我们在两个Thread里面同时跑这个Runnable:
```
Count count = new Count();
Thread t1 = new Thread(count);
Thread t2 = new Thread(count);
t1.start();
t2.start();
try {
    t1.join();
    t2.join();
} catch (InterruptedException e) {
    e.printStackTrace();
}
System.out.print(Count.getCount());
```
得到的结果并不是20000,而是一个比20000小的数,如14233。

这是为什么呢？假设两个线程分别读取sCount为0,然后各自技术得到sCount为1,并先后写入这个结果,因此,虽然sCount++执行了2次,但是实际sCount的值只增加了1。

我们可以用指定加锁对象的方法解决这个问题,这里因为两个Thread跑的是同一个Count实例,所以可以直接给this加锁:
```
class Count implements Runnable {
    private static int sCount = 0;

    public static int getCount() {
        return sCount;
    }

    @Override
    public void run() {
        for (int i = 0; i < 10000; i++) {
            synchronized (this) {
                sCount++;
            }
        }
    }
}
```
我们也可以给实例方法加锁,这种方式和上面那一种的区别就是给this加锁,锁的区域比较小,两个线程交替执行sCount++操作,而给方法加锁的话,先拿到锁的线程会连续执行1000次sCount自增,然后再释放锁给另一个线程。
```
class Count implements Runnable {
    private static int sCount = 0;

    public static int getCount() {
        return sCount;
    }

    @Override
    public synchronized void run() {
        for (int i = 0; i < 10000; i++) {
            sCount++;
        }
    }
}
```

synchronized直接作用于静态方法的用法和上面的给实例方法加锁类似,不过它是作用于静态方法:
```
class Count implements Runnable {
    private static int sCount = 0;

    public static int getCount() {
        return sCount;
    }

    @Override
    public void run() {
        for (int i = 0; i < 10000; i++) {
            increase();
        }
    }

    private static synchronized void increase() {
        sCount++;
    }
}
```

