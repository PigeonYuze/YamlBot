## MessageTemplate

| use调用命名 |           类别           | 
|:-------:|:----------------------:|
| MESSAGE | 对`mirai`支持的部分信息类型进行支持  |

### CreateForwardMessageAndSend

构建并发送一条`ForwardMessage`转发信息

|            name             | returnType |
|:---------------------------:|:----------:|
|  sendCreateForwardMessage   |    Unit    |

**参数说明**

1. 信息的描述，请按照以下格式，其内容需按照下文的`DSL`语法 (接受`List`)
2. 转发信息的二次设定 (接受`Map`) 其`key`值应选取下文提供的内容 **可选**
    - `preview` 卡片预览, 只会显示前四个元素 **no-yaml** (默认选取信息列表中的前四个).
    - `title` 卡片标题, 默认为`转发的聊天记录`
    - `summary` 卡片底部内容，默认为`查看x条转发消息`
    - `brief` 消息列表显示
    - `source` 来源

> ## DSL 语法 (选自官方文档)
>
> 下文中 `S` 代表消息发送人. 可接受: 发送人账号 id(`Long` 或 `Int`)，本项决定了头像以及信息的发送者
>
> 下文中 `M` 代表消息内容. 可接受: `String`, `Message`(调用参数)
>
> ### 陈述一条消息
> 使用 `S says M`
>
> 语句 `123456789 named 鸽子 A says 咕` 创建并添加了一条名为 `鸽子 A` 的用户 `123456789` 发送的内容为 `咕` 的消息
>
>
> #### 陈述
> 一条 '陈述' 必须包含以下属性:
> - 发送人，必须位于第一个参数 如 `sender says M`, `sender named "xxx"`, `sender at 123`
> - 消息内容，必须位于最后一个参数. 只可以通过 `says` 后的内容设置, 即 `says M`.
>
> #### 组合陈述
> 现支持的可选属性为 `named`, `at`
>
> `named` 后为发送者**在聊天记录中的名称**
> `at` 为发送时的时间戳 **单位为秒**
>
> 最基础的陈述为 `S says M`. 可在 `says` 前**按任意顺序添加**组合属性:
>
> `S named xxx says M`; 名为`xxx`的`S`用户发送了`M`
>
> `S at 123456 says M`; 名为`S`的`S`用户在`123456`(`1970-01-02 18:17:36`) 发送了`M`
>
>
> 属性的顺序并不重要. 如下两句陈述效果相同.
>
> `S named "xxx" at 123456 says M`;
>
> `S at 123456 named "xxx" says M`;
>
> 其**基本逻辑与在`kotlin`中使用`DSL`构建的相差不大** 值得注意的是你可以不使用`""`直接写入内容来表示字符串

### ReadForwardMessage

读取转发信息的内容

|        name        | returnType |
|:------------------:|:----------:|
| readForwardMessage |    Any     |

**参数说明**

1. 提供指定的转发信息
2. 选择读取的内容 选择以下内容
    - config **转发卡片内容**
    - data **转发信息内容**
3. 读取的目标
    - 如果是 `config` 可以选取以下内容
        - `preview` 预览字符串内容，返回`List<String`
        - `title` 卡片标题, 默认为`转发的聊天记录`，返回`String`
        - `summary` 卡片底部内容，默认为`查看x条转发消息`，返回`String`
        - `brief` 消息列表显示，返回`String`
        - `source` 来源，返回`String`
    - 如果是 `data` 需要再提供一个参数用于描述下标（从0开始）
        - `message`, `messageChain` 信息内容，返回`MessageChain`
        - `time` 发送时间，返回`Int`
        - `senderId`, `id` 发送者qq号，返回`Long`
        - `senderName`, `name` 发送者昵称，返回`String`
4. 读取的下标 **如果参数3为`data`必须提供本项**

### CreateMusicShareAndSend

创建并发送一个QQ 互联通道音乐分享卡片

|         name         | returnType |
|:--------------------:|:----------:|
| sendCreateMusicShare |    Unit    |

**参数说明**

