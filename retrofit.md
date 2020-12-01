# Retrofit源码解析

#### 在学习retrofit源码过程中需要着重注意一下几点
- 什么是动态代理
- 整个请求流程是如何进行的
- 底层是如何用OkHttp进行请求的
- 方法上的注解是什么时候进行解析的，是如何解析的
- Convert的转换是如何进行的
- CallAdapter的替换过程是如何进行的

以上问题的答案，将在这次的源码解析中逐一获得。学习完本次的源码解析，你一定会理解为什么 Retrofit+OkHttp 会成为目前最广泛使用的网络请求库，你一定会感叹retrofit设计之精妙。

#### Retrofit基本用法
首先先来看一下 retrofit 是如何使用的

1. 添加如下依赖：
```
implementation 'com.squareup.okhttp3:okhttp:4.8.1'
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.7.0'
implementation 'com.squareup.retrofit2:adapter-rxjava2:2.7.2'
implementation 'com.google.code.gson:gson:2.8.6'
implementation 'io.reactivex.rxjava3:rxjava:3.0.6'
implementation 'io.reactivex.rxjava2:rxandroid:2.1.1'
```
2. retrofit 定义

Retrofit：A type-safe HTTP client for Android and Java。一个类型安全的 Http 请求的客户端。底层的网络请求是基于 OkHttp 的，Retrofit 对其做了封装，提供了即方便又高效的网络访问框架。

3. retrofit 基本用法
```
class RetrofitActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_retrofit)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(GitHubApiService::class.java)
        val repos = service.listRepos("ZLOVE32048")

        repos.enqueue(object : Callback<List<Any>> {

            override fun onFailure(call: Call<List<Any>>, t: Throwable) {
                t.printStackTrace()
            }

            override fun onResponse(call: Call<List<Any>>, response: Response<List<Any>>) {
                Log.d("RetrofitActivity", "response.code() = ${response.code()}")
            }

        })
    }
}
```
```
interface GitHubApiService {
    @GET("user/{user}/repos")
    fun listRepos(@Path("user") user: String?): Call<List<Any>>
}
```
以上是retrofit的基本用法，只是用来作为源码分析的入口。

4. 源码分析之前

直观的感受一下上面的代码：
- Retrofit：一个总览全局的类，通过构建者模式（Builder）配置一些参数，例如baseUrl，CallAdapter，ConvertFactory等
- GitHubApiService：自己创建的API请求接口类，通过Retrofit的create方法创建真正的实例
- CallAdapterFactory：根据方法名想来是用于设置真正发送请求的Call对象
- ConverterFactory：根据方法名想来是用于设置解析返回结果的convert对象
- Callback：请求回调

5. 源码分析

分析的入口在上述代码例子中的 ``` repos.enqueue(object : Callback<List<Any>> {...} ```方法，点进去是一个接口类Call的 enqueue 方法
```
public interface Call<T> extends Cloneable {
	/**
   * Asynchronously send the request and notify {@code callback} of its response or if an error
   * occurred talking to the server, creating the request, or processing the response.
   */
  void enqueue(Callback<T> callback);
}
```
这是个接口，是我们自己定义的 GitHubApiService 接口中的 listRepos 方法返回的Call对象，接下来就要看 GitHubApiService 接口是如何被实例化的，并且真正返回的Call对象是谁。

6. Retrofit create 方法

```
public <T> T create(final Class<T> service) {
	// 注释一
    validateServiceInterface(service);
	// 注释二
    return (T)
        Proxy.newProxyInstance(
            service.getClassLoader(),
            new Class<?>[] {service},
            new InvocationHandler() {
              private final Platform platform = Platform.get();
              private final Object[] emptyArgs = new Object[0];

              @Override
              public @Nullable Object invoke(Object proxy, Method method, @Nullable Object[] args)
                  throws Throwable {
                // If the method is a method from Object then defer to normal invocation.
                if (method.getDeclaringClass() == Object.class) {
                  return method.invoke(this, args);
                }
                args = args != null ? args : emptyArgs;
                return platform.isDefaultMethod(method)
                    ? platform.invokeDefaultMethod(method, service, proxy, args)
                    : loadServiceMethod(method).invoke(args);
              }
            });
  }
```
注释一：看方法名可猜测是对我们传入的service参数进行一系列的校验，主要是判断传进来的参数是不是一个接口类，这部分内容不在本文讨论范围内，直接跳过。

注释二：是个动态代理，用来返回我们传入的借口类（本文中的 GitHubApiService ）的一个实例。

