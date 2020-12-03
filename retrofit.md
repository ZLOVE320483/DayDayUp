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

7.1 深入 loadServiceMethod 方法

先看 loadServiceMethod 方法的返回对象是什么，再看它的 invoke 方法
```
ServiceMethod<?> loadServiceMethod(Method method) {
	// 1
    ServiceMethod<?> result = serviceMethodCache.get(method);
    if (result != null) return result;

    synchronized (serviceMethodCache) {
      result = serviceMethodCache.get(method);
      if (result == null) {
		// 2
        result = ServiceMethod.parseAnnotations(this, method);
		// 3
        serviceMethodCache.put(method, result);
      }
    }
    return result;
  }
```
注释 1：从 ConcurrentHashMap 中取一个 ServiceMethod 如果存在直接返回

注释 2：通过 ServiceMethod.parseAnnotations(this, method);方法创建一个 ServiceMethod 对象

注释 3：用 Map 把创建的 ServiceMethod 对象缓存起来，因为我们的请求方法可能会调用多次，缓存提升性能。

看一下 ServiceMethod.parseAnnotations(this, method);方法具体返回的对象是什么，然后再看它的 invoke 方法

7.2 ServiceMethod的parseAnnotations方法
```
//ServiceMethod.java
static <T> ServiceMethod<T> parseAnnotations(Retrofit retrofit, Method method) {
  ...
  //1
  return HttpServiceMethod.parseAnnotations(retrofit, method, requestFactory);
}
```
返回的是一个HttpServiceMethod对象，那么接下来看下它的 invoke 方法
```
//HttpServiceMethod.java
@Override
final @Nullable ReturnT invoke(Object[] args) {
  //1
  Call<ResponseT> call = new OkHttpCall<>(requestFactory, args, callFactory, responseConverter);
  //2
  return adapt(call, args);
}
```
注释 1：创建了一个Call对象，是 OkHttpCall，这个不就是在 GitHubApiService 这个接口声明的 Call 对象吗？

然后再看 OkHttpCall 的enqueue方法，不就知道是怎么进行请求，怎么回调的了吗？

注释 2：是一个 adapt 方法，在不使用 Kotlin 协程的情况下，其实调用的是子类 CallAdapted 的 adapt，这个会在下面具体分析。

现在我们已经知道了 GitHubApiService 接口中定义的 listRepos中的 Call 对象，是 OkHttpCall，接下里看OkHttpCall 的 enqueue 方法。

8. OkHttpCall的enqueue方法

这段代码比较长，但这个就是这个请求的关键，以及怎么使用 OkHttp 进行请求的，如果解析 Response 的，如何回调的。
```
//OkHttpCall.java
@Override
public void enqueue(final Callback<T> callback) {
  Objects.requireNonNull(callback, "callback == null");

  //1
  okhttp3.Call call;
  Throwable failure;

  synchronized (this) {
    if (executed) throw new IllegalStateException("Already executed.");
    executed = true;

    call = rawCall;
    failure = creationFailure;
    if (call == null && failure == null) {
      try {
        //2
        call = rawCall = createRawCall();
      } catch (Throwable t) {
        throwIfFatal(t);
        failure = creationFailure = t;
      }
    }
  }

  if (failure != null) {
    callback.onFailure(this, failure);
    return;
  }

  if (canceled) {
    call.cancel();
  }

  //3
  call.enqueue(
      new okhttp3.Callback() {
        @Override
        public void onResponse(okhttp3.Call call, okhttp3.Response rawResponse) {
          Response<T> response;
          try {
            //4
            response = parseResponse(rawResponse);
          } catch (Throwable e) {
            throwIfFatal(e);
            callFailure(e);
            return;
          }

          try {
            //5
            callback.onResponse(OkHttpCall.this, response);
          } catch (Throwable t) {
            throwIfFatal(t);
            t.printStackTrace(); // TODO this is not great
          }
        }

        @Override
        public void onFailure(okhttp3.Call call, IOException e) {
          callFailure(e);
        }

        private void callFailure(Throwable e) {
          try {
            //6
            callback.onFailure(OkHttpCall.this, e);
          } catch (Throwable t) {
            throwIfFatal(t);
            t.printStackTrace(); // TODO this is not great
          }
        }
      });
}
```
注释 1：声明一个 okhttp3.Call 对象，用来进行网络请求。

