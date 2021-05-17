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
#### 等待(wait) 和 通知(notify)

Object有两个很重要的接口:Object.wait()和Object.notify()

当在一个对象实例上调用了wait()方法后,当前线程就会在这个对象上等待。直到其他线程调用了这个对象的notify()方法或者notifyAll()方法。notifyAll()方法与notify()方法的区别是它会唤醒所有正在等待这个对象的线程,而notify()方法只会随机唤醒一个等待该对象的线程。

wait()、notify()和notifyAll()都需要在synchronized语句中使用:

```
public class LockThread extends Thread {

    private Object mLock;

    public LockThread(Object lock) {
        this.mLock = lock;
    }

    @Override
    public void run() {
        super.run();

        synchronized (mLock) {
            try {
                mLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("--- LockThread ---");
        }
    }
}
```

```
    public static void main(String[] args) {
        Object lock = new Object();
        LockThread thread = new LockThread(lock);
        thread.start();

        System.out.println("--- before sleep ---");

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("--- after sleep ---");

        synchronized (lock) {
            lock.notify();
        }

        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
```
从上面的例子可以看出来,在调用wait()方法实际上已经释放了对象的锁,所以在其他线程中才能获取到这个对象的锁,从而进行notify操作。而等待的线程被唤醒后又需要重新获得对象的锁。

### synchronized容易犯的隐蔽错误

#### 是否给同一个对象加锁

在用synchronized给对象加锁的时候需要注意加锁是不是同一个,如将代码改成这样:
```
    public static void main(String[] args) {
        Thread t1 = new Thread(new Count());
        Thread t2 = new Thread(new Count());
        t1.start();
        t2.start();
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.print(Count.getCount());
    }
```
因为两个线程跑的是不同的Count实例,所以用给指定对象加锁和给实例方法加锁的方法都不能避免两个线程同时对静态成员变量sCount进行自增操作。

但是如果用第三种作用于静态方法的写法,就能正确的加锁。

#### 是否给错误的对象加锁

如我们将sCount的类型改成Integer,并且在sCount++的时候直接对sCount加锁会发生什么事情呢(毕竟我们会很自然的给要操作的对象加锁来实现线程同步)？
```
public class Count implements Runnable {

    private static Integer sCount = 0;

    public static int getCount() {
        return sCount;
    }

    @Override
    public void run() {
        for (int i = 0; i < 1000; i++) {
            synchronized (sCount) {
                sCount++;
            }
        }
    }

}
```
```
    public static void main(String[] args) {
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
    }
```
最后的得到的结果仍然是比2000小的值。这是为什么呢？

> 在Java中,Integer使用不变对象。也就是对象一旦被创建,就不可能被修改。也就是说,如果你有一个Integer代表1,那么它就永远是1,你不可能改变Integer的值,使它位。那如果你需要2怎么办呢？也很简单,新建一个Integer,并让它表示2即可。

也就是说sCount在真实执行时变成了:

```sCount = Integer.valueOf(sCount.intValue()+1);```

进一步看Integer.valueOf()，我们可以看到:

```
public static Integer valueOf(int i) {
    assert IntegerCache.high >= 127;
    if (i >= IntegerCache.low && i <= IntegerCache.high)
        return IntegerCache.cache[i + (-IntegerCache.low)];
    return new Integer(i);
}
```

所以在多个线程中,由于sCount一直在变,并不是同一个对象,所以两个线程的加锁可能加在了不同的Integer对象上,并没有真正的锁住代码块。

### 重入锁
ReentrantLock的意思是Re-Entrant-Lock也就是重入锁,它的特点就是在同一个线程中可以重复加锁,只需要解锁同样的次数就能真正解锁:
```
public class ReentrantLockThread extends Thread {

    private ReentrantLock mLock = new ReentrantLock();

    @Override
    public void run() {
        super.run();
        mLock.lock();
        System.out.println("--- outside ---");
        mLock.lock();
        System.out.println("--- inside ----");
        mLock.unlock();
        mLock.unlock();
    }
}


```

事实上synchronized也是可重入的,比如下面的代码同样是可以正常退出的:


```
public class SynchronizedThread extends Thread {

    @Override
    public void run() {
        super.run();
        synchronized (this) {
            System.out.println("--- outside ---");
            synchronized (this) {
                System.out.println("--- inside ----");
            }
        }
    }
}
```

与synchronized相比,重入锁需要程序员手动调用加锁和解锁,也因为如此,重入锁对逻辑控制的灵活性要远远好于synchronized。

重入锁可以完全替代synchronized关键字。在JDK 5.0的早起版本中,重入锁的性能远远好于synchronized。但从JDK 6.0开始,JDK在synchronized做了大量优化,使得两者的性能差距并不大。