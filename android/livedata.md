## LiveData 使用 & 解析
### 为什么要引进LiveData
> LiveData 是一个可以被观察的数据持有类，它可以感知 Activity、Fragment或Service 等组件的生命周期。简单来说，他主要有以下优点。

1. 它可以做到在组件处于激活状态的时候才会回调相应的方法，从而刷新相应的 UI。
2. 不用担心发生内存泄漏。
3. 当 配置改变 导致 activity 重新创建的时候，不需要手动取处理数据的储存和恢复。它已经帮我们封装好了。
4. 当 Actiivty 不是处于激活状态的时候，如果你想 livedata setValue 之后立即回调 obsever 的 onChange 方法，而不是等到 Activity 处于激活状态的时候才回调 obsever 的 onChange 方法，你可以使用 observeForever 方法，但是你必须在 onDestroy 的时候 removeObserver。

### LiveData使用
```
data class User(val userName: String, val userAge: Int) {

    override fun toString(): String {
        return "userName: $userName, userAge: $userAge"
    }
}
```
```
class UserViewModel : ViewModel() {

    val mUserLiveData = MutableLiveData<User>()

}
```
```
class LiveDataActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_livedata)

        val userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        userViewModel.mUserLiveData.observe(this, Observer<User> { user ->
            user_info.text = user?.toString()
        })

        post_value.setOnClickListener {
            userViewModel.mUserLiveData.postValue(User("zlove", 30))
        }
    }
}
```
LiveData的使用比较简单，不做过多的赘述。值得注意的是，在改变LiveData数据的时候有 setValue 和 postValue 两种方法，两者之间的区是，调用 setValue 方法，Observer 的 onChanged 方法会在调用 serValue 方法的线程回调。而postvalue 方法，Observer 的 onChanged 方法将会在主线程回调。

### 自定义LiveData
LiveData主要有以下几个方法
- observe
- onActive
- onActive
- observeForever

``` void observe(LifecycleOwner owner, Observer observer) ```
> Adds the given observer to the observers list within the lifespan of the given owner. The events are dispatched on the main thread. If LiveData already has data set, it will be delivered to the observer.

``` void onActive() ```
> Called when the number of active observers change to 1 from 0.This callback can be used to know that this LiveData is being used thus should be kept up to date.

当回调该方法的时候，表示该 liveData 正在背使用，因此应该保持最新。

``` void onInactive() ```

> Called when the number of active observers change from 1 to 0.This does not mean that there are no observers left, there may still be observers but their lifecycle states aren’t STARTED or RESUMED (like an Activity in the back stack).You can check if there are observers via hasObservers().

当该方法回调时，表示他所有的 obervers 没有一个状态处理 STARTED 或者 RESUMED，注意，这不代表没有 observers。

``` void observeForever() ```

跟 observe 方法不太一样的是，它在 Activity 处于 onPause ，onStop， onDestroy 的时候，都可以回调 obsever 的 onChange 方法，但是有一点需要注意的是，我们必须手动 remove obsever，否则会发生内存泄漏。

#### 自定义LiveData Demo
结下来以监听网络状态变化为例子，讲解一下如何使用自定义LiveData。

自定义一个Class NetworkLiveData，继承LiveData，重写 onActive 和 onInactive 方法

在 onActive 方法中注册监听网络变化的receiver，在 onInactive 中注销监听
```
class NetworkLiveData private constructor(context: Context): LiveData<NetworkInfo>() {

    private val mContext: Context = context.applicationContext
    private val mNetworkReceiver = NetworkReceiver()
    private val mIntentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)

    companion object {
        var mNetworkInfo: NetworkInfo? = null

        @Volatile
        private var instance: NetworkLiveData? = null

        fun getInstance(context: Context): NetworkLiveData {
            return instance?: synchronized(this) {
                instance?: NetworkLiveData(context).also { instance = it }
            }
        }
    }

    override fun onActive() {
        super.onActive()
        Log.d("NetworkLiveData", "--- onActive ---")
        mContext.registerReceiver(mNetworkReceiver, mIntentFilter)
    }

    override fun onInactive() {
        super.onInactive()
        Log.d("NetworkLiveData", "--- onInactive ---")
        mContext.unregisterReceiver(mNetworkReceiver)
    }



    class NetworkReceiver: BroadcastReceiver() {

        override fun onReceive(context: Context?, p1: Intent?) {
            context?.run {
                val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork = manager.activeNetworkInfo
                getInstance(this).setValue(activeNetwork)
            }
        }
    }
}
```
这样，当我们想监听网络变化的时候，我们只需要调用相应的 observe 方法即可，方便又快捷。