注释 2：给 okhttp3.Call 对象进行赋值，下面会具体看代码，如果创建了一个 okhttp3.Call 对象。

注释 3：调用 okhttp3.Call 的 enqueue 方法，进行真正的网络请求

注释 4：解析响应，下面会具体看代码

注释 5：成功的回调

注释 6：失败的回调

到现在，我们文章开头两个疑问得到解释了

整个请求的流程是怎样的？

底层是如何用 OkHttp 请求的？

我们还要看下一个 okhttp3.Call 对象是怎么创建的，我们写的注解参数是怎么解析的，响应结果是如何解析的，也就是我们在 Retrofit 中配置 addConverterFactory(GsonConverterFactory.create())是如何直接拿到数据模型的。

8.1 看下 call = rawCall = createRawCall();方法
```
//OkHttpCall.java
private final okhttp3.Call.Factory callFactory;

private okhttp3.Call createRawCall() throws IOException {
  //1 callFactory是什么
  okhttp3.Call call = callFactory.newCall(requestFactory.create(args));
  if (call == null) {
    throw new NullPointerException("Call.Factory returned null.");
  }
  return call;
}
```
通过 callFactory 创建的，看一下 callFactory 的赋值过程
```
//OkHttpCall.java
OkHttpCall(
    RequestFactory requestFactory,
    Object[] args,
    okhttp3.Call.Factory callFactory,
    Converter<ResponseBody, T> responseConverter) {
  this.requestFactory = requestFactory;
  this.args = args;
  //通过 OkHttpCall 构造直接赋值
  this.callFactory = callFactory;
  this.responseConverter = responseConverter;
}
```
在 OkHttpCall 构造中直接赋值，那接下来就继续往回追代码
```
//HttpServiceMethod.java
private final okhttp3.Call.Factory callFactory;

@Override
final @Nullable ReturnT invoke(Object[] args) {
  //在 OkHttpCall 实例化时赋值， callFactory 是 HttpServiceMethod 的成员变量
  Call<ResponseT> call = new OkHttpCall<>(requestFactory, args, callFactory, responseConverter);
  return adapt(call, args);
}

//callFactory 是在 HttpServiceMethod 的构造中赋值的
HttpServiceMethod(
    RequestFactory requestFactory,
    okhttp3.Call.Factory callFactory,
    Converter<ResponseBody, ResponseT> responseConverter) {
  this.requestFactory = requestFactory;
    //通过 HttpServiceMethod 构造直接赋值
  this.callFactory = callFactory;
  this.responseConverter = responseConverter;
}
```
发现 callFactory 的值是在创建 HttpServiceMethod 时赋值的，继续跟！

在 7.2 小节，有一行代码HttpServiceMethod.parseAnnotations(retrofit, method, requestFactory);我们没有跟进去，现在看一下 HttpServiceMethod 是怎么创建的
```
static <ResponseT, ReturnT> HttpServiceMethod<ResponseT, ReturnT> parseAnnotations(
    Retrofit retrofit, Method method, RequestFactory requestFactory) {
  boolean isKotlinSuspendFunction = requestFactory.isKotlinSuspendFunction;
  boolean continuationWantsResponse = false;
  boolean continuationBodyNullable = false;

    //1
  okhttp3.Call.Factory callFactory = retrofit.callFactory;
  if (!isKotlinSuspendFunction) {
    //2
    return new CallAdapted<>(requestFactory, callFactory, responseConverter, callAdapter);
  } else if (continuationWantsResponse) {
    //noinspection unchecked Kotlin compiler guarantees ReturnT to be Object.
    return (HttpServiceMethod<ResponseT, ReturnT>)
        new SuspendForResponse<>(
            requestFactory,
            callFactory,
            responseConverter,
            (CallAdapter<ResponseT, Call<ResponseT>>) callAdapter);
  } else {
    //noinspection unchecked Kotlin compiler guarantees ReturnT to be Object.
    return (HttpServiceMethod<ResponseT, ReturnT>)
        new SuspendForBody<>(
            requestFactory,
            callFactory,
            responseConverter,
            (CallAdapter<ResponseT, Call<ResponseT>>) callAdapter,
            continuationBodyNullable);
  }
}
```
注释 1：callFactory 的值是从 Retrofit 这个对象拿到的

