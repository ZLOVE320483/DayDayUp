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

图片很直接的说明了一个问题，那就是 ```LinkedHashMap``` 直接继承自 ```HashMap``` ，这也就说明了上文中我们说到的 ```HashMap``` 一切重要的概念 ```LinkedHashMap``` 都是拥有的，这就包括了，hash 算法定位、hash 桶位置，哈希表由数组和单链表构成，并且当单链表长度超过 8 的时候转化为红黑树，扩容体系，这一切都跟 ```HashMap``` 一样。那么除了这么多关键的相同点以外，```LinkedHashMap``` 比 ```HashMap``` 更加强大，这体现在：

- ```LinkedHashMap``` 内部维护了一个双向链表，解决了 ```HashMap``` 不能随时保持遍历顺序和插入顺序一致的问题

- ```LinkedHashMap``` 元素的访问顺序也提供了相关支持，也就是我们常说的 LRU（最近最少使用）原则

#### LinkedHashMap 双向链表的构建过程

为了便于理解，在看具体源码之前，我们先看一张图，这张图可以很好的体现 LinkedHashMap 中个各个元素关系：

![LinkedHashMap元素关系](https://github.com/ZLOVE320483/DayDayUp/blob/main/pic/linkedhashmap2.png)

> 假设图片中红黄箭头代表元素添加顺序，蓝箭头代表单链表各个元素的存储顺序。head 表示双向链表头部，tail 代表双向链表尾部

上篇文章分析的 ```HashMap``` 源码的时候我们有一张示意图，说明了 ```HashMap``` 的存储结构为，数组 + 单链表 + 红黑树，从上边的图片我们也可以看出 ```LinkedHashMap``` 底层的存储结构并没有发生变化。

唯一变化的是使用双向链表（图中红黄箭头部分）记录了元素的添加顺序，我们知道 ```HashMap``` 中的 Node 节点只有 next 指针，对于双向链表而言只有 next 指针是不够的，所以 ```LinkedHashMap``` 对于 Node 节点进行了拓展：

```
static class Entry<K,V> extends HashMap.Node<K,V> {
   Entry<K,V> before, after;
   Entry(int hash, K key, V value, Node<K,V> next) {
       super(hash, key, value, next);
   }
}
```

```LinkedHashMap``` 基本存储单元 ```Entry<K,V>``` 继承自 ```HashMap.Node<K,V>```,并在此基础上添加了 before 和 after 这两个指针变量。这 before 变量在每次添加元素的时候将会链接上一次添加的元素，而上一次添加的元素的 after 变量将指向该次添加的元素，来形成双向链接。值得注意的是 ```LinkedHashMap``` 并没有覆写任何关于 ```HashMap``` put 方法。所以调用 ```LinkedHashMap``` 的 put 方法实际上调用了父类 ```HashMap``` 的方法。为了方便理解我们这里放一下 ```HashMap``` 的 putVal 方法：

```

final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
              boolean evict) {

   Node<K,V>[] tab; Node<K,V> p; int n, i;

   if ((tab = table) == null || (n = tab.length) == 0)
       n = (tab = resize()).length;
   if ((p = tab[i = (n - 1) & hash]) == null)
       tab[i] = newNode(hash, key, value, null);
   else {// 发生 hash 碰撞了
       Node<K,V> e; K k;
       if (p.hash == hash &&
           ((k = p.key) == key || (key != null && key.equals(k))))
           e = p;
       else if (p instanceof TreeNode){....}
       else {
          //hash 值计算出的数组索引相同，但 key 并不同的时候 循环整个单链表
           for (int binCount = 0; ; ++binCount) {
               if ((e = p.next) == null) {//遍历到尾部
                    // 创建新的节点，拼接到链表尾部
                   p.next = newNode(hash, key, value, null);
                   ....
                   break;
               }
               //如果遍历过程中找到链表中有个节点的 key 与 当前要插入元素的 key 相同，
               //此时 e 所指的节点为需要替换 Value 的节点，并结束循环
               if (e.hash == hash &&
                   ((k = e.key) == key || (key != null && key.equals(k))))
                   break;
               //移动指针
               p = e;
           }
       }
       //如果循环完后 e!=null 代表需要替换e所指节点 Value
       if (e != null) {
           V oldValue = e.value//保存原来的 Value 作为返回值
           // onlyIfAbsent 一般为 false 所以替换原来的 Value
           if (!onlyIfAbsent || oldValue == null)
               e.value = value;
           afterNodeAccess(e);//该方法在 LinkedHashMap 中的实现稍后说明
           return oldValue;
       }
   }
   //操作数增加
   ++modCount;
   //如果 size 大于扩容阈值则表示需要扩容
   if (++size > threshold)
       resize();
   afterNodeInsertion(evict);
   return null;
}
```

可以看出每次添加新节点的时候实际上是调用 newNode 方法生成了一个新的节点，放到指定 hash 桶中,但是很明显，```HashMap``` 中 newNode 方法无法完成上述所讲的双向链表节点的间的关系，所以 ```LinkedHashMap``` 复写了该方法：

```
// HashMap newNode 中实现
Node<K,V> newNode(int hash, K key, V value, Node<K,V> next) {
    return new Node<>(hash, key, value, next);
}

// LinkedHashMap newNode 的实现
Node<K,V> newNode(int hash, K key, V value, Node<K,V> e) {
    LinkedHashMap.Entry<K,V> p =
        new LinkedHashMap.Entry<K,V>(hash, key, value, e);
    // 将 Entry 接在双向链表的尾部
    linkNodeLast(p);
    return p;
}
```

可以看出双向链表的操作一定在 linkNodeLast方法中实现：

```
/**
* 该引用始终指向双向链表的头部
*/
transient LinkedHashMap.Entry<K,V> head;

/**
* 该引用始终指向双向链表的尾部
*/
transient LinkedHashMap.Entry<K,V> tail;
```

```
// newNode 中新节点，放到双向链表的尾部
private void linkNodeLast(LinkedHashMap.Entry<K,V> p) {
    // 添加元素之前双向链表尾部节点
   LinkedHashMap.Entry<K,V> last = tail;
   // tail 指向新添加的节点
   tail = p;
   //如果之前 tail 指向 null 那么集合为空新添加的节点 head = tail = p
   if (last == null)
       head = p;
   else {
       // 否则将新节点的 before 引用指向之前当前链表尾部
       p.before = last;
       // 当前链表尾部节点的 after 指向新节点
       last.after = p;
   }
}
```