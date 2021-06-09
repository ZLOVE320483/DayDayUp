## Android插件化原理

插件化需要解决的问题和技术：

- Hook技术
- 插件的类加载
- 插件的资源加载
- 启动插件Activity

### Hook技术

如果我们自己创建代理对象，然后把原始对象替换为我们的代理对象（劫持原始对象），那么就可以在这个代理对象为所欲为了，修改参数，替换返回值，我们称之为 Hook。

我们可用用Hook技术来劫持原始对象，被劫持的对象叫做Hook点，什么样的对象比较容易Hook呢？当然是单例和静态对象,在一个进程内单例和静态对象不容易发生改变，用代理对象来替代Hook点，这样我们就可以在代理对象中实现自己想做的事情，我们这里Hook常用的startActivity方法来举例

对于 startActivity过程有两种方式：Context.startActivity 和 Activity.startActivity。这里暂不分析其中的区别，以 Activity.startActivity 为例说明整个过程的调用栈。

Activity 中的 startActivity 最终都是由 startActivityForResult 来实现的。

```
  @Override
    public void startActivity(Intent intent, @Nullable Bundle options) {
        if (options != null) {
            startActivityForResult(intent, -1, options);
        } else {
            // Note we want to go through this call for compatibility with
            // applications that may have overridden the method.
            startActivityForResult(intent, -1);
        }
    }
    
     public void startActivityForResult(@RequiresPermission Intent intent, int requestCode) {
        startActivityForResult(intent, requestCode, null);
    }
    
     public void startActivityForResult(@RequiresPermission Intent intent, int requestCode,
            @Nullable Bundle options) {
    ...
            //注释1
            Instrumentation.ActivityResult ar =
                mInstrumentation.execStartActivity(
                    this, mMainThread.getApplicationThread(), mToken, this,
                    intent, requestCode, options);
    ...
    }

```

我们看注释1处，调用了mInstrumentation.execStartActivity,来启动Activity，这个mInstrumentation是Activity成员变量，我们选择mInstrumentation作为Hook点。

- 首先先写出代理Instrumentation类

```
public class ProxyInstrumentation extends Instrumentation {

    private final Instrumentation instrumentation;

    public ProxyInstrumentation(Instrumentation instrumentation){
        this.instrumentation=instrumentation;
    }

    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options) {


        Log.d("mmm", "Hook成功，执行了startActivity"+who);

        Intent replaceIntent = new Intent(target, TextActivity.class);
        replaceIntent.putExtra(TextActivity.TARGET_COMPONENT, intent);
        intent = replaceIntent;

        try {
            Method execStartActivity = Instrumentation.class.getDeclaredMethod(
                    "execStartActivity",
                    Context.class,
                    IBinder.class,
                    IBinder.class,
                    Activity.class,
                    Intent.class,
                    int.class,
                    Bundle.class);
            return (ActivityResult) execStartActivity.invoke(instrumentation, who, contextThread, token, target, intent, requestCode, options);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
```

ProxyInstrumentation类继承Instrumentation,并包含原始Instrumentation的引用，实现了execStartActivity方法，其内部会打印log并且反射调用原始Instrumentation对象的execStartActivity方法。

接下来我们用ProxyInstrumentation类替换原始的Instrumentation,代码如下：

```
    public static void doInstrumentationHook(Activity activity){
        // 拿到原始的 mInstrumentation字段
        Field mInstrumentationField = null;
        try {
            mInstrumentationField = Activity.class.getDeclaredField("mInstrumentation");
            mInstrumentationField.setAccessible(true);

            // 创建代理对象
            Instrumentation originalInstrumentation = (Instrumentation) mInstrumentationField.get(activity);
            mInstrumentationField.set(activity, new ProxyInstrumentation(originalInstrumentation));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
```

然后再MainActivity中调用这个方法

```
  protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ProxyUtils.doInstrumentationHook(this);
    }
```