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

```
public interface NestedScrollingChild {
 
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed);
 
    public boolean dispatchNestedPreFling(float velocityX, float velocityY);
 
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow);
 
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed,
            int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow);
 
    public boolean hasNestedScrollingParent();
 
    public boolean isNestedScrollingEnabled();
 
    public void setNestedScrollingEnabled(boolean enabled);
 
    public boolean startNestedScroll(int axes);
 
    public void stopNestedScroll();
 
｝
 
public interface NestedScrollingParent {
 
    public int getNestedScrollAxes();
 
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed);
 
    public boolean onNestedPreFling(View target, float velocityX, float velocityY);
 
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed);
 
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed,
            int dxUnconsumed, int dyUnconsumed);
 
    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes);
 
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes);
 
    public void onStopNestedScroll(View target);
 
｝
```

我整理的是NestedScrollingChild&NestedScrollingParent接口，26版本又添加了新的方法分别继承自NestedScrollingChild&NestedScrollingParent接口 ，看起来要实现的方法很多，也很复杂的样子，但是实质上通过辅助类NestedScrollingChildHelper和NestedScrollingParentHelper能大大减轻工作量，而且有些方法仅仅是作一个判断，并不需要很复杂的逻辑。在后面的源码验证环节中我们也只会着重分析到重点的几个方法。

在这里先说几个比较重要的方法的调用流程与对应关系：

1. NestedScrollingChild接口的startNestedScroll会在Down事件触发的时候调用，对应NestedScrollingParent的onStartNestedScroll。

2. NestedScrollingChild接口的dispatchNestedPreScroll会在Move事件触发的时候调用，对应NestedScrollingParent的onNestedPreScroll。

3. NestedScrollingChild接口的dispatchNestedScroll会在Move事件触发的时候调用，对应NestedScrollingParent的onNestedScroll。

4. NestedScrollingChild接口的stopNestedScroll会在Up事件触发的时候调用，对应NestedScrollingParent的onStopNestedScroll。

### 深入理解 Behavior

- 拦截 Touch 事件

当我们为一个 CoordinatorLayout 的直接子 View 设置了 Behavior 时，这个 Behavior 就能拦截发生在这个 View 上的 Touch 事件，那么它是如何做到的呢？实际上， CoordinatorLayout 重写了 onInterceptTouchEvent() 方法，并在其中给 Behavior 开了个后门，让它能够先于 View 本身处理 Touch 事件。

具体来说， CoordinatorLayout 的 onInterceptTouchEvent() 方法中会遍历所有直接子 View ，对于绑定了 Behavior 的直接子 View 调用 Behavior 的 onInterceptTouchEvent() 方法，若这个方法返回 true， 那么后续本该由相应子 View 处理的 Touch 事件都会交由 Behavior 处理，而 View 本身表示懵逼，完全不知道发生了什么。

CoordinatorLayout 的onInterceptTouchEvent 方法：

```
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        MotionEvent cancelEvent = null;
 
        final int action = ev.getActionMasked();
 
        // Make sure we reset in case we had missed a previous important event.
        if (action == MotionEvent.ACTION_DOWN) {
        // 先让子 view 种包含Behavior的控件 处理触摸事件
            resetTouchBehaviors();
        }
 
        final boolean intercepted = performIntercept(ev, TYPE_ON_INTERCEPT);
 
        if (cancelEvent != null) {
            cancelEvent.recycle();
        }
 
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            resetTouchBehaviors();
        }
 
        return intercepted;
    }
```
resetTouchBehaviors 方法内部实现：

```
      private void resetTouchBehaviors() {
        if (mBehaviorTouchView != null) {
            final Behavior b = ((LayoutParams) mBehaviorTouchView.getLayoutParams()).getBehavior();
            if (b != null) {
                final long now = SystemClock.uptimeMillis();
                final MotionEvent cancelEvent = MotionEvent.obtain(now, now,
                        MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);
                b.onTouchEvent(this, mBehaviorTouchView, cancelEvent);
                cancelEvent.recycle();
            }
            mBehaviorTouchView = null;
        }
 
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            lp.resetTouchBehaviorTracking();
        }
        mDisallowInterceptReset = false;
    }
```

- 拦截测量及布局

了解了 Behavior 是怎养拦截 Touch 事件的，想必大家已经猜出来了它拦截测量及布局事件的方式 —— CoordinatorLayout 重写了测量及布局相关的方法并为 Behavior 开了个后门。没错，真相就是如此。

