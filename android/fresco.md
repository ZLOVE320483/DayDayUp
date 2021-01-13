## Fresco源码鉴赏

Fresco是一个功能趋于完善的图片加载框架，在Android开发中有着广泛的应用，正是它的种种优点让它备受推崇。先简单说说它的特色：

- 完善的内存管理功能，减少图片对内存的占用，即便在低端机器上也有着不错的表现。
- 自定义图片加载的过程，可以先显示低清晰度图片或者缩略图，加载完成后再显示高清图，可以在加载的时候缩放和旋转图片。
- 自定义图片绘制的过程，可以自定义谷中焦点、圆角图、占位图、overlay、进图条。
- 渐进式显示图片。
- 支持Gif。
- 支持Webp。

由于Fresco比较大，我们先来看一下它的整体结构，有个整体的把握，Fresco的整体架构如下图所示：

![Fresco整体架构](https://github.com/ZLOVE320483/DayDayUp/blob/main/pic/fresco1.jpeg)

- DraweeView：继承于ImageView，只是简单的读取xml文件的一些属性值和做一些初始化的工作，图层管理交由Hierarchy负责，图层数据获取交由负责。
- DraweeHierarchy：由多层Drawable组成，每层Drawable提供某种功能（例如：缩放、圆角）。
- DraweeController：控制数据的获取与图片加载，向pipeline发出请求，并接收相应事件，并根据不同事件控制Hierarchy，从DraweeView接收用户的事件，然后执行取消网络请求、回收资源等操作。
- DraweeHolder：统筹管理Hierarchy与DraweeHolder。
- ImagePipeline：Fresco的核心模块，用来以各种方式（内存、磁盘、网络等）获取图像。
- Producer/Consumer：Producer也有很多种，它用来完成网络数据获取，缓存数据获取、图片解码等多种工作，它产生的结果由Consumer进行消费。
- IO/Data：这一层便是数据层了，负责实现内存缓存、磁盘缓存、网络缓存和其他IO相关的功能。

>纵观整个Fresco的架构，DraweeView是门面，和用户进行交互，DraweeHierarchy是视图层级，管理图层，DraweeController是控制器，管理数据。它们构成了整个Fresco框架的三驾马车。当然还有我们幕后英雄Producer，所有的脏活累活都是它干的，最佳劳模。

理解了Fresco整体的架构，我们还要了解在这套框架里发挥重要作用的几个关键角色，如下所示：

- Supplier：提供一种特定类型的对象，Fresco里有很多以Supplier结尾的类都实现了这个接口。

- SimpleDraweeView：这个我们就很熟悉了，它接收一个URL，然后调用Controller去加载图片。该类继承于GenericDraweeView，GenericDraweeView又继承于DraweeView，DraweeView是Fresco的顶层View类。

- PipelineDraweeController：负责图片数据的获取与加载，它继承于AbstractDraweeController，由PipelineDraweeControllerBuilder构建而来。AbstractDraweeController实现了DraweeController接口，DraweeController是Fresco的数据大管家，所以的图片数据的处理都是由它来完成的。

- GenericDraweeHierarchy：负责SimpleDraweeView上的图层管理，由多层Drawable组成，每层Drawable提供某种功能（例如：缩放、圆角），该类由GenericDraweeHierarchyBuilder进行构建，该构建器将placeholderImage、retryImage、failureImage、progressBarImage、background、overlays与pressedStateOverlay等xml文件或者Java代码里设置的属性信息都传入GenericDraweeHierarchy中，由GenericDraweeHierarchy进行处理。

- DraweeHolder：该类是一个Holder类，和SimpleDraweeView关联在一起，DraweeView是通过DraweeHolder来统一管理的。而DraweeHolder又是用来统一管理相关的Hierarchy与Controller。

- DataSource：类似于Java里的Futures，代表数据的来源，和Futures不同，它可以有多个result。

- DataSubscriber：接收DataSource返回的结果。

- ImagePipeline：用来调取获取图片的接口。

- Producer：加载与处理图片，它有多种实现，例如：NetworkFetcherProducer，LocalAssetFetcherProducer，LocalFileFetchProducer。从这些类的名字我们就可以知道它们是干什么的。Producer由ProducerFactory这个工厂类构建的，而且所有的Producer都是像Java的IO流那样，可以一层嵌套一层，最终只得到一个结果，这是一个很精巧的设计。

- Consumer：用来接收Producer产生的结果，它与Producer组成了生产者与消费者模式。

> 注：Fresco源码里的类的名字都比较长，但是都是按照一定的命令规律来的，例如：以Supplier结尾的类都实现了Supplier接口，它可以提供某一个类型的对象（factory, generator, builder, closure等）。以Builder结尾的当然就是以构造者模式创建对象的类。

通过上面的描述，我们对Fresco有了一个整体认知，面对这样一个庞大的库，我们在分析的时候需要关注以下几个重点：

1. 图片加载流程
2. DraweeController与DraweeHierarchy
3. Producer与Consumer
4. 缓存机制

下面就让我们一起进入到源码中。
### 图片加载流程

举例

> 初始化
```
Fresco.initialize(this);
```
> 加载图片
```
String url = "https://github.com/guoxiaoxing/android-open-framwork-analysis/raw/master/art/fresco/scenery.jpg";
SimpleDraweeView simpleDraweeView = findViewById(R.id.drawee_view);
simpleDraweeView.setImageURI(Uri.parse(url));
```
我们来看一下它的调用流程，序列图如下所示：

![Fresco调用流程图](https://github.com/ZLOVE320483/DayDayUp/blob/main/pic/fresco2.png)

嗯，图看起来有点大，但是不要紧，我们按照颜色将整个流程分为了四大步：

1. 初始化Fresco。
2. 获取DataSource。
3. 绑定Controller与Hierarchy。
4. 从内存缓存/磁盘缓存/网络获取图片，并设置到对应的Drawable层。

> 注：Fresco里的类虽多，类名虽长，但都是基于接口和Abstract类的设计，每个模块自成一套继承体系，所以只要掌握了它们的继承关系以及不同模块之间的联系，整个流程还是比较简单的。

由于序列图涉及具体细节，为了辅助理解，我们再提供一张总结新的流程图，如下所示：

![Fresco调用流程图](https://github.com/ZLOVE320483/DayDayUp/blob/main/pic/fresco3.png)

接下来，我们就针对这两张图结合具体细节来一一分析。

### 1.1 初始化Fresco

```
public class Fresco {
    public static void initialize(
        Context context,
        @Nullable ImagePipelineConfig imagePipelineConfig,
        @Nullable DraweeConfig draweeConfig) {
      //... 重复初始化检验
      try {
        //1. 加载so库，这个主要是一些第三方的native库，例如：giflib，libjpeg，libpng，
        //主要用来做图片解码。
        SoLoader.init(context, 0);
      } catch (IOException e) {
        throw new RuntimeException("Could not initialize SoLoader", e);
      }
      //2. 设置传入的配置参数magePipelineConfig。
      context = context.getApplicationContext();
      if (imagePipelineConfig == null) {
        ImagePipelineFactory.initialize(context);
      } else {
        ImagePipelineFactory.initialize(imagePipelineConfig);
      }
      //3. 初始化SimpleDraweeView。
      initializeDrawee(context, draweeConfig);
    }
  
    private static void initializeDrawee(
        Context context,
        @Nullable DraweeConfig draweeConfig) {
      //构建PipelineDraweeControllerBuilderSupplier对象，并传给SimpleDraweeView。
      sDraweeControllerBuilderSupplier =
          new PipelineDraweeControllerBuilderSupplier(context, draweeConfig);
      SimpleDraweeView.initialize(sDraweeControllerBuilderSupplier);
    }  
}
```

可以发现，Fresco在初始化的过程中，主要做了三件事情：
1. 加载so库，这个主要是一些第三方的native库，例如：giflib，libjpeg，libpng，主要用来做图片解码。
2. 设置传入的配置参数imagePipelineConfig。
3. 初始化SimpleDraweeView。

这里面我们需要重点关注俩个对象：

- ImagePipelineConfig：ImagePipeline参数配置。
- DraweeControllerBuilderSupplier：提供DraweeControllerBuilder用来构建DraweeController。

我们先来看ImagePipelineConfig，ImagePipelineConfig通过建造者模式来构建传递给ImagePipeline的参数，如下所示：

- Bitmap.Config mBitmapConfig; 图片质量。
- Supplier mBitmapMemoryCacheParamsSupplier; 内存缓存的配置参数提供者。
- CountingMemoryCache.CacheTrimStrategy mBitmapMemoryCacheTrimStrategy; 内存缓存的削减策略。
- CacheKeyFactory mCacheKeyFactory; CacheKey的创建工厂。
- Context mContext; 上下文环境。
- boolean mDownsampleEnabled; 是否开启图片向下采样。
- FileCacheFactory mFileCacheFactory; 磁盘缓存创建工厂。
- Supplier mEncodedMemoryCacheParamsSupplier; 未解码图片缓存配置参数提供者。
- ExecutorSupplier mExecutorSupplier; 线程池提供者
- ImageCacheStatsTracker mImageCacheStatsTracker; 图片缓存状态追踪器。
- ImageDecoder mImageDecoder; 图片解码器。
- Supplier mIsPrefetchEnabledSupplier; 是否开启预加载。
- DiskCacheConfig mMainDiskCacheConfig; 磁盘缓存配置。
- MemoryTrimmableRegistry mMemoryTrimmableRegistry; 内存变化监听注册表，那些需要监听系统内存变化的对象需要添加到这个表中类。
- NetworkFetcher mNetworkFetcher; 下载网络图片，默认使用内置的HttpUrlConnectionNetworkFetcher，也可以自定义。
- PlatformBitmapFactory mPlatformBitmapFactory; 根据不同的Android版本生成不同的Bitmap的工厂，主要的区别在Bitmap在内存中的位置，Android 5.0以下存储在Ashmem中，Android 5.0以上存在Java Heap中。
- PoolFactory mPoolFactory; Bitmap池等各种池的构建工厂。
- ProgressiveJpegConfig mProgressiveJpegConfig; 渐进式JPEG配置。
- Set mRequestListeners; 请求监听器集合，监听请求过程中的各种事件。
- boolean mResizeAndRotateEnabledForNetwork; 是否开启网络图片的压缩和旋转。
- DiskCacheConfig mSmallImageDiskCacheConfig; 磁盘缓存配置
- ImageDecoderConfig mImageDecoderConfig; 图片解码配置
- ImagePipelineExperiments mImagePipelineExperiments; Fresco提供的关于Image Pipe的实验性功能。

上述参数基本不需要我们手动配置，除非项目上有定制性的需求。

我们可以发现，在初始化方法的最后调用initializeDrawee()给SimpleDraweeView传入了一PipelineDraweeControllerBuilderSupplier，这是一个很重要的对象，我们来看看它都初始化了哪些东西。

```
public class PipelineDraweeControllerBuilderSupplier implements
    Supplier<PipelineDraweeControllerBuilder> {
    
      public PipelineDraweeControllerBuilderSupplier(
          Context context,
          ImagePipelineFactory imagePipelineFactory,
          Set<ControllerListener> boundControllerListeners,
          @Nullable DraweeConfig draweeConfig) {
        mContext = context;
        //1. 获取ImagePipeline
        mImagePipeline = imagePipelineFactory.getImagePipeline();
    
        if (draweeConfig != null && draweeConfig.getPipelineDraweeControllerFactory() != null) {
          mPipelineDraweeControllerFactory = draweeConfig.getPipelineDraweeControllerFactory();
        } else {
          mPipelineDraweeControllerFactory = new PipelineDraweeControllerFactory();
        }
        //2. 获取PipelineDraweeControllerFactory，并初始化。
        mPipelineDraweeControllerFactory.init(
            context.getResources(),
            DeferredReleaser.getInstance(),
            imagePipelineFactory.getAnimatedDrawableFactory(context),
            UiThreadImmediateExecutorService.getInstance(),
            mImagePipeline.getBitmapMemoryCache(),
            draweeConfig != null
                ? draweeConfig.getCustomDrawableFactories()
                : null,
            draweeConfig != null
                ? draweeConfig.getDebugOverlayEnabledSupplier()
                : null);
        mBoundControllerListeners = boundControllerListeners;
      }
}
```
