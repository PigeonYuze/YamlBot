## ReflectionTemplate

|   use调用命名    |             类别              | 
|:------------:|:---------------------------:|
| `REFLECTION` | JVM中的反射功能，支持`Java`和`Kotlin` |

本模板支持对`jvm`的反射功能，你可以使用此项反射`Java class`或`Kotlin class`

不过，在调用此模板时你需要严格思考这个类是`Java class`还是`Kotlin class`

本函数内置有对`java`与`kotlin`两种语言的反射支持

通常来说，你**必须**根据源语言来选择你所需要的功能

为了加快运行速度，在获取`class`与`final`的值时，会**通过`Map`缓存内容**

### ClearCache

如上，此模板在运行时会选择缓存一部分内容

调用本函数以清楚这些缓存

|    name    | returnType |
|:----------:|:----------:|
| clearCache |    Unit    |

**本函数不需要任何参数**

----
**以下是对`Java`反射功能的介绍**

当您所想要反射的目标为`java class`时，你应该选择以下函数考虑

值得注意的是：`Java`反射实现中的每一项函数都将无视可见级别。

即： 当要获取的对象为`private`时，你依旧可以成功获取。

### JavaReflectionConstruct

反射并尝试构建一个类，返回构建后得到的对象

|     name      | returnType |
|:-------------:|:----------:|
| constructJava |    Any     |

**参数说明**

1. 构建类的完整名称，`包名+类名`的名称。如`java.util.Date`
2. 构建函数的参数，你可以不提供参数，此函数会选择不需要参数的构造方法进行构造类

**操作举例**

假设有以下`java class`

```java
package com.pigeonyuze;

public class Test {
    public Test() {
        System.out.println("Hello,this is construct");
        /* Code */
    }

    public Test(int a) {
        System.out.println("Hello,this is construct a!");
        /* Code */
    }

    public Test(int a, String b) {
        System.out.println("Hello,this is construct a and b!");
        /* Code */
    }
}
```

使用以下内容来尝试构建时，会得到以下输出

```yaml
args:
  - 'com.pigeonyuze.Test' 
```

**输出**
> Hello,this is construct

由此可见，在没有提供第二项参数时选择了无参数的构建方法

反之，如果为:

```yaml
args:
  - 'com.pigeonyuze.Test'
  - - 114514
    - "哼哼哼，啊啊啊啊啊" 
```

**输出**
> Hello,this is construct a and b!

可见，第二项参数会决定函数具体会调用哪一个构建方法

### JavaReflectionMethod

反射调用一个`java class`内的方法，并返回得到的结果。

|  name  | returnType |
|:------:|:----------:|
| method |    Any     |

**参数说明**

1. 方法名称
2. 调用方法的参数
   方法来源
   <br>可以为一个对象(`%call-name`) 或者为对象的名称
   <br> 当为名称时**只可以调用**`static`方法

**举例说明**

假设有以下`java class`

```java
package com.pigeonyuze;

import java.util.Random;

public class Test {
    public Test(Object obj) {
        System.out.println("Hello,this is construct a!");
        /* Code */
    }

    public int random() {
        return new Random().nextInt();
    }

    public static void printoutStatic() {
        System.out.println("Static");
    }

    private void printoutHelloWorld() {
        System.out.println("Hello World");
    }
}
```

使用以下`yaml`配置参数分别可以得到不同的内容

```yaml
args:
  - random
  - '[]'
  - '%call-testObject%' # 此处仅供参考 变量`testObject`属于`Test`类型
```

输出返回得到的内容:
> 1791680175

可以看到成功调用，可是如果我们稍加修改，将对象改为名称呢？

```yaml
args:
  - random
  - '[]'
  - 'com.pigeonyuze.Test'
```

此时再尝试运行会发现会抛出一个错误：

> **com.pigeonyuze.util.TaskException: Cannot call method random, it must be static!**

可见无法成功调用方法，但是如果我们将对象方法名称由`name`改为`printoutStatic`即可成功运行

一般来说，本函数也可以调用被修饰了`private`的方法，如上文中的`printoutHelloWorld`方法

这是因为在内部实现中，**始终**会进行`method.setAccessible(true)`的操作

不过也正因为如此，在**修改可见度时可能会抛出**`InaccessibleObjectException`, 这是在`JDK 9`新增的协议：

`Java` 内部会严格遵循指定模块所规定的可见度，尝试更改时**可能会导致错误**

**只要你不访问一些`jdk`内部不支持访问的方法就不会抛出此类错误**

你可以添加**JVM 运行选项**来防止错误发生，不过在本文并不详细阐述