注释 2：如果不是 Kotlin 的挂起函数，返回是的 CallAdapted 对象
```
static final class CallAdapted<ResponseT, ReturnT> extends HttpServiceMethod<ResponseT, ReturnT> {}
```
CallAdapted 是 HttpServiceMethod 的子类，会调用 adapt方法进行 CallAdapter 的转换，我们后面会详细看。

继续看 Retrofit 的 callFactory 的值Retrofit是通过Builder构建的，看下Builder类
```
//Retrofit.java
public static final class Builder {
    public Retrofit build() {

      okhttp3.Call.Factory callFactory = this.callFactory;
      if (callFactory == null) {
        //1
        callFactory = new OkHttpClient();
      }

      return new Retrofit(
          callFactory,
          baseUrl,
          unmodifiableList(converterFactories),
          unmodifiableList(callAdapterFactories),
          callbackExecutor,
          validateEagerly);
    }

}
```
原来 callFactory 实际是一个 OkHttpClient 对象，也就是 OkHttpClient 创建了一个 Call 对象，嗯就是 OKHttp 网络请求的那一套。

在创建okhttp3.Call 对象的 callFactory.newCall(requestFactory.create(args));方法中的 requestFactory.create(args)方法会返回一个 Request 的对象，这个我们也会在下面看是如何构造一个 OkHttp 的 Request 请求对象的。

8.2.请求注解参数是怎么解析的
```
//ServiceMethod.java
static <T> ServiceMethod<T> parseAnnotations(Retrofit retrofit, Method method) {
  //1
  RequestFactory requestFactory = RequestFactory.parseAnnotations(retrofit, method);
    ...
  //2
  return HttpServiceMethod.parseAnnotations(retrofit, method, requestFactory);
}
```
注释 1：通过 RequestFactory 解析注解，然后返回 RequestFactory 对象

注释 2：把 RequestFactory 对象往 HttpServiceMethod 里面传递，下面会具体看 RequestFactory 对象具体干什么用了？

继续跟代码RequestFactory.parseAnnotations

```
//RequestFactory.java
final class RequestFactory {
  static RequestFactory parseAnnotations(Retrofit retrofit, Method method) {
    //看build方法
    return new Builder(retrofit, method).build();
  }

    RequestFactory build() {
      //1
      for (Annotation annotation : methodAnnotations) {
        parseMethodAnnotation(annotation);
      }

     ....

      return new RequestFactory(this);
    }
}
```
遍历 GitHubApiService 这个 API 接口上定义的方法注解，然后解析注解

继续跟代码parseMethodAnnotation

```
//RequestFactory.java
private void parseMethodAnnotation(Annotation annotation) {
  if (annotation instanceof DELETE) {
    parseHttpMethodAndPath("DELETE", ((DELETE) annotation).value(), false);
  } else if (annotation instanceof GET) {
    parseHttpMethodAndPath("GET", ((GET) annotation).value(), false);
  }
  ...
  else if (annotation instanceof POST) {
    parseHttpMethodAndPath("POST", ((POST) annotation).value(), true);
  }
    ....
  else if (annotation instanceof Multipart) {
    if (isFormEncoded) {
      throw methodError(method, "Only one encoding annotation is allowed.");
    }
    isMultipart = true;
  } else if (annotation instanceof FormUrlEncoded) {
    if (isMultipart) {
      throw methodError(method, "Only one encoding annotation is allowed.");
    }
    isFormEncoded = true;
  }
}
```
就是解析方法上的注解，来存到 RequestFactory 的内部。

