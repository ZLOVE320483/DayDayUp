## Java对象初始化顺序

### 代码示例
```
/**
 * 辅助打印
 */
public class Log {

    public static String baseStaticField() {
        System.out.println("基类静态成员字段...");
        return "";
    }

    public static String baseNormalField() {
        System.out.println("基类普通成员字段...");
        return "";
    }

    public static String staticField() {
        System.out.println("子类静态成员字段...");
        return "";
    }

    public static String normalField() {
        System.out.println("子类普通成员字段...");
        return "";
    }
}
```

```
/**
 * 基类
 */
public class Base {

    static {
        System.out.println("基类静态代码块...");
    }

    private static String basicStaticValue = Log.baseStaticField();

    static {
        System.out.println("基类静态代码块...222...");
    }

    {
        System.out.println("基类普通代码块...");
    }

    private String basicValue = Log.baseNormalField();

    {
        System.out.println("基类普通代码块...222...");
    }

    Base() {
        System.out.println("基类构造函数...");
    }

}
```

```
/**
 * 子类
 */
public class Derived extends Base {
    static {
        System.out.println("子类静态代码块...");
    }

    private static String staticValue = Log.staticField();

    static {
        System.out.println("子类静态代码块...222...");
    }

    {
        System.out.println("子类普通代码块...");
    }

    private String value = Log.normalField();

    {
        System.out.println("子类普通代码块...222...");
    }

    Derived() {
        System.out.println("子类构造函数...");
    }
}
```

```
public class TestMain {

    public static void main(String[] args) {
        Derived d = new Derived();
    }
}

```

打印结果：
```
基类静态代码块...
基类静态成员字段...
基类静态代码块...222...
子类静态代码块...
子类静态成员字段...
子类静态代码块...222...
基类普通代码块...
基类普通成员字段...
基类普通代码块...222...
基类构造函数...
子类普通代码块...
子类普通成员字段...
子类普通代码块...222...
子类构造函数...
```

### 结论
对象在class文件加载完毕，以及为各成员在方法区开辟好内存空间之后，就开始所谓“初始化”的步骤：
1. 基类静态代码块，基类静态成员字段 （并列优先级，按代码中出现先后顺序执行）（只有第一次加载类时执行）
2. 派生类静态代码块，派生类静态成员字段 （并列优先级，按代码中出现先后顺序执行）（只有第一次加载类时执行）
3. 基类普通代码块，基类普通成员字段 （并列优先级，按代码中出现先后顺序执行）
4. 基类构造函数
5. 派生类普通代码块，派生类普通成员字段 （并列优先级，按代码中出现先后顺序执行）
6. 派生类构造函数

#### 注意：静态过程，只在这个类第一次被加载的时候才运行。如果创建两个对象，静态过程是不会再执行的。
