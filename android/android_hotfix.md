## Android Robust 热修复原理解析

### 热修复框架现状

目前热修复框架主要有QQ空间补丁、HotFix、Tinker、Robust等。热修复框架按照原理大致可以分为三类：

- 基于 multidex机制 干预 ClassLoader 加载dex
- native 替换方法结构体
- instant-run 插桩方案

QQ空间补丁和Tinker都是使用的方案一； 阿里的AndFix使用的是方案二； 美团的Robust使用的是方案三。

1. QQ空间补丁原理

把补丁类生成 patch.dex，在app启动时，使用反射获取当前应用的ClassLoader，也就是 BaseDexClassLoader，反射获取其中的pathList，类型为DexPathList， 反射获取其中的Element[] dexElements, 记为elements1;然后使用当前应用的ClassLoader作为父ClassLoader，构造出 patch.dex 的 DexClassLoader,通用通过反射可以获取到对应的Element[] dexElements，记为elements2。将elements2拼在elements1前面，然后再去调用加载类的方法loadClass。


> **隐藏的技术难点 CLASS_ISPREVERIFIED 问题**
> apk在安装时会进行dex文件进行验证和优化操作。这个操作能让app运行时直接加载odex文件，能够减少对内存占用，加快启动速度，如果没有odex操作，需要从apk包中提取dex再运行。
在验证过程，如果某个类的调用关系都在同一个dex文件中，那么这个类会被打上CLASS_ISPREVERIFIED标记，表示这个类已经预先验证过了。但是再使用的过程中会反过来校验下，如果这个类被打上了CLASS_ISPREVERIFIED但是存在调用关系的类不在同一个dex文件中的话，会直接抛出异常。
为了解决这个问题，QQ空间给出的解决方案就是，准备一个 AntilazyLoad 类，这个类会单独打包成一个 hack.dex，然后在所有的类的构造方法中增加这样的代码：
```
if (ClassVerifier.PREVENT_VERIFY) {
   System.out.println(AntilazyLoad.class);
}
```
>复制代码这样在 odex 过程中，每个类都会出现 AntilazyLoad 在另一个dex文件中的问题，所以odex的验证过程也就不会继续下去，这样做牺牲了dvm对dex的优化效果了。
