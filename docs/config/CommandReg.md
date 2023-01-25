## CommandReg

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
> > 则代表返回的就是一个`"null"`

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
