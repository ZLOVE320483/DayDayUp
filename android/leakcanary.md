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
```
public final class ServiceHeapDumpListener implements HeapDump.Listener {

  public ServiceHeapDumpListener(@NonNull final Context context,
      @NonNull final Class<? extends AbstractAnalysisResultService> listenerServiceClass) {
    this.listenerServiceClass = checkNotNull(listenerServiceClass, "listenerServiceClass");
    this.context = checkNotNull(context, "context").getApplicationContext();
  }
}
```
可以看到这里仅仅是在ServiceHeapDumpListener中保存了DisplayLeakService的Class对象和application对象。它的作用就是接收一个heap dump去分析。

然后我们继续看install()方法链式调用.excludedRefs(AndroidExcludedRefs.createAppDefaults().build())的这部分代码。先看AndroidExcludedRefs.createAppDefaults()。

6. AndroidExcludedRefs#createAppDefaults()
```
public enum AndroidExcludedRefs {

  public static @NonNull ExcludedRefs.Builder createAppDefaults() {
    return createBuilder(EnumSet.allOf(AndroidExcludedRefs.class));
  }

  public static @NonNull ExcludedRefs.Builder createBuilder(EnumSet<AndroidExcludedRefs> refs) {
    ExcludedRefs.Builder excluded = ExcludedRefs.builder();
    for (AndroidExcludedRefs ref : refs) {
      if (ref.applies) {
        ref.add(excluded);
        ((ExcludedRefs.BuilderWithParams) excluded).named(ref.name());
      }
    }
    return excluded;
  }
}
```
先来说下AndroidExcludedRefs这个类，它是一个enum类，它声明了Android SDK和厂商定制的SDK中存在的内存泄露的case，根据AndroidExcludedRefs这个类的类名就可看出这些case都会被Leakcanary的监测过滤掉。目前这个版本是有46种这样的case被包含在内，后续可能会一直增加。然后EnumSet.allOf(AndroidExcludedRefs.class)这个方法将会返回一个包含AndroidExcludedRefs元素类型的EnumSet。Enum是一个抽象类，在这里具体的实现类是通用正规型的RegularEnumSet，如果Enum里面的元素个数大于64，则会使用存储大数据量的JumboEnumSet。最后，在createBuilder这个方法里面构建了一个排除引用的建造器excluded，将各式各样的case分门别类地保存起来再返回出去。

最后，我们看到链式调用的最后一步buildAndInstall()。

7. AndroidRefWatcherBuilder#buildAndInstall()
```
  private boolean watchActivities = true;
  private boolean watchFragments = true;

  public @NonNull RefWatcher buildAndInstall() {
    // 1
    if (LeakCanaryInternals.installedRefWatcher != null) {
      throw new UnsupportedOperationException("buildAndInstall() should only be called once.");
    }
    // 2
    RefWatcher refWatcher = build();
    if (refWatcher != DISABLED) {
      // 3
      if (enableDisplayLeakActivity) {
        LeakCanaryInternals.setEnabledAsync(context, DisplayLeakActivity.class, true);
      }
      if (watchActivities) {
        // 4
        ActivityRefWatcher.install(context, refWatcher);
      }
      if (watchFragments) {
        // 5
        FragmentRefWatcher.Helper.install(context, refWatcher);
      }
    }
    // 6
    LeakCanaryInternals.installedRefWatcher = refWatcher;
    return refWatcher;
  }
```
首先，在注释1处，会判断LeakCanaryInternals.installedRefWatcher是否已经被赋值，如果被赋值了，则会抛出异常，警告buildAndInstall()这个方法应该仅仅只调用一次，在此方法结束时，即在注释6处，该LeakCanaryInternals.installedRefWatcher才会被赋值。再来看注释2处，调用了AndroidRefWatcherBuilder其基类RefWatcherBuilder的build()方法，我们它是如何建造的。

