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

RecyclerView的缓存分为四级

- **Scrap**
- **Cache**
- **ViewCacheExtension**
- **RecycledviewPool**

Scrap对应ListView 的Active View，就是屏幕内的缓存数据，就是相当于换了个名字，可以直接拿来复用。

Cache 刚刚移出屏幕的缓存数据，默认大小是2个，当其容量被充满同时又有新的数据添加的时候，会根据FIFO原则，把优先进入的缓存数据移出并放到下一级缓存中，然后再把新的数据添加进来。Cache里面的数据是干净的，也就是携带了原来的ViewHolder的所有数据信息，数据可以直接来拿来复用。需要注意的是，cache是根据position来寻找数据的，这个postion是根据第一个或者最后一个可见的item的position以及用户操作行为（上拉还是下拉）。
举个栗子：当前屏幕内第一个可见的item的position是1，用户进行了一个下拉操作，那么当前预测的position就相当于（1-1=0），也就是position=0的那个item要被拉回到屏幕，此时RecyclerView就从Cache里面找position=0的数据，如果找到了就直接拿来复用。

ViewCacheExtension是google留给开发者自己来自定义缓存的，这个ViewCacheExtension我个人建议还是要慎用，因为我扒拉扒拉网上其他的博客，没有找到对应的使用场景。

RecycledViewPool刚才说了Cache默认的缓存数量是2个，当Cache缓存满了以后会根据FIFO（先进先出）的规则把Cache先缓存进去的ViewHolder移出并缓存到RecycledViewPool中，RecycledViewPool默认的缓存数量是5个。RecycledViewPool与Cache相比不同的是，从Cache里面移出的ViewHolder再存入RecycledViewPool之前ViewHolder的数据会被全部重置，相当于一个新的ViewHolder，而且Cache是根据position来获取ViewHolder，而RecycledViewPool是根据itemType获取的，如果没有重写getItemType（）方法，itemType就是默认的。因为RecycledViewPool缓存的ViewHolder是全新的，所以取出来的时候需要走onBindViewHolder（）方法。

再来张图看看整体流程