其实 RequestFactory 这个类还有 parseParameter 和 parseParameterAnnotation这个就是解析方法参数声明上的具体参数的注解，会在后面分析 Kotlin suspend 挂起函数具体讲。

总之：具体代码就是分析方法上注解上面的值，方法参数上，这个就是细节问题了

总结就是：分析方法上的各个注解，方法参数上的注解，最后返回 RequestFactory 对象，给下面使用。

Retrofit 的大框架简单，细节比较复杂。

RequestFactory 对象返回出去，具体干嘛用了？大胆猜一下，解析出注解存到 RequestFactory 对象，这个对象身上可有各种请求的参数，然后肯定是类创建 OkHttp 的 Request请求对象啊，因为是用 OkHttp 请求的，它需要一个 Request 请求对象。

8.3.RequestFactory 对象返回出去，具体干嘛用了?

下面我就用一个代码块贴了，看着更直接，我会具体表明属于哪个类的

```
//ServiceMethod.java
static <T> ServiceMethod<T> parseAnnotations(Retrofit retrofit, Method method) {
  //解析注解参数，获取 RequestFactory 对象
  RequestFactory requestFactory = RequestFactory.parseAnnotations(retrofit, method);
  //把 RequestFactory 对象传给 HttpServiceMethod
  return HttpServiceMethod.parseAnnotations(retrofit, method, requestFactory);
}

//注意换类了
//HttpServiceMethod.java
static <ResponseT, ReturnT> HttpServiceMethod<ResponseT, ReturnT> parseAnnotations(
    Retrofit retrofit, Method method, RequestFactory requestFactory) {

    ...

  okhttp3.Call.Factory callFactory = retrofit.callFactory;
  //不是 Kotlin 的挂起函数
  if (!isKotlinSuspendFunction) {
    //把requestFactory传给 CallAdapted
    return new CallAdapted<>(requestFactory, callFactory, responseConverter, callAdapter);
  } 
  ....
}

//HttpServiceMethod.java
//CallAdapted 是 HttpServiceMethod 的内部类也是 HttpServiceMethod 的子类
CallAdapted(
    RequestFactory requestFactory,
    okhttp3.Call.Factory callFactory,
    Converter<ResponseBody, ResponseT> responseConverter,
    CallAdapter<ResponseT, ReturnT> callAdapter) {
  //这里把 requestFactory 传给 super 父类的构造参数里了，也就是 HttpServiceMethod
  super(requestFactory, callFactory, responseConverter);
  this.callAdapter = callAdapter;
}

//HttpServiceMethod.java
HttpServiceMethod(
    RequestFactory requestFactory,
    okhttp3.Call.Factory callFactory,
    Converter<ResponseBody, ResponseT> responseConverter) {
  // HttpServiceMethod 的 requestFactory 成员变量保存这个 RequestFactory 对象
  this.requestFactory = requestFactory;
  this.callFactory = callFactory;
  this.responseConverter = responseConverter;
}

//因为会调用  HttpServiceMethod 的 invoke 方法
//会把这个 RequestFactory 对象会继续传递给 OkHttpCall 类中
//注意换类了
//OkHttpCall.java
OkHttpCall(
    RequestFactory requestFactory,
    Object[] args,
    okhttp3.Call.Factory callFactory,
    Converter<ResponseBody, T> responseConverter) {
  //给 OkHttpCall 的requestFactory成员变量赋值
  this.requestFactory = requestFactory;
  this.args = args;
  this.callFactory = callFactory;
  this.responseConverter = responseConverter;
}
```
经过层层传递 RequestFactory 这个实例终于是到了 HttpServiceMethod 类中，最终传到了 OkHttpCall 中，那这个 RequestFactory 对象在什么时候使用呢？ RequestFactory 会继续在OkHttpCall中传递，因为 OkHttpCall 才是进行请求的。

在OkHttpCall的 创建 Call 对象时
```
//OkHttpCall.java
private okhttp3.Call createRawCall() throws IOException {
  //1
  okhttp3.Call call = callFactory.newCall(requestFactory.create(args));
  if (call == null) {
    throw new NullPointerException("Call.Factory returned null.");
  }
  return call;
}
```
注释 1：调用了requestFactory.create(args)