8. RefWatcherBuilder#build()
```
  public final RefWatcher build() {
    if (isDisabled()) {
      return RefWatcher.DISABLED;
    }

    if (heapDumpBuilder.excludedRefs == null) {
      heapDumpBuilder.excludedRefs(defaultExcludedRefs());
    }

    HeapDump.Listener heapDumpListener = this.heapDumpListener;
    if (heapDumpListener == null) {
      heapDumpListener = defaultHeapDumpListener();
    }

    DebuggerControl debuggerControl = this.debuggerControl;
    if (debuggerControl == null) {
      debuggerControl = defaultDebuggerControl();
    }

    HeapDumper heapDumper = this.heapDumper;
    if (heapDumper == null) {
      heapDumper = defaultHeapDumper();
    }

    WatchExecutor watchExecutor = this.watchExecutor;
    if (watchExecutor == null) {
      watchExecutor = defaultWatchExecutor();
    }

    GcTrigger gcTrigger = this.gcTrigger;
    if (gcTrigger == null) {
      gcTrigger = defaultGcTrigger();
    }

    if (heapDumpBuilder.reachabilityInspectorClasses == null) {
      heapDumpBuilder.reachabilityInspectorClasses(defaultReachabilityInspectorClasses());
    }

    return new RefWatcher(watchExecutor, debuggerControl, gcTrigger, heapDumper, heapDumpListener,
        heapDumpBuilder);
  }
```
可以看到，RefWatcherBuilder包含了7个组成部分：
- excludedRefs : 记录可以被忽略的泄漏路径。
- heapDumpListener : 转储堆信息到hprof文件，并在解析完 hprof 文件后进行回调，最后通知 DisplayLeakService 弹出泄漏提醒。
- debuggerControl : 判断是否处于调试模式，调试模式中不会进行内存泄漏检测。为什么呢？因为在调试过程中可能会保留上一个引用从而导致错误信息上报。
- heapDumper : 堆信息转储者，dump 内存泄漏处的 heap 信息到 hprof 文件。
- watchExecutor : 线程控制器，在 onDestroy() 之后并且主线程空闲时执行内存泄漏检测。
- gcTrigger : 用于 GC，watchExecutor 首次检测到可能的内存泄漏，会主动进行 GC，GC 之后会再检测一次，仍然泄漏的判定为内存泄漏，最后根据heapDump信息生成相应的泄漏引用链。
- reachabilityInspectorClasses : 用于要进行可达性检测的类列表。

最后，会使用建造者模式将这些组成部分构建成一个新的RefWatcher并将其返回。

我们继续看回到AndroidRefWatcherBuilder的注释3处的 LeakCanaryInternals.setEnabledAsync(context, DisplayLeakActivity.class, true)这行代码。

9. LeakCanaryInternals#setEnabledAsync()
```
  public static void setEnabledAsync(Context context, final Class<?> componentClass,
      final boolean enabled) {
    final Context appContext = context.getApplicationContext();
    AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
      @Override public void run() {
        setEnabledBlocking(appContext, componentClass, enabled);
      }
    });
  }
```
在这里使用了AsyncTask内部自带的THREAD_POOL_EXECUTOR线程池进行阻塞式地显示DisplayLeakActivity。

然后我们再继续看AndroidRefWatcherBuilder的注释4处的代码。

10. ActivityRefWatcher#install()
```
  public static void install(@NonNull Context context, @NonNull RefWatcher refWatcher) {
    Application application = (Application) context.getApplicationContext();
    ActivityRefWatcher activityRefWatcher = new ActivityRefWatcher(application, refWatcher);

    application.registerActivityLifecycleCallbacks(activityRefWatcher.lifecycleCallbacks);
  }
```
可以看到，在注释1处创建一个自己的activityRefWatcher实例，并在注释2处调用了application的registerActivityLifecycleCallbacks()方法，这样就能够监听activity对应的生命周期事件了。继续看看activityRefWatcher.lifecycleCallbacks里面的操作。
```
    private final Application.ActivityLifecycleCallbacks lifecycleCallbacks =
      new ActivityLifecycleCallbacksAdapter() {
        @Override public void onActivityDestroyed(Activity activity) {
          refWatcher.watch(activity);
        }
      };
```
很明显，实现并重写了Application的ActivityLifecycleCallbacks的onActivityDestroyed()方法，这样便能在所有Activity执行完onDestroyed()方法之后调用 refWatcher.watch(activity)这行代码进行内存泄漏的检测了。

我们再看会注释5处的FragmentRefWatcher.Helper.install(context, refWatcher)这行代码：

