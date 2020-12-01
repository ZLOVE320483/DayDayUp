## ViewModel为什么能保存重建数据

打开官网，我们能看到关于ViewModel的如下描述：
> The ViewModelclass allows data to survive configuration changes such as screen rotations.

这句话的意思大概就是，当我们的activity发生配置改变（例如屏幕旋转）而销毁重建时，ViewModel类依然能是其中保存的数据存活。

于此同时，官网改给出了ViewModel的生命周期图：

![viewmodel生命周期图](https://github.com/ZLOVE320483/DayDayUp/blob/main/pic/viewmodel.jpg)

从上面的生命周期图可以很清楚的看到，ViewModel的生命周期，几乎是和Activity一致的。为了佐证官网上的说法，当手机屏幕发生旋转时，ViewModel对象实例依然存在，我写了一个小Demo
```
class ViewModelActivity: FragmentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewmodel)

        Log.d("ViewModelActivity", "onCreate --- hashcode --- ${hashCode()}")

        val mainViewModel= ViewModelProvider(this).get(MainViewModel::class.java)

        Log.d("ViewModelActivity", "onCreate --- $mainViewModel")
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d("ViewModelActivity", "onDestroy --- hashcode --- ${hashCode()}")
    }
}
```
当app运行和发生屏幕旋转时，控制台分别打印了如下内容：
```
2020-12-02 17:44:14.142 14793-14793/com.zlove.day.up D/ViewModelActivity: onCreate --- hashcode --- 75306670
2020-12-02 17:44:14.142 14793-14793/com.zlove.day.up D/ViewModelActivity: onCreate --- com.zlove.day.up.mvvm.MainViewModel@ebfdc4f
2020-12-02 17:44:19.321 14793-14793/com.zlove.day.up D/ViewModelActivity: onDestroy --- hashcode --- 75306670
2020-12-02 17:44:19.360 14793-14793/com.zlove.day.up D/ViewModelActivity: onCreate --- hashcode --- 89248538
2020-12-02 17:44:19.360 14793-14793/com.zlove.day.up D/ViewModelActivity: onCreate --- com.zlove.day.up.mvvm.MainViewModel@ebfdc4f
```
根据打印内容可以看到，Activity确实发生了销毁重建，而我们的ViewModel对象，也确实仍然是同一个内存地址，这就佐证了官网说法的正确性。

接下来就要开始探索一番，ViewModel是如何拥有这样的能力的。在进行源码分析之前，先来介绍一下ViewModel这个东西产生的背景。

### 背景
- 在发生配置改变时 Activity 和 Fragment 会被销毁重建，它们内部的临时性数据(不是通过 Intent 传入的数据)就会丢失. 如果把这些临时数据放到 ViewModel 中, 则可以避免数据的丢失。当然也可以利用 onSaveInstanceState 来保留临时数据，但是如果临时数据的量较大，onSaveInstanceState 由于涉及了跨进程通信，较大的数据量会造成 marshalling 和 unmashlling 消耗较大。而利用 ViewModel 其实是没有跨进程通信的消耗。但是它没有 onSaveInstanceState 提供的 Activity 被回收之后的数据恢复功能：在 Activity 位于后台时系统会在内存不足时将其回收，当 Activity 再次回到前台时，系统会把 onSaveInstanceState 中保存的数据通过 onRestoreInstanceState 和 onCreate 里的 savedInstanceState 参数传递给 Activity。
- ViewModel 顾名思义能知道它本质就是用来储存与视图相关的数据的，官方也是推荐将 Activity(Fragment) 中的数据及数据操作提取到 ViewModel 中，让视图的显示控制和数据分离。
- ViewModel 能感知 Activity或Fragment 的生命周期的改变，在 Activity或Fragment 销毁时执行一些数据清理工作(ViewModel 的实现类可以通过重写onCleared方法)。

为了避免源码分析的篇幅过长使读者产生拖沓的感觉，所以在这里先抛出源码分析过后的结论。
### 结论
- Activity(Fragment) 的 ViewModel 都存储在 ViewModelStore 中，每个 Activity(Fragment) 都会拥有一个 ViewModelStore 实例
- ViewModelProvider 负责向使用者提供访问某个 ViewModel 的接口，其内部会持有当前 Activity(Fragment) 的 ViewModelStore，然后将操作委托给 ViewModelStore 完成
- ViewModel 能在 Activity(Fragment) 在由于配置重建时恢复数据的实现原理是：Activity(指 support library 中的 ComponentActivity) 会将 ViewModelStore 在 Activity(Fragment) 重建之前交给 ActivityThread 中的 ActivityClientRecord 持有，待 Activity(Fragment) 重建完成之后，再从 ActivityClientRecord 中获取 ViewModelStore
- 如果应用的进程位于后台时，由于系统内存不足被销毁了。即使利用 ViewModel 的也不能在 Activity(Fragment) 重建时恢复数据。因为存储 ViewModel 的 ViewModelStore 是交给 ActivityThread 中的 ActivityClientRecord 暂存的，进程被回收了，ActivityThread 也就会被回收，ViewModelStore 也就被回收了，ViewModel 自然不复存在了

源码分析之前，先给自己抛两个问题：

1. ViewModel中涉及的类和数据结构有哪些

2. ViewModel如何保证在Activity或者Fragment因为配置改变发生重建时，ViewModel中的数据得以保存。

### 核心数据结构
#### ViewModel
ViewModel是一个抽象类，使用者需要继承它，ViewModel内部的变量和方法较少。
```
ViewModel.java

// 表示当前ViewModel对象是否被销毁
private volatile boolean mCleared = false;

// 子类可以通过复写这个方法，在ViewModel被销毁时最一些额外操作，比如资源的释放等等
protected void onCleared() {
}

@MainThread
final void clear() {
}
```
#### ViewModelStore
ViewModelStore 顾名思义，它是负责储存 ViewModel 的一个类。引用 ViewModelStore 代码注释中的一段话表示它的功能:
> ViewModelStore 的实例必须在发生配置更改时得以保留：如果此 ViewModelStore 的持有者由于配置的改变而被销毁并重新创建，那么持有者的新实例应该具有相同的 ViewModelStore 旧实例。
```
ViewModelStore.java

// mMap 是ViewModelStore 中有且仅有的成员变量，看它的泛型类型参数就能明白，它就是 ViewModelStore 用来存储 ViewModel 的池子。
private final HashMap<String, ViewModel> mMap = new HashMap<>();

// 向 ViewModelStore 的池子中存入 ViewModel, 如果池子中已经有 key 对应 ViewModel了，旧的会被新的替换，而且会调用旧的 ViewModel 的 onCleared 方法。
final void put(String key, ViewModel viewModel) {
    ViewModel oldViewModel = mMap.put(key, viewModel);
    if (oldViewModel != null) {
        oldViewModel.onCleared();
    }
}

// 从池子(mMap)中获取 key 对应的 ViewModel
final ViewModel get(String key) {
   return mMap.get(key);
}

// 返回 mMap 的所有 key
Set<String> keys() {
  return new HashSet<>(mMap.keySet());
}

// 清空 mMap 中的所有 ViewModel，并调用每一个的 clear 方法
public final void clear() {
    for (ViewModel vm : mMap.values()) {
       vm.clear();
    }
    mMap.clear();
}
```
这里注意下 ViewModelStore 的 get 和 put 方法的声明，访问权限都是包层级的，也就表示我们使用者是无法直接通过 ViewModelStore 通过 key 拿到对应的 ViewModel 的。

#### ViewModelProvider
> 一个为范围(Activity, Fragment)提供 ViewModels 的实用工具类

```
ViewModelProvider.java

// 以下是 ViewModelProvider.java 中的成员变量

// Factory 表示创建 ViewModel 的工厂，final 的声明表示它必须在 ViewModelProvider 的构造函数中就赋值。
private final Factory mFactory;

// final 的声明表示 mViewModelStore 也必须在构造函数中就赋值。
private final ViewModelStore mViewModelStore;

// DEFAULT_KEY，用来在提交 ViewModel 到 ViewModelStore 时构造 key
private static final String DEFAULT_KEY =
            "androidx.lifecycle.ViewModelProvider.DefaultKey";

public interface Factory {
	
	@NonNull
	<T extends ViewModel> T create(@NonNull Class<T> modelClass);
}
```
```
ViewModelProvider.java

// 以下是 ViewModelProvider.java 中的方法

// 构造方法
    public ViewModelProvider(@NonNull ViewModelStoreOwner owner) {
        this(owner.getViewModelStore(), owner instanceof HasDefaultViewModelProviderFactory
                ? ((HasDefaultViewModelProviderFactory) owner).getDefaultViewModelProviderFactory()
                : NewInstanceFactory.getInstance());
    }

    public ViewModelProvider(@NonNull ViewModelStoreOwner owner, @NonNull Factory factory) {
        this(owner.getViewModelStore(), factory);
    }

    public ViewModelProvider(@NonNull ViewModelStore store, @NonNull Factory factory) {
        mFactory = factory;
        mViewModelStore = store;
    }

// 以下两个方法就是获取 ViewModel 的方法，也是 ViewModel 库向使用者提供的两个公开的接口。第一个 get 方法会利用 DEFAULT_KEY 构造一个 key，然后调用第二个 get 方法。
    @NonNull
    @MainThread
    public <T extends ViewModel> T get(@NonNull Class<T> modelClass) {
        String canonicalName = modelClass.getCanonicalName();
        if (canonicalName == null) {
            throw new IllegalArgumentException("Local and anonymous classes can not be ViewModels");
        }
        return get(DEFAULT_KEY + ":" + canonicalName, modelClass);
    }

    @NonNull
    @MainThread
    public <T extends ViewModel> T get(@NonNull String key, @NonNull Class<T> modelClass) {
        ViewModel viewModel = mViewModelStore.get(key);

        if (modelClass.isInstance(viewModel)) {
            if (mFactory instanceof OnRequeryFactory) {
                ((OnRequeryFactory) mFactory).onRequery(viewModel);
            }
            return (T) viewModel;
        } else {
            //noinspection StatementWithEmptyBody
            if (viewModel != null) {
                // TODO: log a warning.
            }
        }
        if (mFactory instanceof KeyedFactory) {
            viewModel = ((KeyedFactory) (mFactory)).create(key, modelClass);
        } else {
            viewModel = (mFactory).create(modelClass);
        }
        mViewModelStore.put(key, viewModel);
        return (T) viewModel;
    }

```
#### ViewModelStoreOwner
ViewModelStoreOwner 是一个接口，它声明了一个 getViewModelStore 方法需要实现类实现。
> 实现此接口的类的职责是保留其拥有的 ViewModelStore 在配置更改期间不会被销毁。

```
public interface ViewModelStoreOwner {

    @NonNull
    ViewModelStore getViewModelStore();
}
```
#### ComponentActivity

ComponentActivity 是 androidx.activity 包中增加的，FragmentActivity 继承自它。
```
public class ComponentActivity extends androidx.core.app.ComponentActivity implements
        LifecycleOwner,
        ViewModelStoreOwner,
        HasDefaultViewModelProviderFactory,
        SavedStateRegistryOwner,
        OnBackPressedDispatcherOwner {

// ComponentActivity 中与 ViewModel 有关的两个变量:
	
//表示当前 Activity 的 ViewModelStore, 一个 Activity 拥有一个 ViewModelStore
    private ViewModelStore mViewModelStore;
// 创建 ViewModel 的工厂实现，ComponentActivity 中使用的是 SavedStateViewModelFactory, SavedStateViewModelFactory 与 Activity 的重建相关。
    private ViewModelProvider.Factory mDefaultFactory;

    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        if (getApplication() == null) {
            throw new IllegalStateException("Your activity is not yet attached to the "
                    + "Application instance. You can't request ViewModel before onCreate call.");
        }
        if (mViewModelStore == null) {
		// 与 Activity 重建时恢复数据有关，后面会再详谈
            NonConfigurationInstances nc =
                    (NonConfigurationInstances) getLastNonConfigurationInstance();
            if (nc != null) {
                // Restore the ViewModelStore from NonConfigurationInstances
                mViewModelStore = nc.viewModelStore;
            }
            if (mViewModelStore == null) {
		// 创建 ViewModelStore, 赋值给 ComponentActivity 的 mViewModelStore 变量
                mViewModelStore = new ViewModelStore();
            }
        }
        return mViewModelStore;
    }
}
```
总的来说 ViewModelStore 就是 ComponentActivity 里的一个成员字段，且只会创建一次。

再看 ViewModelProvider，它其实只是一个用来访问 ViewModelStore 的门面，它内部没有存任何数据。所以每次要获取 ViewModel 时都是创建一个新的 ViewModelProvider 实例。

### ViewModel 的重建保留
ViewModel 要实现和 onSaveInstanceState 方法同样的功效，那它就需要在 Activity(或Fragment) 销毁时保留内部的数据，待 Activity 重建时恢复数据。

前面提到过，Jetpack 中的库都是基于现有 SDK 实现进行的封装，不会修改 SDK 已有的实现。ViewModel 要实现重建保留的功能，肯定需要一个时机来做保留的动作。我们知道 SDK 已有的实现中，一个 Activity 在因为配置改变而要销毁重建时一定会调用的一个方法就是onSaveInstanceState，所以先去检查下 ComponentActivity 在这个方法中有没有动什么手脚。
```
protected void onSaveInstanceState(@NonNull Bundle outState) {
    Lifecycle lifecycle = getLifecycle();
    if (lifecycle instanceof LifecycleRegistry) {
        ((LifecycleRegistry) lifecycle).setCurrentState(Lifecycle.State.CREATED);
    }
    super.onSaveInstanceState(outState);
    mSavedStateRegistryController.performSave(outState);
}
```
发现没有与 ViewModel 相关的，于是这条路不通了。无路可走时造路走，最后大法 Find Usage，找出使用到 mViewModelStore 的地方，看看有什么特殊没有。

发现除了 getViewModelStore，另外一个使用的方法是 onRetainNonConfigurationInstance() ,继续查找这个方法的使用方。

Activity 的 retainNonConfigurationInstances 方法调用了子类的 onRetainNonConfigurationInstance() 方法:
```
NonConfigurationInstances retainNonConfigurationInstances() {
    //需要 Activity 子类实现的 onRetainNonConfigurationInstance 方法
    Object activity = onRetainNonConfigurationInstance();

    NonConfigurationInstances nci = new NonConfigurationInstances();
    nci.activity = activity;
    nci.children = children;
    nci.fragments = fragments;
    nci.loaders = loaders;
    if (mVoiceInteractor != null) {
        mVoiceInteractor.retainInstance();
        nci.voiceInteractor = mVoiceInteractor;
    }
    return nci;
}
```
在查找 Activity 的 retaionNonConfigurationInstances 方法的调用者，Android Studio 显示找不到了。因为它的调用者是 ActivityThread(@hide 注释的)，那就进入 ActivityThread 搜索看看。
在 ActivityThread 的 performDestroyActivity 找到了调用：
```
ActivityClientRecord performDestroyActivity(IBinder token, boolean finishing, int configChanges, boolean getNonConfigInstance, String reason) {
    // 从 mActivities 中获取 token 对应的 ActivityClientRecord
    ActivityClientRecord r = mActivities.get(token);
    Class<? extends Activity> activityClass = null;
    if (r != null) {
        //...
        // 如果参数 getNonConfigInstance 为 true
        if (getNonConfigInstance) {
            try {
                // 将 ViewModelStore 存入 ActivityClientRecord 的 lastNonConfiguratinInstances 字段中
                r.lastNonConfigurationInstances = r.activity.retainNonConfigurationInstances();
            } catch (Exception e) {}
        }
        //...
    }
    mActivities.remove(token);
    return r;
}
```
那么这个 getNonConfigInstance 参数是在哪里赋值为 true 的呢?