注意：此时的RequestFactory的各个成员变量在解析注解那一步都赋值了
```
//RequestFactory.java
okhttp3.Request create(Object[] args) throws IOException {
  ...
  RequestBuilder requestBuilder =
      new RequestBuilder(
          httpMethod,
          baseUrl,
          relativeUrl,
          headers,
          contentType,
          hasBody,
          isFormEncoded,
          isMultipart);
  ...
  return requestBuilder.get().tag(Invocation.class, new Invocation(method, argumentList)).build();
}
```
最终 requestFactory 的值用来构造 okhttp3.Request 的对象

以上就是解析注解，构造出okhttp3.Request的对象全过程了。

也就解答了方法上的注解是什么时候解析的，怎么解析的？这个问题

8.4.请求响应结果是如何解析的

比如我们在构造 Retrofit 的时候加上 addConverterFactory(GsonConverterFactory.create())这行代码，我们的响应结果是如何通过 Gson 直接解析成数据模型的？

在 OkHttpCall 的enqueue方法中
```
//OkHttpCall.java
@Override
public void enqueue(final Callback<T> callback) {

  okhttp3.Call call;
    ...
  call.enqueue(
      new okhttp3.Callback() {
        @Override
        public void onResponse(okhttp3.Call call, okhttp3.Response rawResponse) {
          Response<T> response;
          try {
            //1 解析响应
            response = parseResponse(rawResponse);
          } catch (Throwable e) {
            throwIfFatal(e);
            callFailure(e);
            return;
          }
        }
    ...
      });
}
```
注释 1：通过parseResponse解析响应返回给回调接口

```
//OkHttpCall.java
private final Converter<ResponseBody, T> responseConverter;

Response<T> parseResponse(okhttp3.Response rawResponse) throws IOException {
  ResponseBody rawBody = rawResponse.body();

    ...

  ExceptionCatchingResponseBody catchingBody = new ExceptionCatchingResponseBody(rawBody);
  try {
    //1 通过 responseConverter 转换 ResponseBody
    T body = responseConverter.convert(catchingBody);
    return Response.success(body, rawResponse);
  } catch (RuntimeException e) {
    // If the underlying source threw an exception, propagate that rather than indicating it was
    // a runtime exception.
    catchingBody.throwIfCaught();
    throw e;
  }
}
```
注释 1：通过 responseConverter 调用convert方法

首先那看 responseConverter 是什么以及赋值的过程，然后再看convert方法
```
//OkHttpCall.java
private final Converter<ResponseBody, T> responseConverter;

OkHttpCall(
    RequestFactory requestFactory,
    Object[] args,
    okhttp3.Call.Factory callFactory,
    Converter<ResponseBody, T> responseConverter) {
  this.requestFactory = requestFactory;
  this.args = args;
  this.callFactory = callFactory;
  //在构造中赋值
  this.responseConverter = responseConverter;
}

// OkHttpCall 在 HttpServiceMethod 类中实例化
//注意换类了
//HttpServiceMethod.java
private final Converter<ResponseBody, ResponseT> responseConverter;

HttpServiceMethod(
    RequestFactory requestFactory,
    okhttp3.Call.Factory callFactory,
    Converter<ResponseBody, ResponseT> responseConverter) {
  this.requestFactory = requestFactory;
  this.callFactory = callFactory;
   //在构造中赋值
  this.responseConverter = responseConverter;
}

//HttpServiceMethod 在子类 CallAdapted 调用 super方法赋值
CallAdapted(
    RequestFactory requestFactory,
    okhttp3.Call.Factory callFactory,
    Converter<ResponseBody, ResponseT> responseConverter,
    CallAdapter<ResponseT, ReturnT> callAdapter) {
  //在CallAdapted中调用super赋值
  super(requestFactory, callFactory, responseConverter);
  this.callAdapter = callAdapter;
}
```
继续看 CallAdapted 的初始化中 responseConverter 的赋值过程
```
//HttpServiceMethod.java
static <ResponseT, ReturnT> HttpServiceMethod<ResponseT, ReturnT> parseAnnotations(
    Retrofit retrofit, Method method, RequestFactory requestFactory) {
   ...
  CallAdapter<ResponseT, ReturnT> callAdapter =
      createCallAdapter(retrofit, method, adapterType, annotations);

  //1 实例化responseConverter
  Converter<ResponseBody, ResponseT> responseConverter =
      createResponseConverter(retrofit, method, responseType);

  okhttp3.Call.Factory callFactory = retrofit.callFactory;
  if (!isKotlinSuspendFunction) {
    //2 CallAdapted的实例化赋值
    return new CallAdapted<>(requestFactory, callFactory, responseConverter, callAdapter);
  } 
  ...
}
```
继续跟代码 createResponseConverter方法

