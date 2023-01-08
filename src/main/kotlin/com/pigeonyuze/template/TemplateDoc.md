# Template文档

本文档用于展示所有`Template`及其参数说明

----
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
>{"bool":true,"map":{"key":"value"},"array":[1,"a",0.0,false]}

```yaml
args:
- true
- key=value
- [1,a,0.0,false]
```
输出：
>[true,{"key":"value"},[1,"a",0.0,false]]

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
- {"bool":true,"map":{"key":"value"},"array":[1,"a",0.0,false]}
- map
- key
```
输出：
>value

```yaml
args:
- {"bool":true,"map":{"key":"value"},"array":[1,"a",0.0,false]}
- array
- 0
```
输出：
>1
----
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
2. 下载到系统存储的路径(如：xxx/xxx/xx.png) **可选(默认为`mcl/data/http`)**
3. 参数(如：a=b,c=d) **可选**


### ApiContent
获取网址内容

|  name   | returnType |
|:-------:|:----------:|
| content |   String   |

**参数说明**

1. 网页链接
2. 参数(如： a=b,c=d) **可选**

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
2. 参数 **可选**
3. 字段名或下标
---
## UserTemplate

| use调用命名 |     类别      | 
|:-------:|:-----------:|
|  USER   | 用户系统的相关功能调用 |

**所有功能都需要`UserConfig-open`为`true`才可以正常调用**

### Value

获取一个参数的值 并返回这个值的字符串形式(`toString()`)

| name  | returnType |
|:-----:|:----------:|
| value |   String   |

**参数说明**

1. 查询对象的名称
2. 查询用户的qq号 **默认为发送者的qq号 可选**

### Reg

注册一个用户，返回是否成功注册

| name | returnType |
|:----:|:----------:|
| reg  |  Boolean   |

**该项不需要任何参数！**

### Set

设置一个用户中的一个值

| name | returnType |
|:----:|:----------:|
| reg  |    Unit    |

**参数说明**

有以下几种传参方式
1. 只含一种参数 <br> 效果：将发送者的对应值**格式化**
   1. 对应值的命名
2. 含有两个参数 <br> 效果：使一个人的对应值**格式化**
   1. 对应值的命名
   2. 要**格式化参数**的人的qq号
3. 含有两个参数 <br> 效果：将发送者的**对应值**修改为**新的对应值**
   1. 对应值的命名
   2. 新的值
4. 含有三个参数 <br> 效果：为**所有用户**修改值
   1. 对应值的命名
   2. 是否要这样做(`true`(**执行**)或`false`(**不执行**))
   3. 新的值
5. 含有三个参数 <br> 效果：向**一个人**修改**一个值**
   1. 对应值的命名
   2. 目标的**qq号**
   3. 新的值

### Plus

向一个用户的**值**进行**加法操作** 

| name | returnType |
|:----:|:----------:|
| plus |    Unit    |

以下简单阐述以下这个操作是如何进行的 (包括**接下来的减法操作也大多一样是按照该逻辑进行**)：

> 该操作作用于一个用户的值内
> - 如果是数字 则会通过`BigDecimal`进行**正常的加减操作**
> - 如果是集合 则是把两个集合叠在一起
> - 如果是`String`类型 就是把后者加至被加的字符串后方
>   - 如果是减法的话：则是把被减的值**替换为空**
> - 如果是`Boolean` 则如果一方为`true`得到的结果就是`true` **相同于或运算**
> - 如果是`Date` 则是把二者对应的世界戳相加 再重新生成一个`Date`
> - 其余的无法进行加减 但是基本都可以进行加减操作
>   - 如果出现了无法相加减的值则会抛出一个`IllegalStateException`错误
> 
>> 此处代码位于 `com/pigeonyuze/account/UserElement.kt  (86 line ~ 120 line)`

 
**参数说明**

同样的 这也有这不同的传参方法
- 两个参数 <br> 效果：向发生者的指定**值**进行增加
  1. 操作对应的值的命名
  2. 相加值
- 三个参数 <br> 效果：向指定用户的**值**进行增加
  1. 操作对应的值的命名
  2. 目标的**qq号**
  3. 相加值

### Minus

向一个用户的**值**进行**减法操作**

| name  | returnType |
|:-----:|:----------:|
| minus |    Unit    |

**参数说明**

同样的 这也有这不同的传参方法
- 两个参数 <br> 效果：向发生者的指定**值**减少
    1. 操作对应的值的命名
    2. 相减的值
- 三个参数 <br> 效果：向指定用户的**值**减少
    1. 操作对应的值的命名
    2. 目标的**qq号**
    3. 相减的值

### CallFunction

由一个用户的值调用**函数/方法**

该过程使用**反射**实现，内容由原生的`Java/Kotlin`内定义的函数实现

如果反射后得到的类为`Java类`则不支持`Kotlin`相关函数（如：扩展函数） 

返回调用该**函数/方法**返回的值 可能是`Unit`，但如果是`null`会**被转义为字符串形式**的`"null"`


|        name         | returnType |
|:-------------------:|:----------:|
| callElementFunction |    Any     |

**参数说明**

1. 指定者的**qq号**   **可选，默认为发送者的qq号**
2. 函数/方法名
3. 提供的参数 **不限 可直接添加`Args`列表内的值来提供 调用时提供的值**

---

## MiraiTemplate

| use调用命名 |       类别       | 
|:-------:|:--------------:|
|  MIRAI  | 对`mirai`相关功能支持 |


### Upload

上传一个资源 并返回可以直接操作的`Message`

如果要上传一个原文件为`.mp3`格式的语音，在上传时我们会自动转换为`.silk`再上传.

这期间产生的`.silk`文件会被保存至`data/.../silk/资源md5.silk`

**这需要`ffmpeg`的相关支持**

|  name  | returnType |
|:------:|:----------:|
| upload |  Message   |

**参数说明**

1. 上传的资源的路径
2. 上传类别 **可选 如果不提供也会自动判断该项** <br> 如果提供 请选择以下值
    - image
    - file
    - audio

### Send

向收到信息的群聊上传并发送一个**图片/文件/语音**信息

该项与`upload`逻辑同样 所以不支持富文本的发送 并且不支持其余的类别

**这需要`ffmpeg`的支持**

| name | returnType |
|:----:|:----------:|
| send |    Unit    |

**参数说明**

1. 发送的资源的路径
2. 发送资源的类别 **可选 如果不提供也会自动判断该项** <br> 如果提供 请选择以下值
    - image
    - file
    - audio


### Downland

由对方发送的信息中下载信息

**信息取自触发指令的同时 原信息内所包含的资源**

|   name   | returnType |
|:--------:|:----------:|
| downland |    Unit    |


**参数说明**

你可以不提供任何参数 会自动将信息中可下载的元素保存到`data/.../$(CURRENT_TIME)_call_downland`

1. 保存到的路径(包括扩展名)
2. 保存的类别**可选 运行时会自动判断**
   - image
   - audio
   - file

### EventValue

获取当前信息事件的信息

| name  | returnType |
|:-----:|:----------:|
| value |    Any     |

**参数说明**

1.参数名称

- `message` <br> 信息的`toString()`，输出一个类似`MiraiCode`而不是的类型，有时这里也会是`Json`格式的信息
- `messageString` <br> 接近官方形式的信息
- `messageMiraiCode` <br> 信息的`MiraiCode`形式，如果不支持会返回空的字符串
- `messageJson` <br> 信息以`Json`方式进行序列化，所有的信息都支持该操作
- `time` <br> 消息发送时间戳, 单位为秒
- `date` <br> 信息发送时的`Date`
- `senderName` <br> 发送人名称. 由群员发送时为群员名片, 由好友发送时为好友昵称
- `senderId` <br> 发送人qq号
- `senderRemark` <br> `Bot` 与发送者的备注信息
  <br> 仅 `Bot` 与 `User` 存在好友关系的时候才可能存在备注
  <br> `Bot` 与 `User` 没有好友关系时永远为空字符串 (`""`)
- `senderAvatarUrl` <br> 发送人的头像下载链接
- `senderAge` <br> 发送人个人资料中设定的年龄
- `senderLevelQQ` <br> 发送人的qq等级
- `senderEmail` <br> 发送人个人资料内备注的电子邮件 如果没有填写则为空字符串 (`""`)
- `senderNick` <br> 发送人的昵称
- `senderSex` <br> 发送人的性别
  <br>如果是男 则为 `MALE`<br>如果是女 则为 `FEMALE`<br>如果设定为保密 则为 `UNKNOWN`
- `senderSign` <br> 发送人的个性签名
- `subjectId` <br> 聊天环境的号码
  <br> 如果为群聊则是群号 反之则与`senderId`相同(好友/临时聊天环境)
- `subjectAvatarUri` <br> 聊天环境的头像
- `groupName` <br> 群名称 **需要聊天环境为群聊**
- `groupPerm` <br> `bot` 在群聊的权限 **需要聊天环境为群聊**
    - `MEMBER` - 普通群员
    - `ADMINISTRATOR` - 管理员
    - `OWNER` - 群主
- `groupOwner` <br> 获取群主 **需要聊天环境为群聊**


### GroupFiles

关于群文件相关的操作

| name | returnType |
|:----:|:----------:|
| file |    Any     |

**参数说明**

1. 寻找的群文件的名称（包含后缀名）
2. 进行的操作
    - `expiryTime` 获取上传时间
    - `md5` 获取文件内容的`md5`
    - `isFile` 表示远程文件时返回 `true`
    - `sha1` 获取文件内容的`SHA-1`
    - `path` 获取文件在群文件中的绝对路径
    - `exists` 查询文件在服务器中是否存在
    - `uploadTime` 获取文件上传文件
    - `extension` 文件的后缀名
    - `nameWithoutExtension` 文件全称 包含后缀名
    - `name` 文件名称 不包含后缀名
    - `url` 获取下载链接 <br>当文件不存在时抛出错误`IllegalStateException`
    - `size` 文件的大小 **可提供参数**
      - `kilobyte`或`kb` - 获取文件以`kb`为单位的大小
      - `megabyte`或`mb` - 获取文件以`mb`为单位的大小
      - `gigabyte`或`gb` - 获取文件以`gb`为单位的大小
      - `terabyte`或`tb` - 获取文件以`tb`为单位的大小 
      - 如果不提供参数则默认提供 以`byte`为单位的大小
    - `downland` 将这个群文件下载到参数处，返回下载到的地址 **必须提供参数**
    - `more` 移动文件到参数提供的子目录 当移动失败时返回`null` **必须提供参数**
    - `rename` 重命名这个文件为参数 **必须提供参数**
    - `delete` 删除文件
3. 参数 **可选,如果操作需要则必须提供**

### ReloadConfig

重新加载本插件的配置文件

| name | returnType |
|:----:|:----------:|
| file |    Any     |

**不需要参数**

---

## FeaturesTemplate

| use调用命名  |   类别    | 
|:--------:|:-------:|
| FEATURES | 一些现成的功能 |


### RandomFile

从一个文件夹内随机抽取一个文件 并返回这个文件的**绝对路径**

| name | returnType |
|:----:|:----------:|
| file |   String   |

**参数说明**
1. 文件夹的绝对路径 如果是文件则返回这个文件的绝对路径


### RandomCutImage

从一张图片中随机裁剪一部分，并保存在指定路径，返回保存路径

|      name      | returnType |
|:--------------:|:----------:|
| randomCutImage |   String   |

**参数说明**

1. 随机裁剪的图片的路径
2. 输出路径
3. 随机裁剪的`x`值 不可大于实际图片的`x` 
4. 随机裁剪的`y`值 **可选，默认同`x`**
5. 后缀名 **可选，默认会自动生成**

### RepetitionFunction

复读

| name | returnType |
|:----:|:----------:|
|  复读  |    Unit    |

**无须任何参数**

### DataFunction

自行保存关于**发送者**的数据 可进行相关操作 

保存路径在`data/.../SAVE_DATA_指令的哈希值.yml` 

| name | returnType |
|:----:|:----------:|
| data |    Any     |

**参数说明**

分以下几种传参方法

##### 读值操作
1. read

返回存储的值(`List<String>`) 当没有找到时返回字符串`"null"`
##### 保存操作
1. put
2. 保存的信息 可继续提供<br> 如：
   1. put
   2. 114514
   3. 1919810

返回存入的值(`List<String>`)

##### 修改操作
1. set
2. 修改的数据 <br>按照填入的数据相对应 如果不想修改则填入`"null"` 例如：
   1. set
   2. 114514
   3. null

当这个用户不存在时返回`false` 若成功修改返回`true`
##### 删除操作
1. rm

返回`Unit`



