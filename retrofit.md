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