动态代理？嗯？啥是动态代理？

接下来先插播一段代码，解释一下啥是动态代理。

6.1 动态代理Demo示例
```
public interface GitHubApiService {
    void listRepos(String user);
}

public class ProxyDemo {
    //程序的入口方法
    public static void main(String[] args) {
        //通过动态代理获取 ApiService 的对象
        GitHubApiService apiService = (GitHubApiService) Proxy.newProxyInstance(
                GitHubApiService.class.getClassLoader(),
                new Class[]{GitHubApiService.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                        System.out.println("method = " + method.getName() + "   args = " + Arrays.toString(args));

                        return null;
                    }
                });

        // 输出 apiService 真正的实例类
        System.out.println(apiService.getClass());
        //调用 listRepos 方法
        apiService.listRepos("ZLOVE320483");
    }
}
```
执行上述代码，可以看到控制台的输出结果：
```
class com.sun.proxy.$Proxy0
method = listRepos   args = [ZLOVE320483]
```
从上面的打印结果可以看到，我们传入的 service 接口参数真正的实现类，是动态代理给我生成的一个叫 $Proxy0 的东东，而当我调用 apiService.listRepos 方法时，InvocationHandler 的 invoke 方法拦截到了我们的方法，参数等信息。


事实上，Retrofit的原理正是如此，通过动态代理生成真正的借口实现类，然后拦截到方法，参数，再根据方法和参数上的注解，组装成一个正常的OkHttp请求，然后执行。

如果你还想了解动态代理的原理，那你可以到[传送门](https://blog.csdn.net/lmj623565791/article/details/79278864)

下面来看一下动态代理帮我们生成的 $Proxy0 这个类：
```
class $Proxy0 extends Proxy implements GitHubApiService {

    protected $Proxy0(InvocationHandler h) {
        super(h);
    }

    @Override
    public void listRepos(String user) {

        Method method = Class.forName("GitHubApiService").getMethod("listRepos", String.class);

        super.h.invoke(this, method, new Object[]{user});
    }
}
```
由此得出结论，我们在调用listRepos方法的时候，实际上调用的是 InvocationHandler 的 invoke 方法。

6.2 动态代理总结

- ProxyDemo代码运行中，会动态创建 GitHubApiService 接口的实现类，作为代理对象，执行InvocationHandler 的 invoke 方法。
- 动态指的是在运行期，而代理指的是实现了GitHubApiService 接口的具体类，称之为代理。
- 本质上是在运行期，生成了 GitHubApiService 接口的实现类，调用了 InvocationHandler 的 invoke方法。

现在已经解决了最开始提出来的第一个问题，什么是动态代理。

讲解完动态代理，继续回到我们的 ``` retrofit.create(GitHubApiService::class.java) ``` 方法。


7. 再看Retrofit.create()方法
```
public <T> T create(final Class<T> service) {
    validateServiceInterface(service);
    return (T)
        Proxy.newProxyInstance(
            service.getClassLoader(), // 1
            new Class<?>[] {service}, // 2
            new InvocationHandler() {// 3
              private final Platform platform = Platform.get();
              private final Object[] emptyArgs = new Object[0];

              @Override
              public @Nullable Object invoke(Object proxy, Method method, @Nullable Object[] args)
                  throws Throwable {
                // If the method is a method from Object then defer to normal invocation.
                if (method.getDeclaringClass() == Object.class) { // 4
                  return method.invoke(this, args);
                }
                args = args != null ? args : emptyArgs;
				// 5
                return platform.isDefaultMethod(method)
                    ? platform.invokeDefaultMethod(method, service, proxy, args)
                    : loadServiceMethod(method).invoke(args);
              }
            });
  }
```
注释 1：获取一个 ClassLoader 对象

注释 2：GitHubApiService 的字节码对象传到数组中去

注释 3：InvocationHandler 的 invoke 是关键，从上面动态代理的 Demo 中，我们知道，在GitHubApiService声明的 listRepos方法在调用时，会执行 InvocationHandler 的invoke的方法体。

注释 4：因为有代理类的生成，默认继承 Object 类，所以如果是 Object.class 走，默认调用它的方法

注释 5：如果是默认方法（比如 Java8 ），就执行 platform 的默认方法。否则执行loadServiceMethod方法的invoke方法

追到这里就可以发现 ``` loadServiceMethod(method).invoke(args); ``` 方法是retrofit最为关键的代码，也是本文需要着重分析的代码。


