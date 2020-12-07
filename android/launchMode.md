## Android Activity LaunchMode详解
### Activity 的四种launchMode
1. standard
```
standard是Activity默认的启动模式，在不进行显示指定的情况下，所有活动都会自动使用这种启动模式。
每次启动都一个新的Activity位于栈顶。
android:launchMode="standard",此时每次点击按钮都会创建新的Activity
```
   
2. singleTop
```
当Activity的启动模式为singleTop时,当启动的Activity已经处于Activity栈顶时，则直接使用。
android:launchMode="singleTop" 
```

3. singleInstance
```
singleInstance模式下会有一个单独的返回栈来管理活动。不管哪个应用程序来访问该活动，都共用同一个栈，这样就可以允许其他程序调用，实现共享该活动。
android:launchMode="singleInstance"
```

4. singleTask
```
当活动的启动模式为singleTask时，启动该Activity会现在栈中检查是否已存在，若存在则直接将该活动之上的Activity全部出栈。
android:launchMode="singleTask"
```