11. FragmentRefWatcher.Helper#install()
```
public interface FragmentRefWatcher {

  void watchFragments(Activity activity);

  final class Helper {

    private static final String SUPPORT_FRAGMENT_REF_WATCHER_CLASS_NAME =
        "com.squareup.leakcanary.internal.SupportFragmentRefWatcher";

    public static void install(Context context, RefWatcher refWatcher) {
      List<FragmentRefWatcher> fragmentRefWatchers = new ArrayList<>();
      // 1
      if (SDK_INT >= O) {
        fragmentRefWatchers.add(new AndroidOFragmentRefWatcher(refWatcher));
      }
      // 2
      try {
        Class<?> fragmentRefWatcherClass = Class.forName(SUPPORT_FRAGMENT_REF_WATCHER_CLASS_NAME);
        Constructor<?> constructor =
            fragmentRefWatcherClass.getDeclaredConstructor(RefWatcher.class);
        FragmentRefWatcher supportFragmentRefWatcher =
            (FragmentRefWatcher) constructor.newInstance(refWatcher);
        fragmentRefWatchers.add(supportFragmentRefWatcher);
      } catch (Exception ignored) {
      }

      if (fragmentRefWatchers.size() == 0) {
        return;
      }

      Helper helper = new Helper(fragmentRefWatchers);
      // 3
      Application application = (Application) context.getApplicationContext();
      application.registerActivityLifecycleCallbacks(helper.activityLifecycleCallbacks);
    }

    private final Application.ActivityLifecycleCallbacks activityLifecycleCallbacks =
        new ActivityLifecycleCallbacksAdapter() {
          @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            for (FragmentRefWatcher watcher : fragmentRefWatchers) {
              watcher.watchFragments(activity);
            }
          }
        };

    private final List<FragmentRefWatcher> fragmentRefWatchers;

    private Helper(List<FragmentRefWatcher> fragmentRefWatchers) {
      this.fragmentRefWatchers = fragmentRefWatchers;
    }
  }
}
```
这里面的逻辑很简单，首先在注释1处将Android标准的Fragment的RefWatcher类，即AndroidOFragmentRefWatcher添加到新创建的fragmentRefWatchers中。在注释2处使用反射将leakcanary-support-fragment包下面的SupportFragmentRefWatcher添加进来，如果你在app的build.gradle下没有添加下面这行引用的话，则会拿不到此类，即LeakCanary只会兼顾监测Activity和标准Fragment这两种情况。

``` 
debugImplementation 'com.squareup.leakcanary:leakcanary-support-fragment:1.6.2'
```

继续看到注释3处helper.activityLifecycleCallbacks里面的代码。

```
    private final Application.ActivityLifecycleCallbacks activityLifecycleCallbacks =
        new ActivityLifecycleCallbacksAdapter() {
          @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            for (FragmentRefWatcher watcher : fragmentRefWatchers) {
              watcher.watchFragments(activity);
            }
          }
        };
```

可以看到，在Activity执行完onActivityCreated()方法之后，会调用指定watcher的watchFragments()方法，注意，这里的watcher可能有两种，但不管是哪一种，都会使用当前传入的activity获取到对应的FragmentManager/SupportFragmentManager对象，调用它的registerFragmentLifecycleCallbacks()方法，在对应的onDestroyView()和onDestoryed()方法执行完后，分别使用refWatcher.watch(view)和refWatcher.watch(fragment)进行内存泄漏的检测，代码如下所示。
```
  private final FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks =
      new FragmentManager.FragmentLifecycleCallbacks() {

        @Override public void onFragmentViewDestroyed(FragmentManager fm, Fragment fragment) {
          View view = fragment.getView();
          if (view != null) {
            refWatcher.watch(view);
          }
        }

        @Override
        public void onFragmentDestroyed(FragmentManager fm, Fragment fragment) {
          refWatcher.watch(fragment);
        }
      };
```

注意，下面到真正关键的地方了，接下来分析refWatcher.watch()这行代码。

12. RefWatcher#watch()
```
  public void watch(Object watchedReference, String referenceName) {
    if (this == DISABLED) {
      return;
    }
    checkNotNull(watchedReference, "watchedReference");
    checkNotNull(referenceName, "referenceName");
    final long watchStartNanoTime = System.nanoTime();
    // 1
    String key = UUID.randomUUID().toString();
    // 2
    retainedKeys.add(key);
    // 3
    final KeyedWeakReference reference =
        new KeyedWeakReference(watchedReference, key, referenceName, queue);
    // 4
    ensureGoneAsync(watchStartNanoTime, reference);
  }
```
注意到在注释1处使用随机的UUID保证了每个检测对象对应的

key 的唯一性。在注释2处将生成的key添加到类型为CopyOnWriteArraySet的Set集合中。在注释3处新建了一个自定义的弱引用KeyedWeakReference，看看它内部的实现。