```
	NetworkLiveData.getInstance(this).observe(this, Observer {
            Log.d("NetworkLiveData", " NetworkInfo --- $it")
        })
```
### 共享数据
#### Fragment Activity 之间共享数据

我们回过头来再来看一下 ViewModelProvider 的 of 方法，他主要有四个方法，分别是

```ViewModelProvider of(@NonNull Fragment fragment)```

```ViewModelProvider of(@NonNull FragmentActivity activity)```

```ViewModelProvider of(@NonNull Fragment fragment, @Nullable Factory factory)```

```ViewModelProvider of(@NonNull FragmentActivity activity, @Nullable Factory factory)```

方法之间的主要区别是传入 Fragment 或者 FragmentActivity。而我们知道，通过 ViewModel of 方法创建的 ViewModel 实例， 对于同一个 fragment 或者 fragmentActivity 实例，ViewModel 实例是相同的，因而我们可以利用该特点，在 Fragment 中创建 ViewModel 的时候，传入的是 Fragment 所依附的 Activity。因而他们的 ViewModel 实例是相同的，从而可以做到共享数据。

#### 全局共享数据
说到全局共享数据，我们想一下我们的应用全景，比如说我的账户数据，这个对于整个 App 来说，肯定是全局共享的。有时候，当我们的数据变化的时候，我们需要通知我们相应的界面，刷新 UI。如果用传统的方式来实现，那么我们一般才采取观察者的方式来实现，这样，当我们需要观察数据的时候，我们需要添加 observer，在界面销毁的时候，我们需要移除 observer。

但是，如果我们用 LiveData 来实现的话，它内部逻辑都帮我们封装好了，我们只需要保证 AccountLiveData 是单例的就ok，在需要观察的地方调用 observer 方法即可。也不需要手动移除 observer，不会发生内存泄漏，方便快捷。

### 小结
> LiveData 内部已经实现了观察者模式，如果你的数据要同时通知几个界面，可以采取这种方式。

> 我们知道 LiveData 数据变化的时候，会回调 Observer 的 onChange 方法，但是回调的前提是 lifecycleOwner（即所依附的 Activity 或者 Fragment） 处于 started 或者 resumed 状态，它才会回调，否则，必须等到 lifecycleOwner 切换到前台的时候，才回调。因此，这对性能方面确实是一个不小的提升。

### 原理解析
我们知道 livedata 的使用很简单，它是采用观察者模式实现的。
```
LiveData.java

    @MainThread
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        assertMainThread("observe");
        if (owner.getLifecycle().getCurrentState() == DESTROYED) {
            // ignore
            return;
        }
        LifecycleBoundObserver wrapper = new LifecycleBoundObserver(owner, observer);
        ObserverWrapper existing = mObservers.putIfAbsent(observer, wrapper);
        if (existing != null && !existing.isAttachedTo(owner)) {
            throw new IllegalArgumentException("Cannot add the same observer"
                    + " with different lifecycles");
        }
        if (existing != null) {
            return;
        }
        owner.getLifecycle().addObserver(wrapper);
    }
```
observe 方法，总结起来就是：

1. 判断是否已经销毁，如果销毁，直接移除

2. 用 LifecycleBoundObserver 包装传递进来的 observer

3. 是否已经添加过，添加过，直接返回

4. 将包装后的 LifecycleBoundObserver 添加进去

#### LifecycleBoundObserver
```
    class LifecycleBoundObserver extends ObserverWrapper implements LifecycleEventObserver {
        @NonNull
        final LifecycleOwner mOwner;

        LifecycleBoundObserver(@NonNull LifecycleOwner owner, Observer<? super T> observer) {
            super(observer);
            mOwner = owner;
        }

        @Override
        boolean shouldBeActive() {
            return mOwner.getLifecycle().getCurrentState().isAtLeast(STARTED);
        }

        @Override
        public void onStateChanged(@NonNull LifecycleOwner source,
                @NonNull Lifecycle.Event event) {
            if (mOwner.getLifecycle().getCurrentState() == DESTROYED) {
                removeObserver(mObserver);
                return;
            }
            activeStateChanged(shouldBeActive());
        }

        @Override
        boolean isAttachedTo(LifecycleOwner owner) {
            return mOwner == owner;
        }

        @Override
        void detachObserver() {
            mOwner.getLifecycle().removeObserver(this);
        }
    }
```
我们来看一下 LifecycleBoundObserver，继承 ObserverWrapper，实现了 LifecycleEventObserver 接口。而 LifecycleEventObserver 接口又实现了 LifecycleObserver 接口。 它包装了我们外部的 observer，有点类似于代理模式。

