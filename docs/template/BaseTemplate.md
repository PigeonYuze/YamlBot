## BaseTemplate

| use调用命名 |   类别    | 
|:-------:|:-------:|
|  BASE   | 基础的模板支持 | 

### RandomText

随机获取文本

|    name    | returnType |
|:----------:|:----------:|
| randomText |   String   |

**参数说明**

无特别要求 `args`则为随机文本的所有内容

### Random

随机数

|  name  | returnType |
|:------:|:----------:|
| random |    Int     |

**参数说明**

- 无参数 - 由0到2147483647的随机数
- 1个参数
  1. 由0到此项的随机数 : `Int`
  1. 由0到此项的随机数 : `Int`
- 2个参数
  1. 随机数的起点(包括该项) : `Int`
  2. 随机数的起点(包括该项) : `Int`
  1. 随机数的起点(包括该项) : `Int`
  2. 随机数的起点(包括该项) : `Int`
- 3个参数
  1. 随机数的起点(包括该项) : `Int`
  2. 随机数的终点(包含该项) : `Int`
  3. 是否包含负数 : `true` or `false`
  1. 随机数的起点(包括该项) : `Int`
  2. 随机数的终点(包含该项) : `Int`
  3. 是否包含负数 : `true` or `false`

### CreateJson

创造一条`Json`信息

|    name    | returnType |
|:----------:|:----------:|
| createJson |   String   |

**参数说明**

第一个参数将决定生成的`Json`是`Array`样式或是`Map`的样式

- 如果包含有`'='` 则该项以`{`开头 接下来的参数都自动判断为`Map`(`key=value`)格式

- 如果不包含 则该项以`[`开头 接下来的参数都自动判断为`value`

其余的参数都遵循以下规则

- `=`会被解析为一个`Map`
- `[`开头,`]`结尾的会被解析为一个`Array`
- 一个参数内的内容可按照以上规则嵌套
- 如果内容为数字或布尔值类型的字符串会被解析为对应的类型而不带双引号

**Args 举例**

```yaml
args:
  - bool=true
  - map=key=value
  - array=[1,a,0.0,false]
```

输出：
> {"bool":true,"map":{"key":"value"},"array":[1,"a",0.0,false]}

```yaml
args:
  - true
  - key=value
  - [ 1,a,0.0,false ]
```

输出：
> [true,{"key":"value"},[1,"a",0.0,false]]

### ParseJson

|   name    | returnType |
|:---------:|:----------:|
| parseJson |   String   |

解析一条`Json`信息

**参数说明**

参数长度不限

但是第一个参数必须为解析的`Json`本体

其次是对`Json`的解析

- 如果是`Map`格式 则该项为`key`
- 如果为`Index`格式 则该项为`index`

**Args 举例**

```yaml
args:
  - { "bool": true,"map": { "key": "value" },"array": [ 1,"a",0.0,false ] }
  - map
  - key
```

输出：
> value

```yaml
args:
  - { "bool": true,"map": { "key": "value" },"array": [ 1,"a",0.0,false ] }
  - array
  - 0
```

输出：
> 1

### Switch

与`Java(jdk>11)`中的`switch`与`kotlin`的`when`相同

如果符合对应的项则运行，每一个项在运行后会自动`break`

|  name  | returnType |
|:------:|:----------:|
| switch |   String   |

**参数说明**

1. 值
2. 需要一个`Map`<br>`key`值为对应的值,`value`为返回的值<br>当未找到合适的项时选择`key`值为`#ELSE`的值

举个例子

```yaml
switch: #非标准写法/也许未来会支持这样
  "hello": "world!"
  "world": "hello,"
  "#ELSE": "hello,world!"
```

与以下代码相同

```kotlin
//kotlin
fun get(value: String) = when (value) {
        "hello" -> "world!"
        "world" -> "hello,"
        else -> "hello,world!"
    }
```

```java
//java
class Run {
    String get(String value) {
        return switch (value) {
            case "hello":
                "world!";
            case "world":
                "hello,";
            default:
                "hello,world!";
        };
    }
}
```

以上只供演示，实际上它还支持其他的类别，随后通过`equals`进行比较

### Equal

比较两则的值是否相同 **会比较`hashCode`**

| name  | returnType |
|:-----:|:----------:|
| equal |  Boolean   |

**参数说明**

提供两个参数，将这两个参数进行比较

如果相同返回`true`，反则为`false`

### MemoryEquals

比较两则的值是否相同 **会比较`hashCode`**

| name | returnType |
|:----:|:----------:|
| ===  |  Boolean   |

**参数说明**

提供两个参数，将这两个参数的内存地址进行比较

如果相同返回`true`，反则为`false`

### CompareTo

(**数字**) 大小比较的实现，你可用调用此项或者使用`<`,`>`,`==`更加方便的比较

|   name    | returnType |
|:---------:|:----------:|
| compareTo |    Int     |

建议使用以下支持的调用方式

| name | returnType |
|:----:|:----------:|
| `<`  |  Boolean   |
| `>`  |  Boolean   |
| `==` |  Boolean   |

#### 判断方法

- 如果二者为数字 则比较数字大小
- 如果二者为数组 比较项的数量
- 如果都不是比较`toString()`的字符串

**参数说明**

提供两个参数

### Switch

与`Java(jdk>11)`中的`switch`与`kotlin`的`when`相同

如果符合对应的项则运行，每一个项在运行后会自动`break`

|  name  | returnType |
|:------:|:----------:|
| switch |   String   |

**参数说明**

1. 值
2. 需要一个`Map`<br>`key`值为对应的值,`value`为返回的值<br>当未找到合适的项时选择`key`值为`#ELSE`的值

举个例子

```yaml
switch: #非标准写法/也许未来会支持这样
  "hello": "world!"
  "world": "hello,"
  "#ELSE": "hello,world!"
```

与以下代码相同

```kotlin
//kotlin
fun get(value: String) = when (value) {
        "hello" -> "world!"
        "world" -> "hello,"
        else -> "hello,world!"
    }
```

```java
//java
class Run {
    String get(String value) {
        return switch (value) {
            case "hello":
                "world!";
            case "world":
                "hello,";
            default:
                "hello,world!";
        };
    }
}
```

以上只供演示，实际上它还支持其他的类别，随后通过`equals`进行比较

### Equal

比较两则的值是否相同 **会比较`hashCode`**

| name  | returnType |
|:-----:|:----------:|
| equal |  Boolean   |

**参数说明**

提供两个参数，将这两个参数进行比较

如果相同返回`true`，反则为`false`

### MemoryEquals

比较两则的值是否相同 **会比较`hashCode`**

| name | returnType |
|:----:|:----------:|
| ===  |  Boolean   |

**参数说明**

提供两个参数，将这两个参数的内存地址进行比较

如果相同返回`true`，反则为`false`

### CompareTo

(**数字**) 大小比较的实现，你可用调用此项或者使用`<`,`>`,`==`更加方便的比较

|   name    | returnType |
|:---------:|:----------:|
| compareTo |    Int     |

建议使用以下支持的调用方式

| name | returnType |
|:----:|:----------:|
| `<`  |  Boolean   |
| `>`  |  Boolean   |
| `==` |  Boolean   |

#### 判断方法

- 如果二者为数字 则比较数字大小
- 如果二者为数组 比较项的数量
- 如果都不是比较`toString()`的字符串

**参数说明**

提供两个参数

