## CoordinateLayout 和 AppbarLayout 联动原理
下图是CoordinatorLayout布局中很常见的一种效果，很多人应该都见过，当我们用手指滑动RecyclerView的时候，不单止RecyclerView会上下滑动，顶部的Toolbar也会随着RecyclerView的滑动隐藏或显现，实现代码的布局如下：


![类加载过程](https://github.com/ZLOVE320483/DayDayUp/blob/main/pic/co1.gif)

具体代码：

```
<?xml version="1.0" encoding="utf-8"?>
<android.support.design.widget.CoordinatorLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
 
    <android.support.design.widget.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">
 
        <android.support.v7.widget.Toolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="?attr/colorPrimary"
            app:layout_scrollFlags="scroll|enterAlways|snap"
            android:theme=
              "@style/ThemeOverlay.AppCompat.Dark.ActionBar"
            app:popupTheme=
            "@style/ThemeOverlay.AppCompat.Light" />
    </android.support.design.widget.AppBarLayout>
 
    <android.support.v7.widget.RecyclerView
        android:id="@+id/recycler_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior=
        "@string/appbar_scrolling_view_behavior" />

</android.support.design.widget.CoordinatorLayout>
```
只要父布局是CoordinatorLayout，然后在Toolbar的外层包上一个AppBarLayout,在Toolbar上添加属性```layout_scrollFlags=”scroll|enterAlways|snap”```，在RecyclerView上添加属性```layout_behavior=”@string/appbar_scrolling_view_behavior”```，并把AppBarLayout与RecyclerView作为CoordinatorLayout的子控件，就能实现。

实现的方法知道了，但是我们不能单纯满足于此，接下来我们对原理进行分析

实现以上效果主要是涉及了嵌套滑动机制和Behavior两个知识点。

### 嵌套滑动机制（NestedScrolling）

根据事件分发机制，我们知道触摸事件最终只会由一个控件进行处理，当我们滑动RecyclerView时，事件最终肯定是传给了RecyclerView，并交给它进行处理，Toolbar是不应该能够接收到事件并响应的。我们无法依靠默认的事件分发机制完成gif图上的效果的（当然，我们通过自定义View，修改事件分发是可以实现这个效果）。

因此Google给我们提供了嵌套滑动机制。通过嵌套滑动机制，RecyclerView能够把自身接受到的点击滑动事件传递给父布局CoordinatorLayout，然后CoordinatorLayout把接收到的事件传递给子布局AppBarLayout（Toolbar的父布局），最终滑动事件交给了AppBarLayout进行处理，完成使Toolbar滚出滚进界面等效果。

这里 NestedScrolling 两个重要的概念提及一下

- NestedScrollingParent NestedScrollingParentHelper

- NestedScrollingChild NestedScrollingChildHelper

巧合的是 CoordinatorLayout 已经实现了 NestedScrollingParent 接口，所以我们配合一个实现了 NestedScrollingChild 接口的 View 就可以轻松的实现以上效果

一般而言，父布局会实现NestedScrollingParent，而滑动列表作为子控件实现NestedScrollingChild，并把事件传给父布局，父布局再根据情况把事件分发到其它子View。而NestedScrollingParentHelper和NestedScrollingChildHelper分别是NestedScrollingParent和NestedScrollingChild的辅助类，具体的逻辑会委托给它们执行。

接下来我们看一下CoordinatorLayout和RecyclerView的源码:

```
public class CoordinatorLayout extends ViewGroup implements NestedScrollingParent {
 
    ......
 
｝
public class RecyclerView extends ViewGroup implements ScrollingView, NestedScrollingChild {
 
    ......
 
｝
```

通过源码可发现CoordinatorLayout实现了NestedScrollingParent，而RecyclerView实现了NestedScrollingChild。毫无疑问，RecyclerView就是通过嵌套滑动机制把滑动事件传给了CoordinatorLayout，然后CoordinatorLayout把事件传递到AppBarLayout中。
那么实现这些接口需要实现哪些方法呢？我们通过源码来了解下：