```
//HttpServiceMethod.java
private static <ResponseT> Converter<ResponseBody, ResponseT> createResponseConverter(
    Retrofit retrofit, Method method, Type responseType) {
  Annotation[] annotations = method.getAnnotations();
  try {
    //调用的是 retrofit的方法
    return retrofit.responseBodyConverter(responseType, annotations);
  } catch (RuntimeException e) { // Wide exception range because factories are user code.
    throw methodError(method, e, "Unable to create converter for %s", responseType);
  }
}
//注意换类了
//Retrofit.java
final List<Converter.Factory> converterFactories;

public <T> Converter<ResponseBody, T> responseBodyConverter(Type type, Annotation[] annotations) {
  //继续跟 nextResponseBodyConverter
  return nextResponseBodyConverter(null, type, annotations);
}

public <T> Converter<ResponseBody, T> nextResponseBodyConverter(
    @Nullable Converter.Factory skipPast, Type type, Annotation[] annotations) {
    ...
  //1 从 converterFactories工厂中遍历取出
  int start = converterFactories.indexOf(skipPast) + 1;
  for (int i = start, count = converterFactories.size(); i < count; i++) {
    Converter<ResponseBody, ?> converter =
        converterFactories.get(i).responseBodyConverter(type, annotations, this);
    if (converter != null) {
      //noinspection unchecked
      return (Converter<ResponseBody, T>) converter;
    }
  }
  ...
}
```
注释 1：从 converterFactories 遍历取出一个来调用 responseBodyConverter 方法，注意根据 responseType 返回值类型来取到对应的 Converter，如果不为空，直接返回此 Converter 对象

看一下 converterFactories 这个对象的赋值过程
```
//Retrofit.java
final List<Converter.Factory> converterFactories;

Retrofit(
    okhttp3.Call.Factory callFactory,
    HttpUrl baseUrl,
    List<Converter.Factory> converterFactories,
    List<CallAdapter.Factory> callAdapterFactories,
    @Nullable Executor callbackExecutor,
    boolean validateEagerly) {
  this.callFactory = callFactory;
  this.baseUrl = baseUrl;
  this.converterFactories = converterFactories; // Copy+unmodifiable at call site.
  //通过 Retrofit 的构造赋值，Retrofit的 初始化是通过内部 Builder 类的build方法
  this.callAdapterFactories = callAdapterFactories; // Copy+unmodifiable at call site.
  this.callbackExecutor = callbackExecutor;
  this.validateEagerly = validateEagerly;
}

//Retrofit.java 内部类 Builder 类的build方法
//Builder.java
 public Retrofit build() {

   ...
      // Make a defensive copy of the converters.
     //1
      List<Converter.Factory> converterFactories =
          new ArrayList<>(
              1 + this.converterFactories.size() + platform.defaultConverterFactoriesSize());
        //2
      converterFactories.add(new BuiltInConverters());
        //3
      converterFactories.addAll(this.converterFactories);
        //4
      converterFactories.addAll(platform.defaultConverterFactories());

      return new Retrofit(
          callFactory,
          baseUrl,
          unmodifiableList(converterFactories),
          unmodifiableList(callAdapterFactories),
          callbackExecutor,
          validateEagerly);
    }
```
注释 1：初始化 converterFactories 这个 list

