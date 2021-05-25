## Java对象生命周期

java对象的生命周期由7个阶段：

1. Created
2. In Use (strongly reachable)
3. Invisible
4. Unreachable
5. Collected
6. Finalized
7. Deallocated

### 创建对象（Created）

创建Java对象阶段的具体步骤如下：

1. 为对象分配存储空间；
2. 开始构造对象；
3. 从超类到子类对static成员进行初始化，类的static成员的初始化在ClassLoader加载该类时进行；
4. 超类成员变量按顺序初始化，递归调用超类的构造方法；
5. 子类成员变量按顺序初始化，子类构造方法调用

一旦对象被创建，并被分派给某些变量赋值，完成后这个对象的状态就切换到了应用阶段。

### 应用阶段（In Use）

至少被一个强引用持有着

### 不可见阶段（Invisible）

即使有强引用持有对象，但是这些强引用对于程序来说是不能访问的（accessible），就会进入这个阶段（非必须经历的阶段）

举个例子：

```
public void run() {
    try {
        Object foo = new Object();
        foo.doSomething();
    } catch (Exception e) {
        // whatever
    }
    while (true) { // do stuff } // loop forever
}
```
在上面的例子中，foo实在try catch block中新建的，当try catch执行完后，foo便不能被访问了，在run方法返回之前，foo是强引用，是gc root，所以它不能被回收。这就有可能产生内存泄漏（比如例子中一直死循环），对于这种情况必须显式的置null来保证垃圾回收。

### 不可达阶段（Unreachable）

对象处于不可达阶段是指该对象不再被任何由gc root的强引用的引用链可达的状态。

先说哪些是gc root：

- 虚拟机栈中引用的对象；
- 类静态变量引用的对象；
- 类常量引用的对象；
- JNI 的 native 方法栈中引用的对象；
- JNI 中的 global 对象；
- 活着的线程

凭什么这些对象是根对象，别的对象就不是呢？王侯将相宁有种乎！不是，并没有任何钦定的意思，这都是有原因的。虚拟机栈，也就是每个线程运行时的方法栈，栈中的每个栈帧对应一个方法调用，栈帧中的局部变量表里保存着对应方法的局部变量。试想一下，如果这些正在执行的方法中局部变量引用着的对象被回收了，这个线程还能正常运行吗？native 方法栈也是同理。另外，方法运行时，随时都可能会访问到类中的静态变量以及常量，这些类肯定也是不能被回收的，JNI global 对象也是同理。

举个循环引用的例子来说明：

```
public void buildDog() {
   Dog newDog = new Dog();
   Tail newTail = new Tail();
   newDog.tail = newTail;
   newTail.dog = newDog;
}
```

在运行buildDog时，内存中是这样的，Dog和Tail相互引用，并且两者都是栈的局部变量，都是gc root。

![循环引用](https://github.com/ZLOVE320483/DayDayUp/blob/main/pic/java_class_lifecycle_1.jpeg)

当buildDog return后，之前所占用的栈的一帧消失了，虽然Dog和Tail相互强引用，但是他们都不是gc root也没有被gc root强引用，所以成为待回收对象。


![循环引用](https://github.com/ZLOVE320483/DayDayUp/blob/main/pic/java_class_lifecycle_2.jpeg)

### 收集阶段（Collected）

当垃圾回收器发现该对象已经处于“不可达阶段”并且垃圾回收器已经对该对象的内存空间重新分配做好准备时，对象进入“收集阶段”。如果该对象已经重写了finalize()方法，并且没有被执行过，则执行该方法的操作。否则直接进入终结阶段。

### 终结阶段（Finalized）

当对象执行完finalize()方法后仍然处于不可达状态时，该对象进入终结阶段。在该阶段，等待垃圾回收器回收该对象空间。

### 重新分配阶段（Deallocated）

如果在完成上述所有工作完成后对象仍不可达，则垃圾回收器对该对象的所占用的内存空间进行回收或者再分配，该对象彻底消失。



