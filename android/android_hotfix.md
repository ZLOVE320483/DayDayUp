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

2. Tinker 原理

对于Tinker，修复前和修复后的apk分别定义为apk1和apk2，tinker自研了一套dex文件差分合并算法，在生成补丁包时，生成一个差分包 patch.dex，后端下发patch.dex到客户端时，tinker会开一个线程把旧apk的class.dex和patch.dex合并，生成新的class.dex并存放在本地目录上，重新启动时，会使用本地新生成的class.dex对应的elements替换原有的elements数组。

3. AndFix 原理

AndFix的修复原理是替换方法的结构体。在native层获取修复前类和修复后类的指针，然后将旧方法的属性指针指向新方法。由于不同系统版本下的方法结构体不同，而且davilk与art虚拟机处理方式也不一样，所以需要针对不同系统针对性的替换方法结构体。

```
// AndFix 代码目录结构
jni
├─ Android.mk
├─ Application.mk
├─ andfix.cpp
├─ art
│  ├─ art.h
│  ├─ art_4_4.h
│  ├─ art_5_0.h
│  ├─ art_5_1.h
│  ├─ art_6_0.h
│  ├─ art_7_0.h
│  ├─ art_method_replace.cpp
│  ├─ art_method_replace_4_4.cpp
│  ├─ art_method_replace_5_0.cpp
│  ├─ art_method_replace_5_1.cpp
│  ├─ art_method_replace_6_0.cpp
│  └─ art_method_replace_7_0.cpp
├─ common.h
└─ dalvik
   ├─ dalvik.h
   └─ dalvik_method_replace.cpp
```

### 美团 Robust 热修复方案原理

下面，进入今天的主题，Robust热修复方案。首先，介绍一下 Robust 的实现原理。

以 State 类为例

```
public long getIndex() {
    return 100L;
}
```

插桩后的 State 类

```
public static ChangeQuickRedirect changeQuickRedirect;
public long getIndex() {
    if(changeQuickRedirect != null) {
        //PatchProxy中封装了获取当前className和methodName的逻辑，并在其内部最终调用了changeQuickRedirect的对应函数
        if(PatchProxy.isSupport(new Object[0], this, changeQuickRedirect, false)) {
            return ((Long)PatchProxy.accessDispatch(new Object[0], this, changeQuickRedirect, false)).longValue();
        }
    }
    return 100L;
}
```

我们生成一个 StatePatch 类, 创一个实例并反射赋值给 State 的 changeQuickRedirect 变量。

```
public class StatePatch implements ChangeQuickRedirect {
    @Override
    public Object accessDispatch(String methodSignature, Object[] paramArrayOfObject) {
        String[] signature = methodSignature.split(":");
        // 混淆后的 getIndex 方法 对应 a
        if (TextUtils.equals(signature[1], "a")) {//long getIndex() -> a
            return 106;
        }
        return null;
    }

    @Override
    public boolean isSupport(String methodSignature, Object[] paramArrayOfObject) {
        String[] signature = methodSignature.split(":");
        if (TextUtils.equals(signature[1], "a")) {//long getIndex() -> a
            return true;
        }
        return false;
    }
}
```

当我们执行出问题的代码 getState 时，会转而执行 StatePatch 中逻辑。这就 Robust 的核心原理，由于没有干扰系统加载dex过程,所以这种方案兼容性最好。

### Robust 实现细节

Robust 的实现方案很简单，如果只是这么简单了解一下，有很多细节问题，我们不去接触就不会意识到。 Robust 的实现可以分成三个部分：插桩、生成补丁包、加载补丁包。下面先从插桩开始。

- 插桩

Robust 预先定义了一个配置文件 robust.xml，在这个配置文件可以指定是否开启插桩、哪些包下需要插桩、哪些包下不需要插桩，在编译 Release 包时，RobustTransform 这个插件会自动遍历所有的类，并根据配置文件中指定的规则，对类进行以下操作：

1. 类中增加一个静态变量 ChangeQuickRedirect changeQuickRedirect
2. 在方法前插入一段代码，如果是需要修补的方法就执行补丁包中的方法，如果不是则执行原有逻辑。