注释 2：添加默认的构建的转换器，其实是 StreamingResponseBodyConverter 和 BufferingResponseBodyConverter

注释 3：就是自己添加的转换配置 addConverterFactory(GsonConverterFactory.create())
```
//Retrofit.java 内部类 Builder.java
public Builder addConverterFactory(Converter.Factory factory) {
  converterFactories.add(Objects.requireNonNull(factory, "factory == null"));
  return this;
}
```
注释 4：如果是 Java8 就是一个 OptionalConverterFactory 的转换器否则就是一个空的

注意：是怎么找到GsonConverterFactory来调用 Gson 的 convert方法的呢？在遍历converterFactories时会根据 responseType来找到对应的转换器。

具体 GsonConverterFactory 的 convert 方法就是 Gson 的逻辑了，就不是 Retrofit 的重点了。

到现在Converter 的转换过程，我们也就清楚了。

还有一个问题，我们写的 API 接口是如何支持 RxJava 的

9. CallAdapter的替换过程

9.1.使用 RxJava 进行网络请求

怎么转成 RxJava

比如：我们在初始化一个Retrofit时加入 addCallAdapterFactory(RxJava2CallAdapterFactory.create())这行

```
//初始化一个Retrofit对象
val retrofit = Retrofit.Builder()
    .baseUrl("https://api.github.com/")
    //加入 RxJava2CallAdapterFactory 支持
    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    .addConverterFactory(GsonConverterFactory.create())
    .build()
```
加入 RxJava2 的配置支持后，把 RxJava2CallAdapterFactory 存到 callAdapterFactories 这个集合中，记住这一点，下面要用到。

```
interface GitHubApiService {
    @GET("users/{user}/repos")
    fun listReposRx(@Path("user") user: String?): Single<Repo>
}
```
我们就可以这么请求接口了
```
//创建出GitHubApiService对象
val service = retrofit.create(GitHubApiService::class.java)
service.listReposRx("octocat")
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe({ repo ->
        "response name = ${repo[0].name}".logE()
    }, { error ->
        error.printStackTrace()
    })
```
我们可以在自己定义的 API 接口中直接返回一个 RxJava 的 Single 对象的，来进行操作了。

我们下面就来看下是如何把请求对象转换成一个 Single 对象的
```
//Retrofit.java 内部类 Builder.java
public Builder addCallAdapterFactory(CallAdapter.Factory factory) {
  callAdapterFactories.add(Objects.requireNonNull(factory, "factory == null"));
  return this;
}
```
把 RxJava2CallAdapterFactory 存到了callAdapterFactories 这个 list 中了。

接下来我们看下是如何使用 callAdapterFactories 的 RxJava2CallAdapterFactory 中的这个 CallAdapter 的吧

这就要看我们之前看到了一个类了 HttpServiceMethod 的parseAnnotations之前看过它的代码，只是上次看的是Converter是如何赋值的也就是第 8.4 小节，这次看 CallAdapter 是如何被赋值使用的。

9.2CallAdapter是如何被赋值过程

HttpServiceMethod的parseAnnotations方法
```
//HttpServiceMethod.java
static <ResponseT, ReturnT> HttpServiceMethod<ResponseT, ReturnT> parseAnnotations(
    Retrofit retrofit, Method method, RequestFactory requestFactory) {

  ....
  //1
  CallAdapter<ResponseT, ReturnT> callAdapter =
      createCallAdapter(retrofit, method, adapterType, annotations);

  okhttp3.Call.Factory callFactory = retrofit.callFactory;
  if (!isKotlinSuspendFunction) {
    //2
    return new CallAdapted<>(requestFactory, callFactory, responseConverter, callAdapter);
  } 
    ...
}
```
注释 1：初始化 CallAdapter

注释 2：给 CallAdapted 中的 callAdapter 变量赋值，然后调用它的adapt 方法。

我们先找到具体 CallAdapter 赋值的对象，然后看它的adapt就知道了，是如何转换的了

