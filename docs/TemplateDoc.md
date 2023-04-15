# Template文档

关于`Template`相关文档的快捷链接

- [BaseTemplate](./template/BaseTemplate.md)
- [FeaturesTemplate](./template/FeaturesTemplate.md)
- [HttpTemplate](./template/HttpTemplate.md)
- [MessageTemplate](./template/MessageTemplate.md)
- [MiraiTemplate](./template/MiraiTemplate.md)
- [UserTemplate](./template/UserTemplate.md)
- [MessageManagerTemplate](./template/MessageManagerTemplate.md)

在调用一个`Template`的参数列表中，默认是以`yaml`配置中的字符串进行传递的

而自版本`v1.1.0`以后，你可以直接使用yaml中的语法提供参数，**部分函数并不支持这方面的使用**

如果参数要求中含有以下字样

- 需要`SE`
- 需要`no-yaml`

则说明不支持直接采用`yaml`语法的`List`or`Map`

在传递参数中经常会遇到一些无法使用字符串表达的内容，此时就需要用`%call-xx%`的方法获取对应的值用于调用

在下文中将介绍相关的传递方法

---

## List

### 由字符串获取`List`

一般来说，不管是否开启`no-yaml`，在参数既需要`List`,又只存储有字符串时，本功能就会被自动调用

在默认状况下，会同时裁剪开头与结尾的字符

例如:
> [hello,world]

就会得到一个内容为`hello`与`world`的集合

而有时并不会这样，反而会得到`[hello`与`world]`的集合

通常只有指定参数，同时可以使用字符串与集合的状况下采取第二种方法

所以如果你要以字符串表达`List`集合，请在开头与结尾加上其他字符

在转换中，会以每一个半角符号`,`进行分割

如果分割后的内容包含有一个小括号，会在下一次出现对应的括号时，将内容连接在一起

举个例子

`[normal_text,left(te,st)right,end_text]` 经过处理后会得到以下的效果

| 下标  |        内容        |
|:---:|:----------------:|
|  0  |   normal_text    |
|  1  | left(te,st)right |
|  2  |     end_text     |

### 由Yaml直接获取`List`

该功能默认打开，当标注`no-yaml`时，无法使用

在`yaml`以`- value`格式即可表达`List`

举个例子

 ```yaml
 list: #下面的内容才是表达的方法 此处只是为了便于阅读
  - 1
  - 2
  - 3
 ```

就会得到一个内容如下的`List`

| 下标  | 内容  |
|:---:|:---:|
|  0  |  1  |
|  1  |  2  |
|  2  |  3  |

---

## Map

### 由字符串获取`Map`

同样的，不管是否开启`no-yaml`，在参数既需要`Map`,又只存储有字符串时，本功能就会被自动调用

`Map`的解析其根本与`List`相差不大，他们都以`,`表达每一个元素的分割，默认截取开头与结尾的字符

不管是什么格式，都以`=`前后表达`key`与`value`，`==`也同样起着相同作用

例如：使用`a=b`经过处理后`key`=a,`value`=b

同样举个例子

 ```text
 [a=b,c=d,key==value]
 ```

处理后的结果如下:

| 下标  | Key | Value |
|:---:|:---:|:-----:|
|  0  |  a  |   b   |
|  1  |  c  |   d   |
|  2  | key | value |

### 由Yaml直接获取`Map`

该功能默认打开，当标注`no-yaml`时，无法使用

可参照以下内容进行编写 若要将其加入至配置内新的内容请在第一行开头删除一个空格字符

```yaml
map: #下面的内容才是表达的方法 此处只是为了便于阅读
 a: b #相比list不需要编写'-'
 c: d #使用半角冒号加一个空格': '来表达key与value
 key: value
```

得到的内容与上文一致

