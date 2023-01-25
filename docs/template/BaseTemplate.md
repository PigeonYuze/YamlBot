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
- 2个参数
    1. 随机数的起点(包括该项) : `Int`
    2. 随机数的起点(包括该项) : `Int`
- 3个参数
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