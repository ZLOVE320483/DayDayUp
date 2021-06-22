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