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