![recyclerview缓存流程](https://github.com/ZLOVE320483/DayDayUp/blob/main/pic/rv_cache6.jpg)

这里大家先记住主要流程，并且记住各级缓存是根据什么拿到ViewHolder以及ViewHolder能否直接拿来复用，先有一个整体的认识，下面我会带着大家再简单分析一下RecyclerView缓存机制的源码。

### 阅读RecyclerView的缓存机制源码

由于篇幅和内容的关系，我不可能带大家一行一行读，这里我只列出关键点，还有哪些需要重点看，哪些可以直接略过，避免大家陷入读源码一个劲儿钻进去出不来的误区。

当RecyclerView绘制的时候，会走到LayoutManager里面的next()方法，在next()里面是正式开始使用缓存机制，这里以LinearLayoutManager为例子

```
        /**
         * Gets the view for the next element that we should layout.
         * Also updates current item index to the next item, based on {@link #mItemDirection}
         *
         * @return The next element that we should layout.
         */
        View next(RecyclerView.Recycler recycler) {
            if (mScrapList != null) {
                return nextViewFromScrapList();
            }
            final View view = recycler.getViewForPosition(mCurrentPosition);
            mCurrentPosition += mItemDirection;
            return view;
        }
```

在next方法里传入了Recycler对象，这个对象是RecyclerView的内部类。我们先去看一眼这个类

```
 public final class Recycler {
        final ArrayList<ViewHolder> mAttachedScrap = new ArrayList<>();
        ArrayList<ViewHolder> mChangedScrap = null;

        final ArrayList<ViewHolder> mCachedViews = new ArrayList<ViewHolder>();

        private final List<ViewHolder>
                mUnmodifiableAttachedScrap = Collections.unmodifiableList(mAttachedScrap);

        private int mRequestedCacheMax = DEFAULT_CACHE_SIZE;
        int mViewCacheMax = DEFAULT_CACHE_SIZE;

        RecycledViewPool mRecyclerPool;

        private ViewCacheExtension mViewCacheExtension;

        static final int DEFAULT_CACHE_SIZE = 2;
}
```

再看一眼RecycledViewPool的源码

```
public static class RecycledViewPool {
        private static final int DEFAULT_MAX_SCRAP = 5;
        static class ScrapData {
            final ArrayList<ViewHolder> mScrapHeap = new ArrayList<>();
            int mMaxScrap = DEFAULT_MAX_SCRAP;
            long mCreateRunningAverageNs = 0;
            long mBindRunningAverageNs = 0;
        }
        SparseArray<ScrapData> mScrap = new SparseArray<>();
```

其中mAttachedScrap对应Scrap；mCachedViews对应Cache；mViewCacheExtension对应ViewCacheExtension；mRecyclerPool对应RecycledViewPool。
注意：mAttachedScrap、mCachedViews和RecycledViewPool里面的mScrapHeap都是ArrayList，缓存被加入到这三个对象里面实际上就是调用的ArrayList.add()方法，复用缓存呢，这里要注意一下不是调用的ArrayList.get（）而是ArrayList.remove(),其实这里也很好理解，因为当缓存数据被取出来展示到了屏幕内，自然就应该被移除。
我们现在回到刚才的next（）方法里，recycler.getViewForPosition(mCurrentPosition); 直接去看getViewForPosition这个方法，接着跟到了这里

```
 View getViewForPosition(int position, boolean dryRun) {
            return tryGetViewHolderForPositionByDeadline(position, dryRun, FOREVER_NS).itemView;
        }
```

接着跟进去

```
  ViewHolder tryGetViewHolderForPositionByDeadline(int position,
                boolean dryRun, long deadlineNs) {
            if (position < 0 || position >= mState.getItemCount()) {
                throw new IndexOutOfBoundsException("Invalid item position " + position
                        + "(" + position + "). Item count:" + mState.getItemCount()
                        + exceptionLabel());
            }
            boolean fromScrapOrHiddenOrCache = false;
            ViewHolder holder = null;
            // 0) If there is a changed scrap, try to find from there
            if (mState.isPreLayout()) {
                holder = getChangedScrapViewForPosition(position);
                fromScrapOrHiddenOrCache = holder != null;
            }
            // 1) Find by position from scrap/hidden list/cache
            if (holder == null) {
                holder = getScrapOrHiddenOrCachedHolderForPosition(position, dryRun);
             
            }
            if (holder == null) {
                final int type = mAdapter.getItemViewType(offsetPosition);
                // 2) Find from scrap/cache via stable ids, if exists
                if (mAdapter.hasStableIds()) {
                    holder = getScrapOrCachedViewForId(mAdapter.getItemId(offsetPosition),
                            type, dryRun);
                    if (holder != null) {
                        // update position
                        holder.mPosition = offsetPosition;
                        fromScrapOrHiddenOrCache = true;
                    }
                }
                if (holder == null && mViewCacheExtension != null) {
                    // We are NOT sending the offsetPosition because LayoutManager does not
                    // know it.
                    final View view = mViewCacheExtension
                            .getViewForPositionAndType(this, position, type);
                    if (view != null) {
                        holder = getChildViewHolder(view);
                      
                    }
                }
                if (holder == null) { // fallback to pool
                    if (DEBUG) {
                        Log.d(TAG, "tryGetViewHolderForPositionByDeadline("
                                + position + ") fetching from shared pool");
                    }
                    holder = getRecycledViewPool().getRecycledView(type);
                    if (holder != null) {
                        holder.resetInternal();
                        if (FORCE_INVALIDATE_DISPLAY_LIST) {
                            invalidateDisplayListInt(holder);
                        }
                    }
                }
                if (holder == null) {
                    long start = getNanoTime();
                    if (deadlineNs != FOREVER_NS
                            && !mRecyclerPool.willCreateInTime(type, start, deadlineNs)) {
                        // abort - we have a deadline we can't meet
                        return null;
                    }
                    holder = mAdapter.createViewHolder(RecyclerView.this, type);
                    if (ALLOW_THREAD_GAP_WORK) {
                        // only bother finding nested RV if prefetching
                        RecyclerView innerView = findNestedRecyclerView(holder.itemView);
                        if (innerView != null) {
                            holder.mNestedRecyclerView = new WeakReference<>(innerView);
                        }
                    }
                }
            } 

            boolean bound = false;
            if (mState.isPreLayout() && holder.isBound()) {
                // do not update unless we absolutely have to.
                holder.mPreLayoutPosition = position;
            } else if (!holder.isBound() || holder.needsUpdate() || holder.isInvalid()) {
                if (DEBUG && holder.isRemoved()) {
                    throw new IllegalStateException("Removed holder should be bound and it should"
                            + " come here only in pre-layout. Holder: " + holder
                            + exceptionLabel());
                }
                final int offsetPosition = mAdapterHelper.findPositionOffset(position);
                bound = tryBindViewHolderByDeadline(holder, offsetPosition, position, deadlineNs);
            }  
            return holder;
        }
```