Activity 回调周期变化的时候，会回调 onStateChanged ，会先判断 mOwner.getLifecycle().getCurrentState() 是否已经 destroy 了，如果。已经 destroy，直接移除观察者。这也就是为什么我们不需要手动 remove observer 的原因。

如果不是销毁状态，会调用 activeStateChanged 方法 ，携带的参数为 shouldBeActive() 返回的值。
而当 lifecycle 的 state 为 started 或者 resume 的时候，shouldBeActive 方法的返回值为 true，即表示激活。

```
    private abstract class ObserverWrapper {
        final Observer<? super T> mObserver;
        boolean mActive;
        int mLastVersion = START_VERSION;

        ObserverWrapper(Observer<? super T> observer) {
            mObserver = observer;
        }

        abstract boolean shouldBeActive();

        boolean isAttachedTo(LifecycleOwner owner) {
            return false;
        }

        void detachObserver() {
        }

        void activeStateChanged(boolean newActive) {
            if (newActive == mActive) {
                return;
            }
            // immediately set active state, so we'd never dispatch anything to inactive
            // owner
            mActive = newActive;
            boolean wasInactive = LiveData.this.mActiveCount == 0;
            LiveData.this.mActiveCount += mActive ? 1 : -1;
            if (wasInactive && mActive) {
                onActive();
            }
            if (LiveData.this.mActiveCount == 0 && !mActive) {
                onInactive();
            }
            if (mActive) {
                dispatchingValue(this);
            }
        }
    }
```
activeStateChanged 方法中，，当 newActive 为 true，并且不等于上一次的值，会增加 LiveData 的 mActiveCount 计数。接着可以看到，onActive 会在 mActiveCount 为 1 时触发，onInactive 方法则只会在 mActiveCount 为 0 时触发。即回调 onActive 方法的时候活跃的 observer 恰好为 1，回调 onInactive 方法的时候，没有一个 Observer 处于激活状态。

当 mActive 为 true 时，会促发 dispatchingValue 方法。

```
    void dispatchingValue(@Nullable ObserverWrapper initiator) {
        if (mDispatchingValue) {
            mDispatchInvalidated = true;
            return;
        }
        mDispatchingValue = true;
        do {
            mDispatchInvalidated = false;
            if (initiator != null) {
                considerNotify(initiator);
                initiator = null;
            } else {
                for (Iterator<Map.Entry<Observer<? super T>, ObserverWrapper>> iterator =
                        mObservers.iteratorWithAdditions(); iterator.hasNext(); ) {
                    considerNotify(iterator.next().getValue());
                    if (mDispatchInvalidated) {
                        break;
                    }
                }
            }
        } while (mDispatchInvalidated);
        mDispatchingValue = false;
    }
```
其中 mDispatchingValue, mDispatchInvalidated 只在 dispatchingValue 方法中使用，显然这两个变量是为了防止重复分发相同的内容。当 initiator 不为 null，只处理当前 observer，为 null 的时候，遍历所有的 obsever，进行分发。

```
    private void considerNotify(ObserverWrapper observer) {
        if (!observer.mActive) {
            return;
        }
        // Check latest state b4 dispatch. Maybe it changed state but we didn't get the event yet.
        //
        // we still first check observer.active to keep it as the entrance for events. So even if
        // the observer moved to an active state, if we've not received that event, we better not
        // notify for a more predictable notification order.
        if (!observer.shouldBeActive()) {
            observer.activeStateChanged(false);
            return;
        }
        if (observer.mLastVersion >= mVersion) {
            return;
        }
        observer.mLastVersion = mVersion;
        observer.mObserver.onChanged((T) mData);
    }
```
如果状态不是在活跃中，直接返回，这也就是为什么当我们的 Activity 处于 onPause， onStop， onDestroy 的时候，不会回调 observer 的 onChange 方法的原因。