13. KeyedWeakReference
```
final class KeyedWeakReference extends WeakReference<Object> {
  public final String key;
  public final String name;

  KeyedWeakReference(Object referent, String key, String name,
      ReferenceQueue<Object> referenceQueue) {
    super(checkNotNull(referent, "referent"), checkNotNull(referenceQueue, "referenceQueue"));
    this.key = checkNotNull(key, "key");
    this.name = checkNotNull(name, "name");
  }
}
```
可以看到，在KeyedWeakReference内部，使用了key和name标识了一个被检测的WeakReference对象。在注释1处，将弱引用和引用队列 ReferenceQueue 关联起来，如果弱引用referent持有的对象被GC回收，JVM就会把这个弱引用加入到与之关联的引用队列referenceQueue中。即 KeyedWeakReference 持有的 Activity 对象如果被GC回收，该对象就会加入到引用队列 referenceQueue 中。

接着我们回到RefWatcher.watch()里注释4处的ensureGoneAsync()方法。

14. RefWatcher#ensureGoneAsync()
```
  private void ensureGoneAsync(final long watchStartNanoTime, final KeyedWeakReference reference) {
    // 1
    watchExecutor.execute(new Retryable() {
      @Override public Retryable.Result run() {
        // 2
        return ensureGone(reference, watchStartNanoTime);
      }
    });
  }
```
在ensureGoneAsync()方法中，在注释1处使用 watchExecutor 执行了注释2处的 ensureGone 方法，watchExecutor 是 AndroidWatchExecutor 的实例。

下面看看watchExecutor内部的逻辑。

15. AndroidWatchExecutor
```
public final class AndroidWatchExecutor implements WatchExecutor {

  static final String LEAK_CANARY_THREAD_NAME = "LeakCanary-Heap-Dump";
  private final Handler mainHandler;
  private final Handler backgroundHandler;
  private final long initialDelayMillis;
  private final long maxBackoffFactor;

  public AndroidWatchExecutor(long initialDelayMillis) {
    mainHandler = new Handler(Looper.getMainLooper());
    HandlerThread handlerThread = new HandlerThread(LEAK_CANARY_THREAD_NAME);
    handlerThread.start();
    // 1
    backgroundHandler = new Handler(handlerThread.getLooper());
    this.initialDelayMillis = initialDelayMillis;
    maxBackoffFactor = Long.MAX_VALUE / initialDelayMillis;
  }

  @Override public void execute(@NonNull Retryable retryable) {
    // 2
    if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
      waitForIdle(retryable, 0);
    } else {
      postWaitForIdle(retryable, 0);
    }
  }

  private void postWaitForIdle(final Retryable retryable, final int failedAttempts) {
    mainHandler.post(new Runnable() {
      @Override public void run() {
        waitForIdle(retryable, failedAttempts);
      }
    });
  }

  private void waitForIdle(final Retryable retryable, final int failedAttempts) {
    // This needs to be called from the main thread.
    Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {
      @Override public boolean queueIdle() {
        postToBackgroundWithDelay(retryable, failedAttempts);
        return false;
      }
    });
  }

  private void postToBackgroundWithDelay(final Retryable retryable, final int failedAttempts) {
    long exponentialBackoffFactor = (long) Math.min(Math.pow(2, failedAttempts), maxBackoffFactor);
    // 3
    long delayMillis = initialDelayMillis * exponentialBackoffFactor;
    // 4
    backgroundHandler.postDelayed(new Runnable() {
      @Override public void run() {
        // 5
        Retryable.Result result = retryable.run();
        // 6
        if (result == RETRY) {
          postWaitForIdle(retryable, failedAttempts + 1);
        }
      }
    }, delayMillis);
  }
}
```
在注释1处AndroidWatchExecutor的构造方法中，注意到这里使用HandlerThread的looper新建了一个backgroundHandler，后面会用到。在注释2处，会判断当前线程是否是主线程，如果是，则直接调用waitForIdle()方法，如果不是，则调用postWaitForIdle()，来看看这个方法。

很清晰，这里使用了在构造方法中用主线程looper构造的mainHandler进行post，那么waitForIdle()最终也会在主线程执行。接着看看waitForIdle()的实现。

这里MessageQueue.IdleHandler()回调方法的作用是当 looper 空闲的时候，会回调 queueIdle 方法，然后执行内部的postToBackgroundWithDelay()方法。接下来看看它的实现。

