## HTTP Template

| use调用命名 |    类别    | 
|:-------:|:--------:|
|  HTTP   | 网络请求相关功能 | 

### Downland

针对指定的网页进行下载网页内的内容 并返回下载到的路径

|   name   | returnType |
|:--------:|:----------:|
| downland |    File    |

**参数说明**

1. 网页链接
2. 下载到系统存储的路径(如：xxx/xxx/xx.png) **可选(默认为`mcl/data/httpHashCode`)**
3. 参数(如：a=b,c=d) **可选 你可以在`CommandReg`中直接使用`YamlMap`来表达你的参数列表(自1.1.0)**

### ApiContent

获取网址内容

|  name   | returnType |
|:-------:|:----------:|
| content |   String   |

**参数说明**

1. 网页链接
2. 参数(如： a=b,c=d) **可选 你可以在`CommandReg`中直接使用`YamlMap`来表达你的参数列表(自1.1.0)**

### ApiField

获取`Json`格式为内容的网页中的字段

值得注意的是 每提供调用一次 就会重新发送一个请求

并且如果网页内容不是`Json`格式的将调用失败

请谨慎考虑对本函数的调用

**该项未来可能会被重写**

| name  | returnType |
|:-----:|:----------:|
| field |   String   |

**参数说明**

1. 网页链接
2. 参数 **可选 你可以在`CommandReg`中直接使用`YamlMap`来表达你的参数列表(自1.1.0)**
3. 字段名或下标
