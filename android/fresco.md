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