先看到注释6处，可以明白，postToBackgroundWithDelay()是一个递归方法，如果result 一直等于RETRY的话，则会一直执行postWaitForIdle()方法。在回到注释3处，这里initialDelayMillis 的默认值是 5s，因此delayMillis就是5s。在注释4处，使用了在构造方法中用HandlerThread的looper新建的backgroundHandler进行异步延时执行retryable的run()方法。这个run()方法里执行的就是RefWatcher的ensureGoneAsync()方法中注释2处的ensureGone()这行代码，继续看它内部的逻辑。

16. RefWatcher#ensureGone()

```
  Retryable.Result ensureGone(final KeyedWeakReference reference, final long watchStartNanoTime) {
    long gcStartNanoTime = System.nanoTime();
    long watchDurationMs = NANOSECONDS.toMillis(gcStartNanoTime - watchStartNanoTime);

    // 1
    removeWeaklyReachableReferences();

    // 2
    if (debuggerControl.isDebuggerAttached()) {
      // The debugger can create false leaks.
      return RETRY;
    }

    // 3
    if (gone(reference)) {
      return DONE;
    }

    // 4
    gcTrigger.runGc();
    removeWeaklyReachableReferences();

    // 5
    if (!gone(reference)) {
      long startDumpHeap = System.nanoTime();
      long gcDurationMs = NANOSECONDS.toMillis(startDumpHeap - gcStartNanoTime);

      File heapDumpFile = heapDumper.dumpHeap();
      if (heapDumpFile == RETRY_LATER) {
        // Could not dump the heap.
        return RETRY;
      }
      long heapDumpDurationMs = NANOSECONDS.toMillis(System.nanoTime() - startDumpHeap);

      HeapDump heapDump = heapDumpBuilder.heapDumpFile(heapDumpFile).referenceKey(reference.key)
          .referenceName(reference.name)
          .watchDurationMs(watchDurationMs)
          .gcDurationMs(gcDurationMs)
          .heapDumpDurationMs(heapDumpDurationMs)
          .build();

      heapdumpListener.analyze(heapDump);
    }
    return DONE;
  }
```

在注释1处，执行了removeWeaklyReachableReferences()这个方法，接下来分析下它的含义。

```
private void removeWeaklyReachableReferences() {
 KeyedWeakReference ref;
 while ((ref = (KeyedWeakReference) queue.poll()) != null) {
 retainedKeys.remove(ref.key);
 }
}
```
这里使用了while循环遍历 ReferenceQueue ，并从 retainedKeys中移除对应的Reference。

再看到注释2处，当Android设备处于debug状态时，会直接返回RETRY进行延时重试检测的操作。在注释3处，我们看看gone(reference)这个方法的逻辑。

```
private boolean gone(KeyedWeakReference reference) {
 return !retainedKeys.contains(reference.key);
}
```

这里会判断 retainedKeys 集合中是否还含有 reference，若没有，证明已经被回收了，若含有，可能已经发生内存泄露（或Gc还没有执行回收）。前面的分析中我们知道了 reference 被回收的时候，会被加进 referenceQueue 里面，然后我们会调用removeWeaklyReachableReferences()遍历 referenceQueue 移除掉 retainedKeys 里面的 refrence。

接着我们看到注释4处，执行了gcTrigger的runGc()方法进行垃圾回收，然后使用了removeWeaklyReachableReferences()方法移除已经被回收的引用。这里我们在深入地分析下runGc()的实现。

```
public interface GcTrigger {
    GcTrigger DEFAULT = new GcTrigger() {
        public void runGc() {
            Runtime.getRuntime().gc();
            this.enqueueReferences();
            System.runFinalization();
        }

        private void enqueueReferences() {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException var2) {
                throw new AssertionError();
            }
        }
    };

    void runGc();
}
```
这里并没有使用System.gc()方法进行回收，因为system.gc()并不会每次都执行。而是从AOSP中拷贝一段GC回收的代码，从而相比System.gc()更能够保证进行垃圾回收的工作。

最后我们分析下注释5处的代码处理。首先会判断activity 如果还没有被回收，则证明发生内存泄露，进行if判断里面的操作。在里面先调用堆信息转储者heapDumper的dumpHeap()生成相应的 hprof 文件。这里的heapDumper是一个HeapDumper接口，具体的实现是AndroidHeapDumper。我们分析下AndroidHeapDumper的dumpHeap()方法是如何生成hprof文件的。

