## Java泛型擦除

### Java泛型的实现方法：类型擦除

大家都知道，Java的泛型是伪泛型，这是因为Java在编译期间，所有的泛型信息都会被擦掉，正确理解泛型概念的首要前提是理解类型擦除。Java的泛型基本上都是在编译器这个层次上实现的，在生成的字节码中是不包含泛型中的类型信息的，使用泛型的时候加上类型参数，在编译器编译的时候会去掉，这个过程成为类型擦除。

如在代码中定义 ```List<Object>``` 和 ```List<String>``` 等类型，在编译后都会变成List，JVM看到的只是List，而由泛型附加的类型信息对JVM是看不到的。Java编译器会在编译时尽可能的发现可能出错的地方，但是仍然无法杜绝在运行时刻出现的类型转换异常的情况。

### 举例证明类型擦除

例1：原始类型相等
```
    public static void main(String[] args) {

        ArrayList<String> list1 = new ArrayList<String>();
        list1.add("abc");

        ArrayList<Integer> list2 = new ArrayList<Integer>();
        list2.add(123);

        System.out.println(list1.getClass() == list2.getClass());
    }
```

在这个例子中，我们定义了两个ArrayList数组，不过一个是 ```ArrayList<String>``` 泛型类型的，只能存储字符串；一个是 ```ArrayList<Integer>``` 泛型类型的，只能存储整数，最后，我们通过list1对象和list2对象的getClass()方法获取他们的类的信息，最后发现结果为true。说明泛型类型String和Integer都被擦除掉了，只剩下原始类型。

例2：通过反射添加其它类型元素
```
    public static void main(String[] args) throws Exception {

        ArrayList<Integer> list = new ArrayList<Integer>();

        list.add(1);  //这样调用 add 方法只能存储整形，因为泛型类型的实例为 Integer

        list.getClass().getMethod("add", Object.class).invoke(list, "asd");

        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i));
        }
    }
```
在程序中定义了一个ArrayList泛型类型实例化为Integer对象，如果直接调用add()方法，那么只能存储整数数据，不过当我们利用反射调用add()方法的时候，却可以存储字符串，这说明了Integer泛型实例在编译之后被擦除掉了，只保留了原始类型。

### 原始类型

原始类型 就是擦除去了泛型信息，最后在字节码中的类型变量的真正类型，无论何时定义一个泛型，相应的原始类型都会被自动提供，类型变量擦除，并使用其限定类型（无限定的变量用Object）替换。

例3: 原始类型Object
```
public class Pair<T> {
    
    private T value;

    public void setValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }
}
```
Pair的原始类型为:
```
public class Pair {
    
    private Object value;

    public void setValue(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}
```
因为在 ```Pair<T>``` 中，T 是一个无限定的类型变量，所以用Object替换，其结果就是一个普通的类，如同泛型加入Java语言之前的已经实现的样子。在程序中可以包含不同类型的Pair，如 ```Pair<String>``` 或 ```Pair<Integer>```，但是擦除类型后他们的就成为原始的Pair类型了，原始类型都是Object。

从上面的例2中，我们也可以明白ArrayList被擦除类型后，原始类型也变为Object，所以通过反射我们就可以存储字符串了。

如果类型变量有限定，那么原始类型就用第一个边界的类型变量类替换。

比如: Pair这样声明的话:

``` public class Pair<T extends Comparable> {}  ```

那么原始类型就是Comparable。

要区分原始类型和泛型变量的类型。

在调用泛型方法时，可以指定泛型，也可以不指定泛型。

在不指定泛型的情况下，泛型变量的类型为该方法中的几种类型的同一父类的最小级，直到Object
在指定泛型的情况下，该方法的几种类型必须是该泛型的实例的类型或者其子类。

```
public class Main {

    public static void main(String[] args) {
        
        /**不指定泛型的时候*/
        int i = add(1, 2); //这两个参数都是Integer，所以T为Integer类型
        Number f = add(1, 1.2); //这两个参数一个是Integer，以风格是Float，所以取同一父类的最小级，为Number
        Object o = add(1, "assd"); //这两个参数一个是Integer，以风格是Float，所以取同一父类的最小级，为Object

        /**指定泛型的时候*/
        int a = Main.<Integer>add(1, 2); //指定了Integer，所以只能为Integer类型或者其子类
        int b = Main.<Integer>add(1, 2.2); //编译错误，指定了Integer，不能为Float
        Number c = Main.<Number>add(1, 2.2); //指定为Number，所以可以为Integer和Float
    }

    public static <T> T add(T x, T y) {
        return y;
    }

}
```

其实在泛型类中，不指定泛型的时候，也差不多，只不过这个时候的泛型为Object，就比如ArrayList中，如果不指定泛型，那么这个ArrayList可以存储任意的对象。

例4: Object泛型
```
public static void main(String[] args) {  
    ArrayList list = new ArrayList();  
    list.add(1);  
    list.add("121");  
    list.add(new Date());  
}  
```