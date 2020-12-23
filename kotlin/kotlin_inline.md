## Kotlin内联函数

kotlin的内联函数是kotlin的高级特性，,也是与java的区别之一，为什么kotlin需要内联函数呢？

### 方法的调用流程

调用一个方法其实就是一个方法压栈和出栈的过程，调用方法时将栈帧压入方法栈，然后执行方法体，方法结束时将栈帧出栈，这个压栈和出栈的过程是一个耗费资源的过程，这个过程中传递形参也会耗费资源。

#### 为什么要使用内联函数inline

我们在写代码的时候难免会遇到这种情况，就是很多处的代码是一样的，于是乎我们就会抽取出一个公共方法来进行调用，这样看起来就会很简洁，但是也出现了一个问题，就是这个方法会被频繁调用，就会很耗费资源。

举个例子：
```
    fun <T>method(lock: Lock, body: () ->T): T {
        lock.lock()
        try {
            return body()
        } finally {
            lock.unlock()
        }
    }
```

这里的method方法在调用的时候是不会把形参传递给其他方法的,调用一下:
```
    fun test() {
        val lock = ReentrantLock()
        method(lock) {"我是body"}
    }
```

对于编译器来说，调用method方法就要将参数lock和lambda表达式{"我是body的方法体"}进行传递，就要将method方法进行压栈出栈处理，这个过程就会耗费资源。如果是很多地方调用,就会执行很多次,这样就非常消耗资源了。

于是乎就引进了inline

#### inline
被inline标记的函数就是内联函数,其原理就是:在编译时期,把调用这个函数的地方用这个函数的方法体进行替换
举个栗子:
我们调用上面的method方法：

``` method(lock) {"我是body"} ```

其实上面调用的方法,在编译时期就会把下面的内容替换到调用该方法的地方,这样就会减少方法压栈,出栈,进而减少资源消耗：

```
   lock.lock()
   try {
         return "我是body"
      } finally {
         lock.unlock()
      }
```

也就是说inline关键字实际上增加了代码量，但是提升了性能，而且增加的代码量是在编译期执行的，对程序可读性不会造成影响,可以说是非常的nice。

#### noline

虽然内联非常好用,但是会出现这么一个问题,就是内联函数的参数(ps:参数是函数,比如上面的body函数)如果在内联函数的方法体内被其他非内联函数调用,就会报错.
举个栗子:
```
    inline fun <T>method(lock: Lock, body: () ->T): T {
        lock.lock()
        try {
            otherMethod(body)
            return body()
        } finally {
            lock.unlock()
        }
    }

    fun <T> otherMethod(body: () -> T) {

    }
```
原因:因为method是内联函数,所以它的形参也是inline的,所以body就是inline的,但是在编译时期,body已经不是一个函数对象,而是一个具体的值,然而otherMehtod却要接收一个body的函数对象,所以就编译不通过了。

解决方法:当然就是加noinline了,它的作用就已经非常明显了.就是让内联函数的形参函数不是内联的,保留原有的函数特征。

具体操作:
```
    inline fun <T>method(lock: Lock, noinline body: () ->T): T {
        lock.lock()
        try {
            otherMethod(body)
            return body()
        } finally {
            lock.unlock()
        }
    }

    fun <T> otherMethod(body: () -> T) {

    }
```
这样编译时期这个body函数就不会被内联了。

#### crossinline

什么是crossinline呢,crossinline 的作用是内联函数中让被标记为crossinline 的lambda表达式不允许非局部返回。
怎么理解呢?
首先我们来看下非局部返回
我们都知道,kotlin中,如果一个函数中,存在一个lambda表达式，在该lambda中不支持直接通过return退出该函数的,只能通过return@XXXinterface这种方式

举个栗子:
```
fun outterFun() {
    innerFun {
        //return  //错误，不支持直接return
        //只支持通过标签，返回innerFun
        return@innerFun 1
    }

    //如果是匿名或者具名函数，则支持
    var f = fun(){
        return
    }
}

fun innerFun(a: () -> Int) {}
```

但是如果这个函数是内联的却是可以的

```
fun outterFun() {
    innerFun {
        return  //支持直接返回outterFun       
    }
}

inline fun innerFun(a: () -> Int) {}
```

这种直接从lambda中return掉函数的方法就是非局部返回,crossinline就是为了让其不能直接return

举个栗子
```
fun outterFun() {
    innerFun {
        return  //这样就报错了
    }
}

inline fun innerFun( crossinline a: () -> Int) {}
```

这里的a函数就是被crossinline修饰了,如果在lambda中直接return就无法编译通过;

官方给出的解释:

>一些内联函数可能调用传给它们的不是直接来自函数体、而是来自另一个执行上下文的 lambda 表达式参数，例如来自局部对象或嵌套函数。在这种情况下，该 lambda 表达式中也不允许非局部控制流。为了标识这种情况，该 lambda 表达式参数需要用 crossinline 修饰符标记。

说白了，我们如果直接在lambda参数中结束当前函数，而不给lambda提供一个返回值，这种情况是不被允许的。当然这个使用的机会并不多,但是有的时候还是会用到。

举个栗子：
```
fun main(args: Array<String>) {
    //正常
    method{
        1
    }
    //return报错
    method{
        return 
    }
}
interface TestInter{
    fun test(a:Int):Int
}
inline fun method(crossinline t: (Int) -> Int): TestInter = object : TestInter {
    override fun test(a: Int): Int = t.invoke(a)
}
```
这里如果不通过crossinline禁止lambda表达式t直接执行的return操作，那么t直接return后，返回值是Unit，这并不符合fun test(a: Int): Int 需要Int返回值的要求,就返回了一个Unit,这样是不符合需求的。

#### reified
什么是reified,字面意思:具体化,其实就是具体化泛型;
我们都知道在java中如果是泛型,是不能直接使用泛型的类型的,但是kotlin却是可以的,这点和java就有了显著的区别。通常java中解决的方案就是通过函数来传递类，但是kotlin就老牛逼了,直接就可以用了,主要还是有内联函数inline这个好东西,才使得kotlin能够直接通过泛型就能拿到泛型的类型。

举个栗子:
```
 inline fun <reified T : Activity> Activity.startActivity() {
     startActivity(Intent(this, T::class.java))
}
```

通过kotlin的拓展写个启动activity的方法,只需要传入该activity的泛型即可。

再来看看一个需求,有的时候我们需要创建一个Fragment的实例,并且要传递参数,如果是之前你可能会在每个Fragment里面这样写:
```
 fun newInstance(param: Int): ActyFragment {
            val fragment = ActyFragment()
            val args = Bundle()
            args.putInt(PARAMS, param)
            fragment.arguments = args
            return fragment
        }
```

但是这样是不是很low,只要需要的Fragment都要写个这个,有强迫症的人是受不了的现在通过reified来优化：
```
inline fun <reified F : Fragment> Context.newFragment(vararg args: Pair<String, String>): F {
    val bundle = Bundle()
    args.let {
        for (arg in args) {
            bundle.putString(arg.first, arg.second)
        }
    }
    return Fragment.instantiate(this, F::class.java.name, bundle) as F
}
```
这样就不要每个Fragment都写个方法了,可以说非常的nice了。