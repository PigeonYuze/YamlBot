## ArgCommand

可提供**参数**的指令，通过本项你可以要求使用者**提供目标参数**以及**调用相关参数**

该项在`NormalCommand`的基础上扩展了1个必填参数与5个可选参数

同时修改了指令的判断逻辑

---

### 参数说明

#### Name 必填参数

该项为指令的调用名称，但是与`NormalCommand`不同的是，本项为**不包括参数**的名称，而是**指令开头**，
关于参数的解析将由插件自动生成

举个例子:

```yaml
name:
  - '/arg test'
  - '/give me your settings' 
```

以上在经过`argsSize`与`argsSplit`的加工后用户使用以下内容也可以触发指令
> /arg test **1 2 3**
>
> /give me your settings **open:true obj:111**

#### AnsweringMethod 必填参数

指令回答的方式 逻辑与`NormalCommand`相同

应为`AT_SEND`,`QUOTE`,`SEND_MESSAGE`中的一者

#### AnswerContent 必填参数

指令回答的内容，支持`MiraiCode`,逻辑与`NormalCommand`相同

#### Run 必填参数

进行的操作，你可以理解为赋值，逻辑与`NormalCommand`相同

虽然为**必填参数**，但是本项可以为一个**空组**，如：

```yaml
run:[]
```

#### Condition 必填参数

进行的要求，逻辑与`NormalCommand`相同

虽然为**必填参数**，但是本项可以为一个**空组**，如：

```yaml
run:[]
```

#### ArgsSize 必填参数

参数的数量

- 当超过此项时(`> argsSize`)
  使用在`argsSize`**范围内的所有参数**返回
- 当等于此项时(`= argsSize`)
  直接将得到的参数返回
- 当小于此项时(`< argsSize`)
  默认下会尝试让发送者提供后续参数，当`useLaterAddParams`为`false`时回复**您所提供的参数不足**，结束进程

#### ArgsSplit 可选参数

在截取参数时，以此来截取每一个参数，**默认为`' '`**

举个例子：

```yaml
argsSplit: "|"
```

则指令的参数部分`a|b|c|d`会被分割为以下存储

```yaml
arg1: "a"
arg2: "b"
arg3: "c"
arg4: "d"
```

你可以在`%call-ANY%`中调用参数内容，如：`%call-arg1%`就可以获取上文中的`a`为值

#### UseLaterAddParams 可选参数

是否开启“后续添加参数”, **默认为`true`**

> ##### 什么是“后续添加参数”?
>
> 在发送者发送了原生信息后
>
> 如果信息内的参数数量满足了设定的数量就会直接返回
>
> 如果未满足则执行本操作
>
> 由`bot`监听下一条信息获取剩余的参数，直到满足了条件为止

#### LaterAddParamsTimeoutSecond 可选参数

在后续添加参数中，每一个参数的等待时间如果超过的此项将会超时结束**单位为秒 默认为`60`**

#### Request 可选参数

对参数的要求（目前仅支持对后续添加参数的检测） **默认为`null`**

**当参数对应的下标未被定义或者本项未被定义时，不会检测参数**

**参数的检测是根据原信息的每一个内容进行检测，如果原富文本同时包含有两个内容，则选择符合条件的一项**

此处类型为`Map`

- `key`为下标，从0开始
- `value`为参数的类别，目前支持以下几项
    - `double` 小数类型，需要为纯文本
    - `int` 纯数字类型，需要为纯文本
    - `long` 数字类型，需要为纯文本
    - `string` 纯文本，无任何要求，**默认都为此项**
    - `boolean` 布尔值类型(`true`,`false`)，需要为纯文本
    - `forwardMessage` 转发信息
    - `flashImage` 闪照信息
    - `Image` 照片信息
    - `pokeMessage` 戳一戳信息
    - `audio` 语音信息
    - `at` At信息
    - `musicShare` 音乐分享卡片
    - `xmlJsonMessage` `xml`与`json`格式的富文本信息，通常为小程序信息(`json`)与服务信息

举个例子，

```yaml
request:
  0: int
  2: image
```

这样就规定了发送者的第一条信息包含纯文本数字，第三条信息包含图片，第二条信息没有任何规定

返回的参数也会在检测过程中自动转换为规定的类型

#### Describe 可选参数

指令对应参数的描述，如果不支持后续添加参数则该项无效。**默认为`null`**

此处类型为`Map`

- `key`为下标，从0开始
- `value`为描述

在后续添加参数时，插件会发送以下信息作为介绍：
> 请为 `$MESSAGE$` 指令提供值 [$i/$argsSize]
>
> 描述：`$describe[SIZE]`
>
>> 如果设置了要求
> > 该项需要 `${request[SIZE]}` 格式的信息
>
> `$laterAddParamsTimeoutSecond`秒后未响应则自动停止

如果没有设置此项则，但是设置需求则为：
> 请为 `$MESSAGE$` 指令提供值 [$i/$argsSize]
>
> 该项需要 `${request[SIZE]}` 格式的信息
>
> `$laterAddParamsTimeoutSecond`秒后未响应则自动停止

如果什么都没有设置则为：
> 请为 `$MESSAGE$` 指令提供值 [$i/$argsSize]
>
> 该项无任何要求
>
> `$laterAddParamsTimeoutSecond`秒后未响应则自动停止

---