CoordinatorLayout 在 onMeasure() 方法中，会遍历所有直接子 View ，若该子 View 绑定了一个 Behavior ，就会调用相应 Behavior 的 onMeasureChild() 方法，若此方法返回 true，那么 CoordinatorLayout 对该子 View 的测量就不会进行。这样一来， Behavior 就成功接管了对 View 的测量。

同样，CoordinatorLayout 在 onLayout() 方法中也做了与 onMeasure() 方法中相似的事，让 Behavior 能够接管对相关子 View 的布局。

我们可以通过Behaviour观察我们感兴趣的控件的事件，并作出相应的操作。

通过在xml中添加layout_behavior属性可以给控件设置Behaviour，比如在上面的代码中，就是在RecyclerView中添加属性

```
layout_behavior="@string/appbar_scrolling_view_behavior"
```

将RecyclerView的Behaviour指定成AppBarLayout的内部类ScrollingViewBehavior。

或者通过注解的方式给控件设置Behaviour，比如AppBarLayout就是通过

```
@CoordinatorLayout.DefaultBehavior(AppBarLayout.Behavior.class)
```

定义自身的Behavior为AppBarLayout.Behavior

注意的是，Behavior是CoordinatorLayout的专属属性，设置Behavior的控件需要是CoordinatorLayout的子控件。

在我们上面的事例代码中一共设置有两个Behavior，第一个就是RecyclerView中通过layout_behavior属性进行设置的ScrollingViewBehavior，第二个就是AppBarLayout的代码中通过注解默认设置的一个AppBarLayout.Behavior.class。

当我们要依赖另一个view的状态变化，例如大小、显示、位置状态，我们至少应该重写以下两个方法：

```
 public boolean layoutDependsOn(CoordinatorLayout parent, V child, View dependency) {
      return false;
 }
 public boolean onDependentViewChanged(CoordinatorLayout parent, V child, View dependency) {
     return false;
 }
```
第一个方法负责决定依赖哪个View，第二个方法负责根据依赖的View的变化做出响应。

我们的示例中给RecycleView设置的ScrollingViewBehavior也实现了这两个方法，使得RecycleView一直处于AppBarLayout的下方。

当我们要依赖某个实现了NestedScrollingChild的View的滑动状态时，应该重写以下方法：

```
        public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout,
                V child, View directTargetChild, View target, int nestedScrollAxes) {
            return false;
        }
        public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, V child, View target,
                int dx, int dy, int[] consumed) {
            // Do nothing
        }
        public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, V child, View target,
                float velocityX, float velocityY) {
            return false;
        }
```

onStartNestedScroll决定Behavior是否接收嵌套滑动机制传过来的事件；onNestedPreScroll负责接收依赖的View滑动的滑动事件并处理；onNestedPreFling负责接收快速滑动时的惯性滑动事件并处理。

我们的事例中AppBarLayout通过注解设置的AppBarLayout.Behavior实现了这3个方法，使得AppBarLayout能够接收到RecycleView传来的滑动事件并响应。

### 联动分析

我们滑动RecyclerView的时候，RecyclerView会通过滑动嵌套机制把接收到的事件传给CoordinatorLayout，然后CoordinatorLayout把事件传给AppBarLayout,AppBarLayout再根据自身的Behavior（AppBarLayout.Behavior.class）做相应的处理，判断是否处理该滑动事件，如果不处理，则事件仍交还给RecyclerView，如果处理，就做出相应的操作，例如将Toolbar滚出或者滚进屏幕，并消耗掉需要的滑动事件。

这时候可能会有人有疑问：当AppBarLayout处理并消耗了RecyclerView传递的滑动事件的时候（既Toolbar上下滑动时），RecyclerView为什么也还能跟随着手指上下移动呢？其实这里RecyclerView并不是跟随着手指移动，而是一直保持在AppBarLayout的正下方。这是因为我们在RecyclerView中添加属性『layout_behavior="@string/appbar_scrolling_view_behavior"』

给RecyclerView指定了AppBarLayout$ScrollingViewBehavior，这个Behavior会观察AppBarLayout，当AppBarLayout发生变化时做出相应的操作。正是因为这样，就算RecyclerView把滑动事件交给AppBarLayout处理并消耗掉，它也还能一直保持在AppBarLayout的正下方。

总结：当我们滑动RecyclerView时，Toolbar能上下滚动是由嵌套滑动机制和AppBarLayout.Behavior共同工作完成的。而在Toolbar上下滚动时，RecyclerView也能始终保持在其正下方的功能是由ScrollingViewBehavior实现的。


