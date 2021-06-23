## RecyclerView缓存机制

使用 ScrollView 的时候，它的所有子 view 都会一次性被加载出来。而正确使用 RecyclerView 可以做到按需加载，按需绑定，并实现复用。本文主要分析 RecyclerView 缓存复用的原理。
