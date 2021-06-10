## Android中的ClassLoader

Android中的ClassLoader和java中的是不一样的，因为java中的ClassLoader主要加载Class文件，但是Android中的ClassLoader主要加载dex文件。

### Android中的ClassLoader

Android中的ClassLoader分为两种类型，系统类加载器，自定义类加载器。其中系统的类加载器分为三种，BootClassLoader，PathClassLoader，DexClassLoader。

- BootClassLoader

Android 系统启动时，会用BootClassLoader来预加载常用类，与java中的ClassLoader不同，他不是用C++实现，而是用java实现的,如下：

```
class BootClassLoader extends ClassLoader {

    private static BootClassLoader instance;

    @FindBugsSuppressWarnings("DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED")
    public static synchronized BootClassLoader getInstance() {
        if (instance == null) {
            instance = new BootClassLoader();
        }

        return instance;
    }
...

```

BootClassLoader是ClassLoader的内部类，并继承自ClassLoader，BootClassLoader是一个单例类，需要注意的是BootClassLoader是默认修饰符，只能包内访问，我们是无法使用的。

- DexClassLoader

DexClassLoader可以加载dex文件和包含dex文件的压缩文件（比如jar和apk文件），不管加载那种文件，最终都是加载dex文件，我们看一下代码：

```
public class DexClassLoader extends BaseDexClassLoader {
    public DexClassLoader(String dexPath, String optimizedDirectory, String librarySearchPath, ClassLoader parent) {
        super((String)null, (File)null, (String)null, (ClassLoader)null);
        throw new RuntimeException("Stub!");
    }
}
```

DexClassLoader有四个参数

	1. dexPath：dex相关文件的路径集合，多个文件用路径分割符分割，默认的文件分割符为 ":"
	2. optimizedDirectory:解压的dex文件储存的路径，这个路径必须是一个内部储存路径，一般情况下使用当钱应用程序的私有路径/data/data/<Package Name>/...
	3. librarySearchPath:包含C++库的路径集合，多个路径用文件分割符分割，可以为null
	4. parent：父加载器

DexClassLoade继承自BaseDexClassLoader,所有的实现都是在BaseDexClassLoader中

- PathClassLoader

Android 用PathClassLoader来加载系统类和应用程序类，代码如下：

```
public class PathClassLoader extends BaseDexClassLoader {
    public PathClassLoader(String dexPath, ClassLoader parent) {
        super((String)null, (File)null, (String)null, (ClassLoader)null);
        throw new RuntimeException("Stub!");
    }

    public PathClassLoader(String dexPath, String librarySearchPath, ClassLoader parent) {
        super((String)null, (File)null, (String)null, (ClassLoader)null);
        throw new RuntimeException("Stub!");
    }
}
```

PathClassLoader继承自BaseDexClassLoader,所有的实现都是在BaseDexClassLoader中

PathClassLoader构造方法没有optimizedDirectory参数，因为PathClassLoader默认optimizedDirectory参数是/data/dalvik-cache，很显然PathClassLoader无法定义解压的dex储存的位置，因此PathClassLoader通常用来加载已经安装的apk的dex文件（安装的apk的dex文件在/data/dalvik-cache中）

### ClassLoder的继承关系

运行一个应用程序需要几个ClassLoader呢？

```
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ClassLoader classLoader = MainActivity.class.getClassLoader();
        while (classLoader != null) {
            Log.d("mmmClassLoader", classLoader.toString()+"\n");
            classLoader = classLoader.getParent();
        }

    }
}
```

看下log

```
mmmClassLoader: dalvik.system.PathClassLoader[DexPathList[[zip file "/data/app/com.baidu.bpit.aibaidu.idl3-2/base.apk"],
nativeLibraryDirectories=[/data/app/com.baidu.bpit.aibaidu.idl3-2/lib/arm64, /data/app/com.baidu.bpit.aibaidu.idl3-2/base.apk!/lib/arm64-v8a, /system/lib64, /vendor/lib64, /system/vendor/lib64, /product/lib64]]]
 
java.lang.BootClassLoader@fcb14c9
```

我们看到主要用了俩个ClassLoader，分别是PathClassLoader和BootClassLoader，其中DexPathList包含了很多apk的路径，其中/data/app/com.baidu.bpit.aibaidu.idl3-2/base.apk就是实例应用安装在手机上的位置，DexPathList是在BaseDexClassLoder中创建的，里面储存dex相关文件的路径

除了上方的3中ClassLoader，Android还提供了其他的类加载器和ClassLoader相关类，继承关系如下：

![类继承关系](https://github.com/ZLOVE320483/DayDayUp/blob/main/pic/android_classloader_1.jpeg)

分别介绍一下上方的8种ClassLoader

> - ClassLoader是一个抽象类，其中定义了ClassLoder的主要功能，BootClassLoader是他的内部类
> -	SecureClassLoader和JDK8中的SecureClassLoader代码是一样的，他继承抽象类ClassLoader，SecureClassLoader并不是ClassLoader的实现类，而是扩展了ClassLoader权限方面的功能，加强了Classloader的安全性
> - URLClassLoader和JDK8中的URLClassLoader是一样的，他继承了SecureClassLoader通能过URL路径从jar文件中加载类和资源
> - InMemoryDexClassLoader他是Android8.0新加的类加载器，继承自BaseDexClassLoader，用于加载内存中的dex文件
> - BaseDexClassLoader继承自ClassLoader，是抽象类ClassLoader的具体实现类

### ClassLoader的加载过程

Android中的ClassLoader同样遵循双亲委托模型，ClassLoader的加载方法为loadClass,方法定义在ClassLoader中

```
protected Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException{
            // 首先检查类是否被加载过
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                try {
                    if (parent != null) {
                        c = parent.loadClass(name, false);
                    } else {
                        c = findBootstrapClassOrNull(name);
                    }
                } catch (ClassNotFoundException e) {
                    //如果父类抛出ClassNotFoundException异常
                    //则说明父类不能加载该类
                }

                if (c == null) {
                    //如果父类无法加载，则调用自身的findClass进行加
                    c = findClass(name);
                }
            }
            return c;
    }
```

上方逻辑很清楚，首先检查类是否被加载过，如果没有被加载过，就调用父类的加载器加载，如果父类加载器为空就调用启动加载器加载，如果父类加载失败，就调用自己的findClass加载,我们看一下这个方法

```
  protected Class<?> findClass(String name) throws ClassNotFoundException {
        throw new ClassNotFoundException(name);
    }
```