常用的字节码操纵框架有：

	ASM
	AspectJ
	BCEL
	Byte Buddy
	CGLIB
	Cojen
	Javassist
	Serp

美团 Robust 分别使用了ASM、Javassist两个框架实现了插桩修改字节码的操作。个人感觉 javaassist 更加容易理解一些，下面的代码分析都以 javaassist 操作字节码为例进行阐述。

```
for (CtBehavior ctBehavior : ctClass.getDeclaredBehaviors()) {
    // 第一步： 增加 静态变量 changeQuickRedirect
    if (!addIncrementalChange) {
        //insert the field
        addIncrementalChange = true;
        // 创建一个静态变量并添加到 ctClass 中
        ClassPool classPool = ctBehavior.getDeclaringClass().getClassPool();
        CtClass type = classPool.getOrNull(Constants.INTERFACE_NAME);  // com.meituan.robust.ChangeQuickRedirect
        CtField ctField = new CtField(type, Constants.INSERT_FIELD_NAME, ctClass);  // changeQuickRedirect
        ctField.setModifiers(AccessFlag.PUBLIC | AccessFlag.STATIC);
        ctClass.addField(ctField);
    }
    // 判断这个方法需要修复
    if (!isQualifiedMethod(ctBehavior)) {
        continue;
    }
    // 第二步： 方法前插入一段代码 ...
}
```

对于方法前插入一段代码，

```
// Robust 给每个方法取了一个唯一id
methodMap.put(ctBehavior.getLongName(), insertMethodCount.incrementAndGet());
try {
    if (ctBehavior.getMethodInfo().isMethod()) {
        CtMethod ctMethod = (CtMethod) ctBehavior;
        boolean isStatic = (ctMethod.getModifiers() & AccessFlag.STATIC) != 0;
        CtClass returnType = ctMethod.getReturnType();
        String returnTypeString = returnType.getName();
        // 这个body 就是要塞到方法前面的一段逻辑
        String body = "Object argThis = null;";
        // 在 javaassist 中 $0 表示 当前实例对象，等于this
        if (!isStatic) {
            body += "argThis = $0;";
        }
        String parametersClassType = getParametersClassType(ctMethod);
        // 在 javaassist 中 $args 表达式代表 方法参数的数组，可以看到 isSupport 方法传了这些参数：方法所有参数，当前对象实例，changeQuickRedirect，是否是静态方法，当前方法id，方法所有参数的类型，方法返回类型
        body += "   if (com.meituan.robust.PatchProxy.isSupport($args, argThis, " + Constants.INSERT_FIELD_NAME + ", " + isStatic +
                ", " + methodMap.get(ctBehavior.getLongName()) + "," + parametersClassType + "," + returnTypeString + ".class)) {";
        // getReturnStatement 负责返回执行补丁包中方法的代码
        body += getReturnStatement(returnTypeString, isStatic, methodMap.get(ctBehavior.getLongName()), parametersClassType, returnTypeString + ".class");
        body += "   }";
        // 最后，把我们写出来的body插入到方法执行前逻辑
        ctBehavior.insertBefore(body);
    }
} catch (Throwable t) {
    //here we ignore the error
    t.printStackTrace();
    System.out.println("ctClass: " + ctClass.getName() + " error: " + t.getMessage());
}
```

再来看看 getReturnStatement 方法，

```
 private String getReturnStatement(String type, boolean isStatic, int methodNumber, String parametersClassType, String returnTypeString) {
        switch (type) {
            case Constants.CONSTRUCTOR:
                return "    com.meituan.robust.PatchProxy.accessDispatchVoid( $args, argThis, changeQuickRedirect, " + isStatic + ", " + methodNumber + "," + parametersClassType + "," + returnTypeString + ");  ";
            case Constants.LANG_VOID:
                return "    com.meituan.robust.PatchProxy.accessDispatchVoid( $args, argThis, changeQuickRedirect, " + isStatic + ", " + methodNumber + "," + parametersClassType + "," + returnTypeString + ");   return null;";
            // 省略了其他返回类型处理
        }
 }
```

```PatchProxy.accessDispatchVoid``` 最终调用了 ```changeQuickRedirect.accessDispatch```。

至此插桩环节就结束了。