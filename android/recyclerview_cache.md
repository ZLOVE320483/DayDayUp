## RecyclerView缓存机制

### 前沿

RecyclerView这个控件几乎所有的Android开发者都使用过（甚至不用加几乎），它是真的很好用，完美取代了ListView和GridView，而RecyclerView之所以好用，得益于它优秀的缓存机制。关于RecyclerView缓存机制，更是需要我们开发者来掌握的。本文就将先从整体流程看RecyclerView的缓存，再带你从源码角度分析，跳过读源码的坑，最后用一个简单的demo的形式展示出来。在开始RecyclerView的缓存机制之前我们先学习关于ViewHolder的知识。

### RecyclerView为什么强制我们实现ViewHolder模式？

关于这个问题，我们首先看一下ListView。ListView是不强制我们实现ViewHolder的，但是后来google建议我们实现ViewHolder模式。我们先分别看一下这两种不同的方式。

![不使用viewholder](https://github.com/ZLOVE320483/DayDayUp/blob/main/pic/rv_cache1.jpg)

![使用viewholder](https://github.com/ZLOVE320483/DayDayUp/blob/main/pic/rv_cache2.jpg)

其实这里我已经用红框标出来了，ListView使用ViewHolder的好处就在于可以避免每次getView都进行findViewById()操作，因为findViewById()利用的是DFS算法（深度优化搜索），是非常耗性能的。而对于RecyclerView来说，强制实现ViewHolder的其中一个原因就是避免多次进行findViewById（）的处理，另一个原因就是因为ItemView和ViewHolder的关系是一对一，也就是说一个ViewHolder对应一个ItemView。这个ViewHolder当中持有对应的ItemView的所有信息，比如说：position；view；width等等，拿到了ViewHolder基本就拿到了ItemView的所有信息，而ViewHolder使用起来相比itemView更加方便。RecyclerView缓存机制缓存的就是ViewHolder（ListView缓存的是ItemView），这也是为什么RecyclerView为什么强制我们实现ViewHolder的原因。

### Listview的缓存机制

在正式讲RecyclerView的缓存机制之前还需要提一嘴ListView的缓存机制，不多BB，先上图

![listview缓存](https://github.com/ZLOVE320483/DayDayUp/blob/main/pic/rv_cache3.jpg)

ListView的缓存有两级，在ListView里面有一个内部类 RecycleBin，RecycleBin有两个对象Active View和Scrap View来管理缓存，Active View是第一级，Scrap View是第二级。

- **Active View:** 是缓存在屏幕内的ItemView，当列表数据发生变化时，屏幕内的数据可以直接拿来复用，无须进行数据绑定。
- **Scrap view:** 缓存屏幕外的ItemView，这里所有的缓存的数据都是"脏的"，也就是数据需要重新绑定，也就是说屏幕外的所有数据在进入屏幕的时候都要走一遍getView（）方法。

再来一张图，看看ListView的缓存流程

![listview缓存流程](https://github.com/ZLOVE320483/DayDayUp/blob/main/pic/rv_cache4.jpg)

当Active View和Scrap View中都没有缓存的时候就会直接create view。

### 小结

ListView的缓存机制相对比较好理解，它只有两级缓存，一级缓存Active View是负责屏幕内的ItemView快速复用，而Scrap View是缓存屏幕外的数据，当该数据从屏幕外滑动到屏幕内的时候需要走一遍getView()方法。

### RecyclerView的缓存机制

先上图

![recyclerview缓存](https://github.com/ZLOVE320483/DayDayUp/blob/main/pic/rv_cache5.jpg)