```
  public File dumpHeap() {
    File heapDumpFile = leakDirectoryProvider.newHeapDumpFile();

    if (heapDumpFile == RETRY_LATER) {
      return RETRY_LATER;
    }

    FutureResult<Toast> waitingForToast = new FutureResult<>();
    showToast(waitingForToast);

    if (!waitingForToast.wait(5, SECONDS)) {
      CanaryLog.d("Did not dump heap, too much time waiting for Toast.");
      return RETRY_LATER;
    }

    Notification.Builder builder = new Notification.Builder(context)
        .setContentTitle(context.getString(R.string.leak_canary_notification_dumping));
    Notification notification = LeakCanaryInternals.buildNotification(context, builder);
    NotificationManager notificationManager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    int notificationId = (int) SystemClock.uptimeMillis();
    notificationManager.notify(notificationId, notification);

    Toast toast = waitingForToast.get();
    try {
      Debug.dumpHprofData(heapDumpFile.getAbsolutePath());
      cancelToast(toast);
      notificationManager.cancel(notificationId);
      return heapDumpFile;
    } catch (Exception e) {
      CanaryLog.d(e, "Could not dump heap");
      // Abort heap dump
      return RETRY_LATER;
    }
  }
```

这里的核心操作就是调用了

Android SDK的API Debug.dumpHprofData() 来生成 hprof 文件。

如果这个文件等于RETRY_LATER则表示生成失败，直接返回RETRY进行延时重试检测的操作。如果不等于的话，则表示生成成功，最后会执行heapdumpListener的analyze()对新创建的HeapDump对象进行泄漏分析。由前面对AndroidRefWatcherBuilder的listenerServiceClass()的分析可知，heapdumpListener的实现

就是ServiceHeapDumpListener，接着看到ServiceHeapDumpListener的analyze方法。

17. ServiceHeapDumpListener#analyze()
```
public final class HeapAnalyzerService extends ForegroundService
    implements AnalyzerProgressListener {

  private static final String LISTENER_CLASS_EXTRA = "listener_class_extra";
  private static final String HEAPDUMP_EXTRA = "heapdump_extra";

  public static void runAnalysis(Context context, HeapDump heapDump,
      Class<? extends AbstractAnalysisResultService> listenerServiceClass) {
    setEnabledBlocking(context, HeapAnalyzerService.class, true);
    setEnabledBlocking(context, listenerServiceClass, true);
    Intent intent = new Intent(context, HeapAnalyzerService.class);
    intent.putExtra(LISTENER_CLASS_EXTRA, listenerServiceClass.getName());
    intent.putExtra(HEAPDUMP_EXTRA, heapDump);
    ContextCompat.startForegroundService(context, intent);
  }

  public HeapAnalyzerService() {
    super(HeapAnalyzerService.class.getSimpleName(), R.string.leak_canary_notification_analysing);
  }

  @Override protected void onHandleIntentInForeground(@Nullable Intent intent) {
    if (intent == null) {
      CanaryLog.d("HeapAnalyzerService received a null intent, ignoring.");
      return;
    }
    String listenerClassName = intent.getStringExtra(LISTENER_CLASS_EXTRA);
    HeapDump heapDump = (HeapDump) intent.getSerializableExtra(HEAPDUMP_EXTRA);

    HeapAnalyzer heapAnalyzer =
        new HeapAnalyzer(heapDump.excludedRefs, this, heapDump.reachabilityInspectorClasses);

    AnalysisResult result = heapAnalyzer.checkForLeak(heapDump.heapDumpFile, heapDump.referenceKey,
        heapDump.computeRetainedHeapSize);
    AbstractAnalysisResultService.sendResultToListener(this, listenerClassName, heapDump, result);
  }

  @Override public void onProgressUpdate(Step step) {
    int percent = (int) ((100f * step.ordinal()) / Step.values().length);
    CanaryLog.d("Analysis in progress, working on: %s", step.name());
    String lowercase = step.name().replace("_", " ").toLowerCase();
    String message = lowercase.substring(0, 1).toUpperCase() + lowercase.substring(1);
    showForegroundNotification(100, percent, false, message);
  }
}
```
可以看到，这里执行了HeapAnalyzerService的runAnalysis()方法，为了避免减慢app进程或占用内存，这里将HeapAnalyzerService设置在了一个独立的进程中。接着继续分析runAnalysis()方法里面的处理。

