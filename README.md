# YamlBot

##### 一个简单的 通过向config下的yml配置文件增加内容而创建指令的插件

里面含有部分功能需要`ffmpeg`的支持，如果你想要有完整的功能支持，请配置好[ffmpeg](https://github.com/FFmpeg/FFmpeg)

在未来还会向此增加包含参数的指令(例如： `/test command arg1 arg2`)

目前本插件已可以实现****基本的调用参数**** 以及基本的账号系统

由于yml是一个对****空格敏感****的格式 因此在解析yml时可能会导致相关的报错

**请保证你的.yml文件格式是正确的 再运行本插件!**

你可以将你的相关配置(如：`CommandReg.yml`) 以 `功能+原名`的方式上传到 [your config](https://github.com/PigeonYuze/YamlBot/tree/master/your-config)

-----

## Config设置

### UserConfig

这是关于自带的账号内容的配置

---
首先是关于是否开启的设置：

```yaml
# 是否开启用户设置
open: false
```

该项如果被关闭了 那么所有关于用户的设置都会被抛出错误`IllegalStateException`
并且提示`User does not open` 如果出现了该错误 则意味除了保存信息意外
还有其他的函数调用了`User`的内置函数 你可以通过查看指令config进行寻找
---

```yaml
# 用户号的开始位
# 如果为1000 则注册时展示的UID为 1001(1000+1)
userStartIndex: 1000
```

该项是对用户id的设置 在注册时**id取该项加用户总量**

如共有10个用户，该项设置为1000，在新有人注册时，他的id为1011(1000+10+1)

---
```yaml
# 默认用户名的选择
# 当为 "nick" 时 采用用户的qq昵称
# 当为 "name" 时 采用用户的注册群昵称(如果不是在群内 则采取机器人的备注/昵称)
# 如果为其他则以值作为标注
# 
userNickSource: nick
```
---

如注释所示，在`CommandReg.yml`处可通过模板:
```yaml
     - use: USER
       call: value
       args: 
        - name
       name: any
```
进行调用

关于`CommandReg.yml`的设置请看下文

---
```yaml
# 其他的元素
# 你可以提供提供设置此项来为你的bot的User增加一个参数
# 
# 你需要提供name,type,defaultValue三个参数
# 
# defaultValue是赋值时的默认参数 如果你希望他是默认值 你可以使用new代替
# 
# type是这一个变量的类型 它可以为Java的八大基本类型 外加list,set,map,string,date
# 
# name为这一个变量的名称 在调用时它会默认采取该项为调取名 此项不可重复
otherElements: 
  - name: regDate
    type: date
    defaultValue: new
  - name: coin
    type: int
    defaultValue: 0
```
参数说明：

**`name`为该参数的名称 可通过该名称在外部调用**

**`type`为参数的类型**

该项`type`支持`List`,`Map`,`Set`,`String`(`str`),`Date`,
`int`,`long`,`byte`,`short`,`float`,`double`,`boolean`(`bool`)


以上代码在`Java`中等同与：

```java
import java.util.Date;

public class OtherElements {
    public OtherElements() {
        Date date = new Date();
        int coin = 0;
    }
}
```
可以发现 defaultValue的值就是量的默认值，且需要满足要求
如果你想要使用`List`,`Map`,`Set`的话，请不要直接在yml写出相关的内容，
而是使用如`[a,b,c]`这样的方式

例如:
```yaml
- name: test
  type: list
  defaultValue: [a,b,c,1,2,3]
- name: test2
  type: map
  defaultValue: [a=b,b=1,1=c]
```
则等同于:

```java
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OtherElements {
    public OtherElements() {
        List<Object> test = List.of("a", "b", "c", 1, 2, 3);
        Map<Object, Object> test2 = new HashMap<>() {{
            this.put("a", "b"); //key="a",value="v"
            this.put("b", 1); //key="b",value=1
            this.put(1,"c"); //key=1,value="c"
        }};
    }
}
```
可以发现，当运行时会自动转换其类型
如果它是纯数字，会转换为`long`或`int`；
如果它是小数类型，会转换为`double`；
如果都不是，则为字符串

并且，只要内容`defaultValue`并且`type`为`Map`,`List`,`Set`中包含`=`**该项就会被判断为一个`Map`(可嵌套)**!

`Map`的`Key`或者`Value`中都不能包含`=`（会把前后判断为Key和Value）

该项不支持泛型

---
### CommandReg
**注册指令**

在通过对该项的设置后，在对`MessageEvent`的监听会根据所设置自动作出回答

```yaml
COMMAND:
  - name:
      - test
    answeringMethod: QUOTE
    answerContent: "hello,world!\nthis is a test message!\n%call-sender%"
    run:
      - use: MIRAI
        call: value
        args:
          - sender
        name: sender
    condition:
      - request: none
        call: null
```
以上是一个标准的`Command`的写法，
同样的，你也可以在此处填写其他的`Command` 如：`OnlyRunCommand` 和 `ArgCommand`

### 参数说明
### Name
```yaml
- name: 
  - test
```

name为该指令的**调用名** 当使用者发送的信息在这其中时会运行此项

当然 如果是`ArgCommand`的话 会采用开头匹配的方法进行判断

### AnsweringMethod
```yaml
answeringMethod: QUOTE
```
**回复的方式** 

可选为`QUOTE`,`SEND_MESSAGE`,`AT_SEND`

- `QUOTE` 为回复信息
- `SEND_MESSAGE` 为直接发送信息
- `AT_SEND` 为at发送者后发送信息

### AnswerContent
```yaml
answerContent: "hello,world!\nnew line"
```
**回答的内容**

使用`\n`进行换行 切记不可直接进行换行

错误示范:
```yaml
answerContent: "a
b
c"
```
如果这样会被判断为`a b c`

在此项中 你可以提供`%call-name%`这样的格式来调用一个参数

以上分为三个部分
- `%call-`  为调用的**标识符** 不重要 但是只有**有了此项才会被识别**
- `name`  为**调用目标的名称** 该项**需要在`run`内找到** 否则会被用`null`替换.<br>
使用时就是将`run`中`name`与之对应的对象替换文本
- `%`  同样为**标识符** 标识着调用的**结束**

如果在替换后出现了null 也可能标识对应的`run`返回的就是`"null"`

可查看日志进行判断 ~~实在不行你可以附上完整的日志到我发的贴下面体温~~
> V/Easy Mirai: [Command-run] Function value return: null return type: java.lang.String
> >则代表返回的就是一个`"null"`

### Run
```yaml
 run:
  - use: MIRAI
    call: value
    args:
      - sender
    name: sender
```

**同时进行的操作** 你也可以当作是**声明一个量**

- `use` 为调用的库 它可以为
  - USER
  - BASE
  - HTTP
  - MIRAI
  - FEATURES
  <br> 中的一种 
- `call` 为调用的函数名 
- `args` 为传递的参数
- `name` 为命名 它不可包含`%`字段

你可以把该项看作以下的 `Kotlin` 代码
```kotlin
val sender: String = MiraiTemplate.value("sender")
```

### Condition
```yaml
condition:           
  - request: none    
    call: null       
```
**条件** 但是目前还没有什么用

`request`可以为以下几项
  - `if true`
  - `if false`
  - `else if true`
  - `else if false`
  - `else`

以上都需要call返回一个布尔值 再提供信息进行`if/else`操作

***此项可以不写 不强制要求序列化 或者按照举例一样修改***

关于调用函数方面，你可以前往[TemplateDoc](https://github.com/PigeonYuze/YamlBot/blob/069e5651ab8e83e2b91294f6308bce61a2e8d1e9/src/main/kotlin/com/pigeonyuze/template/TemplateDoc.md)获取帮助