1. 音乐分享的渠道
    - `NeteaseCloudMusic` 网易云音乐
    - `QQMusic` QQ音乐
    - `MiguMusic` 咪咕音乐
    - `KugouMusic` 酷狗音乐
    - `KuwoMusic` 酷我音乐
2. 消息卡片标题，通常为歌曲名
3. 消息卡片内容，通常为歌手
4. 点击卡片跳转网页连接
5. 点击卡片跳转网页
6. 音乐文件连接
7. 信息列表显示文本 **可选，默认为`[分享]title`**

**参数示例**

```yaml
args:
  - NeteaseCloudMusic
  - "ジェリーフィッシュ"
  - "Yunomi/ローラーガール",
  - "https://y.music.163.com/m/song?id=562591636&uct=QK0IOc%2FSCIO8gBNG%2Bwcbsg%3D%3D&app_version=8.7.46"
  - "http://p1.music.126.net/KaYSb9oYQzhl2XBeJcj8Rg==/109951165125601702.jpg"
  - "http://music.163.com/song/media/outer/url?id=562591636&&sc=wmv&tn="
#  - "[分享]ジェリーフィッシュ"  #可选参数
```

以上与以下`kotlin`代码相同

```kotlin
MusicShare(
    kind = NeteaseCloudMusic,
    title = "ジェリーフィッシュ",
    summary = "Yunomi/ローラーガール",
    jumpUrl = "https://y.music.163.com/m/song?id=562591636&uct=QK0IOc%2FSCIO8gBNG%2Bwcbsg%3D%3D&app_version=8.7.46",
    pictureUrl = "http://p1.music.126.net/KaYSb9oYQzhl2XBeJcj8Rg==/109951165125601702.jpg",
    musicUrl = "http://music.163.com/song/media/outer/url?id=562591636&&sc=wmv&tn=",
//    brief = "[分享]ジェリーフィッシュ", //可选参数
)
```

### ReadMusicShare

读取一条音乐分享卡片中的信息

|      name      | returnType |
|:--------------:|:----------:|
| readMusicShare |    Any     |

**参数调用**

1. 信息
2. 读取的内容 请选以下内容
    - `kind`, `musicKind` 类型
    - `musicUrl`, `music` 音乐文件链接
    - `jumpUrl`, `jump` 跳转链接
    - `pictureUrl`, `picture` 图片链接
    - `summary` 内容
    - `brief` 消息列表显示的简介
    - `title` 标题
    - `content` 接近qq官方客户端的信息显示内容

### CreateFlashImageAndSend

创造并发送一张闪照

|         name         | returnType |
|:--------------------:|:----------:|
| sendCreateFlashImage |    Unit    |

**参数说明**

只需一个参数

你可以传递一张图片的`id`部分 满足以下格式

```regexp
(\{[\da-fA-F]{8}-([\da-fA-F]{4}-){3}[\da-fA-F]{12}}\..{3,5})
```

当然，你也可以直接传递它的路径，将会自动生成一个对应的图片

### ReadFlashImage

读取一张闪照中所含有的信息

|      name      | returnType |
|:--------------:|:----------:|
| readFlashImage |    Any     |

**参数说明**

1. 信息
2. 内容
    - `content` 接近qq官方客户端的信息显示内容
    - `image` 获取闪照中所对应的普通图片，返回`Image`
    - `miraiCode` 获取图片的`miraiCode`
    - `imageId` 图片的 id
    - `imageType` 图片的类型，可以返回以下内容之一
        - `PNG`
            - `BMP`
            - `JPG`
            - `GIF`
            - `APNG`
            - 当未知时返回`UNKNOWN`
    - `size` 图片的大小（字节）, 当无法获取时为 0
    - `height` 图片的高度 (px), 当无法获取时为 0
    - `width` 图片的宽度 (px), 当无法获取时为 0
    - `md5` 图片文件 MD5 16 bytes. 返回`Array<Byte>`
    - `queryUrl` 查询原图下载链接

### CreateFaceMessage

创建表情信息，返回这条表情信息的`Message`，你可以在`messageContent`中使用它

|        name         | returnType |
|:-------------------:|:----------:|
|  createFaceMessage  |  Message   |