可参照： [Stackoverflow About Question](https://stackoverflow.com/questions/41265266/)

### JavaReflectionField

获取指定对象的字段，返回字段内容 **必须提供实例**

|   name    | returnType |
|:---------:|:----------:|
| fieldJava |    Any     |

**参数说明**

1. 字段名称
2. 字段所属对象实例

**举例说明**

设有以下类

```java
package com.pigeonyuze;

import java.util.Random;

public class Test {
    public int i = 0;

    private final String obj = "";
}
```

可使用以下代码获取指定的字段

```yaml
args:
  - i
  - '%call-testObject%' # 此处仅供参考 变量`testObject`属于`Test`类型
```

输出结果得到：
> 0

同样的，该项也可以访问私有字段`obj`，此处不多展示

### JavaReflectionSetField

修改一个类中的字段，返回是否修改成功

**当对应字段为`final`时即不会主动修改字段，直接返回`false`**

如需修改`final`字段请使用`setFinalFieldJava`

|     name     | returnType |
|:------------:|:----------:|
| setFieldJava |  Boolean   |

**参数说明**

1. 字段名称
2. 字段所属的对象实例
3. 新的值 **需要与原本的值属于统一类型**

**举例说明**

设有以下类

```java
package com.pigeonyuze;

import java.util.Random;

public class Test {
    public int i = 0;

    public final String obj = "";
}
```

当选择

```yaml
args:
  - i
  - '%call-testObject%' # 此处仅供参考 变量`testObject`属于`Test`类型
  - 114514 
```

会返回`true`，并重新访问`i`的值会更改为`114514`

如果为

```yaml
args:
  - obj
  - '%call-testObject%' # 此处仅供参考 变量`testObject`属于`Test`类型
  - "new string"
```

会访问`false`，表明修改并未修改成功，重新访问会发现依旧是`""`

### JavaReflectionSetFinalField

强制修改一个字段，这个字段可以是`final`或`static final`

但当对应字段为`static final`时，此函数会尝试反射至`java.lang.reflect.Field`并修改`modifiers`使其可以访问修改

不过这在`jdk 9`及以上版本中是不允许的操作

在不同的`jdk`版本运行可能会出现不一样的操作效果

例如：

- 在`JDK Oracle OpenJdk version 17.0.1`中 会直接抛出`java.lang.reflect.InaccessibleObjectException`
- 在`JDK Eclipse Temurin version 11.0.1`中 不会执行操作，并在控制台输出`WARNING: XXXX`字样

这取决于你当前系统所运行的`Java`版本，如果在`Java <= 8`的环境下即可以正常运行

当函数捕获到异常`java.lang.reflect.InaccessibleObjectException`时，会使用`mirai logger`记录错误

内容为: `"The specified field could not be modified because a InaccessibleObjectException error occurred while trying to modify 'getDeclaredFields0' by reflection,
Please add '--add-opens java.base/java.lang=ALL-UNNAMED' to JVM running options"`

你可以尝试在运行`mcl`的脚本尝试运行`java -jar`时添加`--add-opens java.base/java.lang=ALL-UNNAMED`

运行后，返回重新反射访问该字段后得到的结果。

|       name        | returnType |
|:-----------------:|:----------:|
| setFinalFieldJava |    Any     |

**参数说明**

1. 字段名称
2. 字段所属对象实例
3. 新的值

**举例说明**

此项在修改非`final`字段时与`setFieldJava`差异不大

但是在修改`final`字段时，可以直接修改其内容，并同步更新缓存内的时

同时还可以设置被`static final`修饰的字段，但是有时此功能并不会工作

如有以下字段:

```java
package com.pigeonyuze;

import java.util.Random;

public class Test {
    public static final int i = 0;

    public final String obj = "";

    private final String privateStr = "private";
}
```

你可以直接修改其中的`obj`和`privateStr`两个字段

理论上你也可以修改`i`字段，但是**可能会修改错误**，如上文所说。

----
**以下是对`Kotlin`反射功能的介绍**

当您所想要反射的目标为`kotlin class`时(例如：`Any.kt`)，你应该选择以下函数考虑

在`kotlin`代码中的`val`与`var`在`kotlin`中属于`Property`，
以下文档中描述使用**字段**代替以便于阅读。

### KotlinRefectionProperty

**不支持扩展字段，当遇到了扩展字段则会抛出错误**

读取一个类内所含的所有`val`或`var`中`get`的值，返回反射得到的值

此处只会读取一个`object`或构造函数无参数的字段

这是因为，每一次调用都会创建一个**新的实例**

|      name      | returnType |
|:--------------:|:----------:|
| propertyKotlin |    Any     |

**参数说明**

1. 字段名称
2. 类的完整名称, 如`com.pigeonyuze.Name`

**举例说明**

若有以下`kotlin class`

```kotlin
package com.pigeonyuze

object TestObject {
    const val constValue = 0
    var normalVarValue = ""
}

class TestNormalObject {
    val random: Int = (0..1000).random()
}
```

我们可以尝试调用以上字段，尝试调用获取`TestObject`中的`const val constValue`的值

```yaml
args:
  - constValue
  - 'com.pigeonyuze.TestObject' 
```

可以看到成功获取了其中的值，输出后得到结果`0`

可是我们如果尝试多次获取`TestNormalObject`中的`val random`字段呢？

```yaml
args:
  - random
  - 'com.pigeonyuze.TestNormalObject' 
```

我们第一次调用得到：
> out: 251

可如果再次调用呢？
> out: 831

我们会发现，这样每一次的结果都不一样，这是因为`val random`字段是由
`(0..1000).random()`生成的

而如果是指同一个实例（例如：`object`），则每一次输出的结果都一致

这是因为在调用时此函数**每次调用都会创造一个新的实例**
而像`object`这样的单例类，只存在有一个实例，故此不会改变

### KotlinRefectionInstanceProperty

**不支持扩展字段，当遇到了扩展字段则会抛出错误**

访问所给予实例的字段，需要自行提供实例

|          name          | returnType |
|:----------------------:|:----------:|
| instancePropertyKotlin |    Any     |

**参数说明**

1. 字段名称
2. 对象实例

### KotlinRefectionSetProperty

**不支持扩展字段，当遇到了扩展字段则会抛出错误**

访问并设置一个`var`字段的值

此操作等同于调用`kotlin property`中自动生成的`set`函数，它在声明时看起来是这样的：

```kotlin
var mutableProperty: Any
    get() = TODO()
    set(value) {
        /* Code */
        this = value //修改值，默认以此修改
    }
```

`kotlin`对字段的修改是根据以上的访问器以访问并修改

返回是否修改成功，当修改失败时返回`false`

|       name        | returnType |
|:-----------------:|:----------:|
| setPropertyKotlin |  Boolean   |

**参数说明**

1. 字段名称
2. 类的完整名称, 如`com.pigeonyuze.Name`
3. 新的值

### KotlinRefectionSetInstanceProperty

**不支持扩展字段，当遇到了扩展字段则会抛出错误**

由一个实例修改其中的字段，需要提供对应实例

|           name            | returnType |
|:-------------------------:|:----------:|
| instanceSetPropertyKotlin |  Boolean   |

**参数说明**

1. 字段名称
2. 实例
3. 新的值

### KotlinReflectionFunction

反射调用一个类的函数

**需要**调用函数的**实例**

|   name   | returnType |
|:--------:|:----------:|
| function |    Any     |

**参数说明**

1. 函数名称 <br> 在运行时使用函数的名称或者`JvmName`匹配
2. 参数
3. 目标实例
4. 扩展函数的接收器 **可选**

**举例声明**

有以下`kotlin class`

```kotlin
package com.pigeonyuze

class FunctionRef {
    fun hello() {
        println("Hello,")
    }
    fun world(join: String) {
        print("World $join! ")
    }

    fun FunctionRef.helloWorld() {
       hello()
       world("")
    }
}


```

假设我们有一个实例`obj`

现在我们尝试依次调用这些函数

`hello`函数:

```yaml
args:
   - hello
   - '%call-obj%'
   - ""
```

> out: Hello,

`world`函数:

```yaml
args:
   - world
   - '%call-obj%'
   - "*Parameter join*" 
```

> out: World *Parameter join*!

`helloWorld`扩展函数:

```yaml
args:
   - helloWorld
   - '%call-obj%'
   - ""
   - '%call-obj%' #扩展函数接受器
```

> out: Hello,World !

### KotlinReflectionConstruct

反射构造函数并构建类，返回构造后得到的实例

|      name       | returnType |
|:---------------:|:----------:|
| constructKotlin |    Any     |

**参数说明**

1. 类的完整名称, 如`com.pigeonyuze.Name`
2. 构造函数的参数 **可选，不提供时选择不需要参数的构建函数**

**举例说明**

```kotlin
package com.pigeonyuze

class ConstructTest {
   constructor() {
      println("0. Hello!")
   }

   constructor(string: String) {
      println("1. Hello $string")
   }
}
```

首先我们尝试不提供参数

```yaml
args:
   - com.pigeonyuze.ConstructTest
```

> out: 0. Hello!

再尝试调用第二项构建函数

```yaml
   - com.pigeonyuze.ConstructTest
   - "World"
```

> out: 1. Hello World

