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