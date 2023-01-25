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
