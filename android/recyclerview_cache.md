## RecyclerView缓存机制

### 前沿

RecyclerView这个控件几乎所有的Android开发者都使用过（甚至不用加几乎），它是真的很好用，完美取代了ListView和GridView，而RecyclerView之所以好用，得益于它优秀的缓存机制。关于RecyclerView缓存机制，更是需要我们开发者来掌握的。本文就将先从整体流程看RecyclerView的缓存，再带你从源码角度分析，跳过读源码的坑，最后用一个简单的demo的形式展示出来。在开始RecyclerView的缓存机制之前我们先学习关于ViewHolder的知识。

### RecyclerView为什么强制我们实现ViewHolder模式？

关于这个问题，我们首先看一下ListView。ListView是不强制我们实现ViewHolder的，但是后来google建议我们实现ViewHolder模式。我们先分别看一下这两种不同的方式。

![不使用viewholder](https://github.com/ZLOVE320483/DayDayUp/blob/main/pic/rv_cache1.jpg)