接下来就是跟代码的过程了
```
//HttpServiceMethod.java
private static <ResponseT, ReturnT> CallAdapter<ResponseT, ReturnT> createCallAdapter(
    Retrofit retrofit, Method method, Type returnType, Annotation[] annotations) {
  try {
    //noinspection unchecked
    //调用retrofit的callAdapter方法
    return (CallAdapter<ResponseT, ReturnT>) retrofit.callAdapter(returnType, annotations);
  } catch (RuntimeException e) { // Wide exception range because factories are user code.
    throw methodError(method, e, "Unable to create call adapter for %s", returnType);
  }
}

//Retrofit.java
public CallAdapter<?, ?> callAdapter(Type returnType, Annotation[] annotations) {
  //调用nextCallAdapter
  return nextCallAdapter(null, returnType, annotations);
}

public CallAdapter<?, ?> nextCallAdapter(
    @Nullable CallAdapter.Factory skipPast, Type returnType, Annotation[] annotations) {

  ...

  //遍历 callAdapterFactories
  int start = callAdapterFactories.indexOf(skipPast) + 1;
  for (int i = start, count = callAdapterFactories.size(); i < count; i++) {
    //是具体CallAdapterFactory的 get 方法
    CallAdapter<?, ?> adapter = callAdapterFactories.get(i).get(returnType, annotations, this);
    if (adapter != null) {
      return adapter;
    }
  }
  ...
}
```
遍历 callAdapterFactories 根据 returnType类型 来找到对应的 CallAdapter 返回

比如：我们在 GitHubApiService 的 returnType 类型为 Single，那么返回的就是 RxJava2CallAdapterFactory 所获取的 CallAdapter

```
interface GitHubApiService {
    @GET("users/{user}/repos")
    fun listReposRx(@Path("user") user: String?): Single<Repo>
}
```
RxJava2CallAdapterFactory的 get方法
```
//RxJava2CallAdapterFactory.java
@Override public @Nullable CallAdapter<?, ?> get(
    Type returnType, Annotation[] annotations, Retrofit retrofit) {
  Class<?> rawType = getRawType(returnType);

  if (rawType == Completable.class) {
    return new RxJava2CallAdapter(Void.class, scheduler, isAsync, false, true, false, false,
        false, true);
  }

  boolean isFlowable = rawType == Flowable.class;
  //当前是Single类型
  boolean isSingle = rawType == Single.class;
  boolean isMaybe = rawType == Maybe.class;
  if (rawType != Observable.class && !isFlowable && !isSingle && !isMaybe) {
    return null;
  }
  ...
    //返回 RxJava2CallAdapter对象，isSingle参数为 true
  return new RxJava2CallAdapter(responseType, scheduler, isAsync, isResult, isBody, isFlowable,
      isSingle, isMaybe, false);
}
```
返回的是 RxJava2CallAdapter 对象，并且根据 rawType 判断当前是个什么类型

看下 RxJava2CallAdapter 的adapt方法
```
//RxJava2CallAdapter.java
@Override public Object adapt(Call<R> call) {
  //1 把Call包装成一个Observable对象
  Observable<Response<R>> responseObservable = isAsync
      ? new CallEnqueueObservable<>(call)
      : new CallExecuteObservable<>(call);

  Observable<?> observable;
  if (isResult) {
    observable = new ResultObservable<>(responseObservable);
  } else if (isBody) {
    observable = new BodyObservable<>(responseObservable);
  } else {
    observable = responseObservable;
  }

  if (scheduler != null) {
    observable = observable.subscribeOn(scheduler);
  }

  if (isFlowable) {
    return observable.toFlowable(BackpressureStrategy.LATEST);
  }
  //2
  if (isSingle) {
    return observable.singleOrError();
  }
  if (isMaybe) {
    return observable.singleElement();
  }
  if (isCompletable) {
    return observable.ignoreElements();
  }
  return RxJavaPlugins.onAssembly(observable);
}
```
注释 1：把 Call 包装成一个 Observable 对象

注释2：如果是 Single 则调用observable.singleOrError();方法

到目前为止，CallAdapter 怎么变成一个 RxJava2CallAdapter 以及它的具体调用，我们也就清楚了。