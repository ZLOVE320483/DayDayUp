## RecyclerView缓存机制

使用 ScrollView 的时候，它的所有子 view 都会一次性被加载出来。而正确使用 RecyclerView 可以做到按需加载，按需绑定，并实现复用。本文主要分析 RecyclerView 缓存复用的原理。

### 从缓存获取 ViewHolder 流程概览

从缓存获取的大致流程如下图所示：

![缓存获取流程](https://github.com/ZLOVE320483/DayDayUp/blob/main/pic/recyclerview_cache1.jpg)