判断数据是否是最新，如果是最新，返回，不处理

数据不是最新，回调 mObserver.onChanged 方法。并将 mData 传递过去

```
    @MainThread
    protected void setValue(T value) {
        assertMainThread("setValue");
        mVersion++;
        mData = value;
        dispatchingValue(null);
    }
```
setValue 方法中，首先，断言是主线程，接着 mVersion + 1; 并将 value 赋值给 mData，接着调用 dispatchingValue 方法。dispatchingValue 传递 null，代表处理所有 的 observer。

这个时候如果我们依附的 activity 处于 onPause 或者 onStop 的时候，虽然在 dispatchingValue 方法中直接返回，不会调用 observer 的 onChange 方法。但是当所依附的 activity 重新回到前台的时候，会促发 LifecycleBoundObserver onStateChange 方法，onStateChange 又会调用 dispatchingValue 方法，在该方法中，因为 mLastVersion < mVersion，所以会回调 obsever 的 onChange 方法，这也就是 LiveData 设计得比较巧妙的一个地方

同理，当 activity 处于后台的时候，您多次调用 livedata 的 setValue 方法，最终只会回调 livedata observer 的 onChange 方法一次。
```
    protected void postValue(T value) {
        boolean postTask;
        synchronized (mDataLock) {
            postTask = mPendingData == NOT_SET;
            mPendingData = value;
        }
        if (!postTask) {
            return;
        }
        ArchTaskExecutor.getInstance().postToMainThread(mPostValueRunnable);
    }

    private final Runnable mPostValueRunnable = new Runnable() {
        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            Object newValue;
            synchronized (mDataLock) {
                newValue = mPendingData;
                mPendingData = NOT_SET;
            }
            setValue((T) newValue);
        }
    };
```
首先，采用同步机制，通过 postTask = mPendingData == NOT_SET 有没有人在处理任务。 true，没人在处理任务， false ，有人在处理任务，有人在处理任务的话，直接返回

调用 AppToolkitTaskExecutor.getInstance().postToMainThread 到主线程执行 mPostValueRunnable 任务。

```
    @MainThread
    public void observeForever(@NonNull Observer<? super T> observer) {
        assertMainThread("observeForever");
        AlwaysActiveObserver wrapper = new AlwaysActiveObserver(observer);
        ObserverWrapper existing = mObservers.putIfAbsent(observer, wrapper);
        if (existing instanceof LiveData.LifecycleBoundObserver) {
            throw new IllegalArgumentException("Cannot add the same observer"
                    + " with different lifecycles");
        }
        if (existing != null) {
            return;
        }
        wrapper.activeStateChanged(true);
    }

    private class AlwaysActiveObserver extends ObserverWrapper {

        AlwaysActiveObserver(Observer<? super T> observer) {
            super(observer);
        }

        @Override
        boolean shouldBeActive() {
            return true;
        }
    }
```
因为 AlwaysActiveObserver 没有实现 LifecycleEventObserver 方法接口，所以在 Activity o生命周期变化的时候，不会回调 onStateChange 方法。从而也不会主动 remove 掉 observer。因为我们的 obsever 被 remove 掉是依赖于 Activity 生命周期变化的时候，回调 LifecycleEventObserver 的 onStateChange 方法。

### 总结
- liveData 当我们 addObserver 的时候，会用 LifecycleBoundObserver 包装 observer，而 LifecycleBoundObserver 可以感应生命周期，当 activity 生命周期变化的时候，如果不是处于激活状态，判断是否需要 remove 生命周期，需要 remove，不需要，直接返回

- 当处于激活状态的时候，会判断是不是 mVersion最新版本，不是的话需要将上一次缓存的数据通知相应的 observer，并将 mLastVsersion 置为最新

- 当我们调用 setValue 的时候，mVersion +1，如果处于激活状态，直接处理，如果不是处理激活状态，返回，等到下次处于激活状态的时候，在进行相应的处理

- 如果你想 livedata setValue 之后立即回调数据，而不是等到生命周期变化的时候才回调数据，你可以使用 observeForever 方法，但是你必须在 onDestroy 的时候 removeObserver。因为 AlwaysActiveObserver 没有实现 LifecycleEventObserver 接口，不能感应生命周期。
