## LinkedHashMap源码解析
#### 本文将要分析的问题
1. LinkedHashMap 与 HashMap 的关系
2. LinkedHashMap 双向链表的构建过程
3. LinkedHashMap 删除节点的过程
4. LinkedHashMap 如何维持访问顺序
5. LinkedHashMap - LRU (Least Recently Used) 最简单的构建方式

#### LinkedHashMap 和 HashMap 的关系
先上一张 Map 系的体系图：

![Map体系图](https://github.com/ZLOVE320483/DayDayUp/blob/main/pic/linkedhashmap1.png)
