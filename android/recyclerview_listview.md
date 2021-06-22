## RecyclerView VS ListView

### 简单使用

ListView 的实现

1. 继承重写BaseAdapter类；
2. 自定义ViewHolder与convertView的优化（判断是否为null）；

```

public class TestAdapter extends ArrayAdapter {
  public TestAdapter(@NonNull Context context, int resource) {
    super(context, resource);
    resourceId = resource;
  }
  @NonNull
  @Override
  public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
    // 获取当前项的数据实例
    Fruit fruit = getItem(position);
    View view;
    ViewHolder viewHolder;
    // convertView是将我们加载好的布局进行缓存
    if (convertView == null) {
      view = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);
      viewHolder = new ViewHolder();
      viewHolder.textView = view.findViewById(R.id.fruit_name);
      view.setTag(viewHolder);
    } else {
      view = convertView;
      viewHolder = (ViewHolder) view.getTag();
    }
    viewHolder.textView.setText(fruit.getName());
    return view;
  }
  // 定义内部类
  class ViewHolder {
    TextView textView;
  }
}

......

      // listView 与 adapter 关联

        FruitAdapter adapter = new FruitAdapter(MainActivity.this,R.layout.fruit_item,fruitList);
        ListView listView = findViewById(R.id.list_view);
        listView.setAdapter(adapter);
```

RecyclerView 的实现

1. 继承重写RecyclerView.Adapter与RecyclerView.ViewHolder
2. 设置LayoutManager，以及layout的布局效果

```

// 第一步：继承重写 RecyclerView.Adapter 和 RecyclerView.ViewHolder
public class AuthorRecyclerAdapter extends RecyclerView.Adapter<AuthorRecyclerAdapter.AuthorViewHolder> {
    ...
    @Override
    public AuthorViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ...
        return viewHolder;
    }
    @Override
    public void onBindViewHolder(AuthorViewHolder holder, int position) {
        ...
    }
    @Override
    public int getItemCount() {
        if (mData == null) {
            return 0;
        }
        return mData.size();
    }
    class AuthorViewHolder extends RecyclerView.ViewHolder {
        ...
        public AuthorViewHolder(View itemView) {
            super(itemView);
            ...
        }
    }
}
mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
mRecyclerAdapter = new AuthorRecyclerAdapter(mData);
// 第二步：设置布局管理器，控制布局效果
LinearLayoutManager linearLayoutManager = new LinearLayoutManager(RecyclerDemoActivity.this);
linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
mRecyclerView.setLayoutManager(linearLayoutManager);
mRecyclerView.setAdapter(mRecyclerAdapter);
```

对比看到：

- ViewHolder 的编写规范化，ListView 是需要自己定义的，而RecyclerView是规范好的；
- RecyclerView复用item全部搞定，不需要想ListView那样setTag()与getTag()；
- RecyclerView多了一些LayoutManager工作，但实现了布局效果多样化。

### 布局效果

- ListView 的布局比较单一，只有一个纵向效果；
- RecyclerView 的布局效果丰富， 可以在 LayoutMananger 中设置：线性布局（纵向，横向），表格布局，瀑布流布局；
- 在RecyclerView 中，如果存在的 LayoutManager 不能满足需求，可以自定义 LayoutManager。

### Item 点击事件

- RecyclerView不支持 item 点击事件，只能用回调接口来设置点击事件
- ListView的 item 点击事件直接是setOnItemClickListener

### 局部刷新

- 在ListView中通常刷新数据是用notifyDataSetChanged() ，但是这种刷新数据是全局刷新的（每个item的数据都会重新加载一遍），这样一来就会非常消耗资源；
- RecyclerView中可以实现局部刷新，例如：notifyItemChanged()；
- 如果要在ListView实现局部刷新，依然是可以实现的，当一个item数据刷新时，我们可以在Adapter中，实现一个notifyItemChanged()方法，在方法里面通过这个 item 的 position，刷新这个item的数据

### 缓存区别

- 层级不同
> ListView有两级缓存，在屏幕与非屏幕内。mActivityViews + mScrapViews
>
> RecyclerView比ListView多两级缓存：支持开发者自定义缓存处理逻辑，RecyclerViewPool(缓存池)。并且支持多个离屏ItemView缓存（缓存屏幕外2个 itemView）。 mAttachedScrap + mCacheViews + mViewCacheExtension + mRecyclerPool

- 缓存内容不同
> ListView缓存View。 RecyclerView缓存RecyclerView.ViewHolder


- RV优势
> mCacheViews的使用，可以做到屏幕外的列表项ItemView进入屏幕内时也无须bindView快速重用；
>
> mRecyclerPool可以供多个RecyclerView共同使用，在特定场景下，如viewpaper+多个列表页下有优势
>
> 客观来说，RecyclerView在特定场景下对ListView的缓存机制做了补强和完善

### 动画效果
- 在RecyclerView中，已经封装好API来实现自己的动画效果；并且如果我们需要实现自己的动画效果，我们可以通过相应的接口实现自定义的动画效果（RecyclerView.ItemAnimator类），然后调用RecyclerView.setItemAnimator() (默认的有SimpleItemAnimator与DefaultItemAnimator）
- 但是ListView并没有实现动画效果，但我们可以在Adapter自己实现item的动画效果

### 嵌套滚动机制
- 在事件分发机制中，Touch事件在进行分发的时候，由父View向子View传递，一旦子View消费这个事件的话，那么接下来的事件分发的时候，父View将不接受，由子View进行处理；但是与Android的事件分发机制不同，嵌套滚动机制（Nested Scrolling）可以弥补这个不足，能让子View与父View同时处理这个Touch事件，主要实现在于NestedScrollingChild与NestedScrollingParent这两个接口；而在RecyclerView中，实现的是NestedScrollingChild，所以能实现嵌套滚动机制；
- ListView就没有实现嵌套滚动机制
