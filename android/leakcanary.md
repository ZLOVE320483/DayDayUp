## LeakCanary原理解析

### 原理概述
[LeakCanary官网](https://square.github.io/leakcanary/)

阅读完官网之后，总结LeakCanary原理为一下几点：

1. RefWatcher.watch()创建了一个KeyedWeakReference用于去观察对象。
2. 然后，在后台线程中，它会检测引用是否被清除了，并且是否没有触发GC。
3. 如果引用仍然没有被清除，那么它将会把堆栈信息保存在文件系统中的.hprof文件里。
4. HeapAnalyzerService被开启在一个独立的进程中，并且HeapAnalyzer使用了HAHA开源库解析了指定时刻的堆栈快照文件heap dump。
5. 从heap dump中，HeapAnalyzer根据一个独特的引用key找到了KeyedWeakReference，并且定位了泄露的引用。
6. HeapAnalyzer为了确定是否有泄露，计算了到GC Roots的最短强引用路径，然后建立了导致泄露的链式引用。
7. 这个结果被传回到app进程中的DisplayLeakService，然后一个泄露通知便展现出来了。

> 结论就是：在一个Activity执行完onDestroy()之后，将它放入WeakReference中，然后将这个WeakReference类型的Activity对象与ReferenceQueque关联。这时再从ReferenceQueque中查看是否有没有该对象，如果没有，执行gc，再次查看，还是没有的话则判断发生内存泄露了。最后用HAHA这个开源库去分析dump之后的heap内存。

### 简单示例
首先在需要引用LeakCanary库的module中的gradle文件添加如下依赖：
```
dependencies {
    testImplementation 'com.squareup.leakcanary:leakcanary-android:1.6.3'
    debugImplementation 'com.squareup.leakcanary:leakcanary-android:1.6.3'
}
```
然后在Application中配置：
```
class DayUpApplication : Application() {

    private var mRefWatcher: RefWatcher? = null

    companion object {
        fun getRefWatcher(context: Context): RefWatcher? {
            val application = context.applicationContext as DayUpApplication
            return application.mRefWatcher
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (!LeakCanary.isInAnalyzerProcess(this)) {
            mRefWatcher = LeakCanary.install(this)
        }
    }
}
```
在Application初始化过程中，会首先判断当前进程是否是Leakcanary专门用于分析heap内存的而创建的那个进程，即HeapAnalyzerService所在的进程，如果是的话，则不进行Application中的初始化功能。如果是当前应用所处的主进程的话，则会执行注释2处的LeakCanary.install(this)进行LeakCanary的安装。只需这样简单的几行代码，我们就可以在应用中检测是否产生了内存泄露了。当然，这样使用只会检测Activity是否发生内存泄漏，如果要检测Fragment在执行完onDestroy()之后是否发生内存泄露的话，则需要在Fragment的onDestroy()方法中加上如下两行代码去监视当前的Fragment：
```
    override fun onDestroy() {
        activity?.run {
            DayUpApplication.getRefWatcher(this)?.watch(this)
        }
        super.onDestroy()
    }
```
上面的RefWatcher其实就是一个引用观察者对象，是用于监测当前实例对象的引用状态的。从以上的分析可以了解到，核心代码就是LeakCanary.install(this)这行代码，接下来，就从这里出发将LeakCanary一步一步进行拆解。

### 源码分析
1. LeakCanary#install()
```
  public static @NonNull RefWatcher install(@NonNull Application application) {
    return refWatcher(application).listenerServiceClass(DisplayLeakService.class)
        .excludedRefs(AndroidExcludedRefs.createAppDefaults().build())
        .buildAndInstall();
  }
```
在install()方法中的处理，可以分解为如下四步：

- refWatcher(application)
- 链式调用listenerServiceClass(DisplayLeakService.class)
- 链式调用excludedRefs(AndroidExcludedRefs.createAppDefaults().build())
- 链式调用buildAndInstall()

首先，我们来看下第一步，这里调用了LeakCanary类的refWatcher方法，如下所示：
```
  public static @NonNull AndroidRefWatcherBuilder refWatcher(@NonNull Context context) {
    return new AndroidRefWatcherBuilder(context);
  }
```
然后新建了一个AndroidRefWatcherBuilder对象，再看看AndroidRefWatcherBuilder这个类。

2. AndroidRefWatcherBuilder
```
public final class AndroidRefWatcherBuilder extends RefWatcherBuilder<AndroidRefWatcherBuilder> {

  AndroidRefWatcherBuilder(@NonNull Context context) {
    this.context = context.getApplicationContext();
  }

}
```

在AndroidRefWatcherBuilder的构造方法中仅仅是将外部传入的applicationContext对象保存起来了。AndroidRefWatcherBuilder是一个适配Android平台的引用观察者构造器对象，它继承了RefWatcherBuilder，RefWatcherBuilder是一个负责建立引用观察者RefWatcher实例的基类构造器。继续看看RefWatcherBuilder这个类。

3. RefWatcherBuilder
```
public class RefWatcherBuilder<T extends RefWatcherBuilder<T>> {
  public RefWatcherBuilder() {
    heapDumpBuilder = new HeapDump.Builder();
  }

}
```
在RefWatcher的基类构造器RefWatcherBuilder的构造方法中新建了一个HeapDump的构造器对象。其中HeapDump就是一个保存heap dump信息的数据结构。

接着来分析下install()方法中的链式调用的listenerServiceClass(DisplayLeakService.class)这部分逻辑。

4. AndroidRefWatcherBuilder#listenerServiceClass()
```
  public @NonNull AndroidRefWatcherBuilder listenerServiceClass(
      @NonNull Class<? extends AbstractAnalysisResultService> listenerServiceClass) {
    enableDisplayLeakActivity = DisplayLeakService.class.isAssignableFrom(listenerServiceClass);
    return heapDumpListener(new ServiceHeapDumpListener(context, listenerServiceClass));
  }
```
在这里，传入了一个DisplayLeakService的Class对象，它的作用是展示泄露分析的结果日志，然后会展示一个用于跳转到显示泄露界面DisplayLeakActivity的通知。在listenerServiceClass()这个方法中新建了一个ServiceHeapDumpListener对象，看看它内部的操作。

5. ServiceHeapDumpListener