```
  public @NonNull AnalysisResult checkForLeak(@NonNull File heapDumpFile,
      @NonNull String referenceKey,
      boolean computeRetainedSize) {
    long analysisStartNanoTime = System.nanoTime();

    if (!heapDumpFile.exists()) {
      Exception exception = new IllegalArgumentException("File does not exist: " + heapDumpFile);
      return failure(exception, since(analysisStartNanoTime));
    }

    try {
      listener.onProgressUpdate(READING_HEAP_DUMP_FILE);
      // 1
      HprofBuffer buffer = new MemoryMappedFileBuffer(heapDumpFile);
      // 2
      HprofParser parser = new HprofParser(buffer);
      listener.onProgressUpdate(PARSING_HEAP_DUMP);
      Snapshot snapshot = parser.parse();
      listener.onProgressUpdate(DEDUPLICATING_GC_ROOTS);
      // 3
      deduplicateGcRoots(snapshot);
      listener.onProgressUpdate(FINDING_LEAKING_REF);
      // 4
      Instance leakingRef = findLeakingReference(referenceKey, snapshot);

      // False alarm, weak reference was cleared in between key check and heap dump.
      // 5
      if (leakingRef == null) {
        String className = leakingRef.getClassObj().getClassName();
        return noLeak(className, since(analysisStartNanoTime));
      }
      // 6
      return findLeakTrace(analysisStartNanoTime, snapshot, leakingRef, computeRetainedSize);
    } catch (Throwable e) {
      return failure(e, since(analysisStartNanoTime));
    }
  }
```

这里的HeapAnalyzerService实质是一个类型为IntentService的ForegroundService，执行startForegroundService()之后，会回调onHandleIntentInForeground()方法。注释1处，首先会新建一个HeapAnalyzer对象，顾名思义，它就是根据RefWatcher生成的heap dumps信息来分析被怀疑的泄漏是否是真的。在注释2处，然后会调用它的checkForLeak()方法去使用haha库解析 hprof文件。

在注释1处，会新建一个内存映射缓存文件buffer。在注释2处，会使用buffer新建一个HprofParser解析器去解析出对应的引用内存快照文件snapshot。在注释3处，为了减少在Android 6.0版本中重复GCRoots带来的内存压力的影响，使用deduplicateGcRoots()删除了gcRoots中重复的根对象RootObj。在注释4处，调用了findLeakingReference()方法将传入的referenceKey和snapshot对象里面所有类实例的字段值对应的keyCandidate进行比较，如果没有相等的，则表示没有发生内存泄漏，直接调用注释5处的代码返回一个没有泄漏的分析结果AnalysisResult对象。如果找到了相等的，则表示发生了内存泄漏，执行注释6处的代码findLeakTrace()方法返回一个有泄漏分析结果的AnalysisResult对象。

最后，我们来分析下HeapAnalyzerService中注释3处的AbstractAnalysisResultService.sendResultToListener()方法，很明显，这里AbstractAnalysisResultService的实现类就是我们刚开始分析的用于展示泄漏路径信息得DisplayLeakService对象。在里面直接创建一个由PendingIntent构建的泄漏通知用于供用户点击去展示详细的泄漏界面DisplayLeakActivity。核心代码如下所示：

```
public class DisplayLeakService extends AbstractAnalysisResultService {
 @Override
 protected final void onHeapAnalyzed(@NonNull AnalyzedHeap analyzedHeap) {
 ...
 boolean resultSaved = false;
 boolean shouldSaveResult = result.leakFound || result.failure != null;
 if (shouldSaveResult) {
 heapDump = renameHeapdump(heapDump);
 // 1
 resultSaved = saveResult(heapDump, result);
 }
 if (!shouldSaveResult) {
 ...
 showNotification(null, contentTitle, contentText);
 } else if (resultSaved) {
 ...
 // 2
 PendingIntent pendingIntent =
 DisplayLeakActivity.createPendingIntent(this, heapDump.referenceKey);
 ...
 showNotification(pendingIntent, contentTitle, contentText);
 } else {
 onAnalysisResultFailure(getString(R.string.leak_canary_could_not_save_text));
 }
 ...
}
```
可以看到，只要当分析的堆信息文件保存成功之后，即在注释1处返回的resultSaved为true时，才会执行注释2处的逻辑，即创建一个供用户点击跳转到DisplayLeakActivity的延时通知。