**参数说明**

只需一个参数

这个参数可以是它的`id`，如：`0`

也可以是它的名称，如：`惊讶`

### ReadFaceMessage

读取表情信息的内容

|      name       | returnType |
|:---------------:|:----------:|
| readFaceMessage |    Any     |

**参数说明**

1. 信息
2. 内容
    - `name` 名称
    - `id`
    - `content` 接近官方格式的字符串

### CreateDiceMessageAndSend

~~这里原来是想要实现商城表情的，只可惜不支持~~

发送一个骰子信息

|   name   | returnType |
|:--------:|:----------:|
| sendDice |    Unit    |

**参数说明**
只需一个参数

你可以指定它的结果(`1~6`之中)

也可以直接不提供参数随机获取

### ReadDiceMessage

读取骰子信息的内容

|   name   | returnType |
|:--------:|:----------:|
| readDice |    Any     |

**参数说明**

1. 信息
2. 内容
    - `value` 结果
    - `name` 名称
    - `content` 接近官方格式的字符串

### CreateLightAppAndSend

创造并发送小程序卡片，目前该项暂时不支持读取

|        name        | returnType |
|:------------------:|:----------:|
| sendCreateLightApp |    Unit    |

**参数说明**

只需一个参数：

该卡片的内容，它可以是`Json`或者为`Xml`

### SendRockPaperScissors

创造并发送一个剪刀石头布表情

|          name          | returnType |
|:----------------------:|:----------:|
| sendRockPaperScissors  |    Unit    |

**参数说明**

通常来说，插件会自动根据环境提供联系人背景

你可以提供一个可选参数以指定输出对应的表情：

- `ROCK`石头
- `SCISSORS` 剪刀
- `PAPER` 布

**默认会随机抽取**

### ReadRockPaperScissors

读取剪刀石头布表情的内容

|         name          | returnType |
|:---------------------:|:----------:|
| readRockPaperScissors |    Any     |

**参数说明**

提供读取的对象，可选以下内容

- `content` 内容，如`[布]`
- `name` 名称，如 `[开心]`
- `id` 内部实验性的`Id`, 在`2.14.0`中始终为`11415`
- `internalId` 内部`Id`，在`2.14.0`中可为以下
  - `48` 石头
  - `49` 剪刀
  - `50` 布
- `eliminates`  判断 **当前手势** 能否淘汰对手 (**需要传递另一个`RockPaperScissors`**)
  
  赢返回 `true`，输返回 `false`，平局时返回 `null`(`NullObject`)

---

以下信息的创造已在`MiraiTemplate`中声明，你可以前往对应的文档创造

此处只负责读取

### ReadImage

读取图片信息的内容

|   name    | returnType |
|:---------:|:----------:|
| readImage |    Any     |

**参数说明**

1. 信息
2. 内容
    - `content` 接近qq官方客户端的信息显示内容
    - `miraiCode` 获取图片的`miraiCode`
    - `imageId` 图片的 id
    - `imageType` 图片的类型，可以返回以下内容之一
        - `PNG`
            - `BMP`
            - `JPG`
            - `GIF`
            - `APNG`
            - 当未知时返回`UNKNOWN`
    - `size` 图片的大小（字节）, 当无法获取时为 0
    - `height` 图片的高度 (px), 当无法获取时为 0
    - `width` 图片的宽度 (px), 当无法获取时为 0
    - `md5` 图片文件 MD5 16 bytes. 返回`Array<Byte>`
    - `queryUrl` 查询原图下载链接

### ReadAudio

读取语音信息

|   name    | returnType |
|:---------:|:----------:|
| readAudio |    Any     |

**参数说明**

1. 信息
2. 内容
    - `codec` 编码方式.
        - `AMR`
        - `SILK`
    - `content` 接近qq官方客户端的信息显示内容
    - `filename` 文件名称.
    - `md5` 文件 MD5. 16 bytes. 返回`Array<Byte>`
    - `size` 文件大小 bytes. 官方客户端支持最大文件大小约为 1MB
    - `length` 语音长度秒数
    - `downloadUrl` 下载链接 HTTP URL.