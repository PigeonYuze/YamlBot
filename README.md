# YamlBot

### 简介

一个简单的 通过向config下的yml配置文件进行运行`bot`的插件

你可以用此实现诸多功能

例如：

- 创建并使用指令
- 简单的可调用的账号系统

在未来我们还会实现更多的内容！

你可以将你的相关配置(如：`CommandReg.yml`) 以 `功能+原名`
的方式上传到 [your config](https://github.com/PigeonYuze/YamlBot/tree/master/your-config)

### TODO

- [x] 指令系统
- [x] 群公告调用支持
- [x] 对`Java`或`Kotlin`支持的部分 如：`switch(when)`
- [ ] 对群关闭功能
- [ ] 简短的调用语法
- [ ] 监听事件 **After 1.5.0**

### 需要

里面含有部分功能需要`ffmpeg`的支持，如果你想要有完整的功能支持，请配置好[ffmpeg](https://github.com/FFmpeg/FFmpeg)

由于yml是一个对****空格敏感****的格式 因此在解析yml时可能会导致相关的报错

**请保证你的.yml文件格式是正确的 再运行本插件!**

### 相关教程

关于调用函数方面，你可以前往[TemplateDoc](https://github.com/PigeonYuze/YamlBot/blob/3d15eee0f68095835e8c9990029251941722d68d/docs/TemplateDoc.md)
获取帮助

配置的说明，请可查看[configs](docs/config)下的内容

除了`CommandReg`中展示的普通指令(`NormalCommand`)
的创建说明，请查看[ArgCommandDoc](https://github.com/PigeonYuze/YamlBot/blob/3d15eee0f68095835e8c9990029251941722d68d/docs/ArgCommandDoc.md)
与[OnlyRunCommandDoc](https://github.com/PigeonYuze/YamlBot/blob/3d15eee0f68095835e8c9990029251941722d68d/docs/OnlyRunCommandDoc.md)

### 示例

以下我们以每日一图这样较为复杂的功能为例，逐一解释其原理

我们可以找到一个可以提供每日一言功能的api网站，以下以[必应每日一图](https://api.xygeng.cn/bing/`)为例

首先我们先声明它的名称，一般来说当有人发出这些内容时，指令就会被调用

```yaml
name:
  - 每日一图
  - bing一图
  - '/image bing'
```

在接下来，我们可以设定它的回复内容。

我们希望它会回复原信息，并且信息中包含有名称，图片以及来源

```yaml
answeringMethod: QUOTE
```

可是我们并不知道它会得到什么内容，要如何声明呢？

在这方面，`YamlBot`支持了声明与调用一个变量，你可以使用`%call-变量名%`来调用变量

```yaml
answerContent: '『%call-name%』%call-image%\n%call-from%'
```

接下来就是最重要的“声明变量”部分了，它叫做`run`

首先，我们需要获取今日的一图信息。

由于我们需要这个`api`网站的内容，所以我们需要调用`HTTP`内的`content`功能

```yaml
- use: HTTP
  call: content
  args:
    - 'https://api.xygeng.cn/bing/'
  name: content
```

这样子，就算是声明了一个变量，它的名称取决于`name`中的内容

`use`你可以理解为`import`，使`YamlBot`能够由这里找到你所需要的函数

`content`就变为了 `https://api.xygeng.cn/bing/` 网站的内容：

 ```json
{
  "code": 200,
  "data": {
    "id": 247,
    "time": "20230126",
    "title": "通往天门的阶梯",
    "url": "https://cn.bing.com/th?id=OHR.HighArchChina_ZH-CN8170154553_1920x1080.jpg&rf=LaDigue_1920x1080.jpg&pid=hp",
    "urlbase": "/th?id=OHR.HighArchChina_ZH-CN8170154553",
    "copyright": "天门洞，湖南天门山国家森林公园，中国 (© Shane P. White/Minden Pictures)",
    "copyrightlink": "...",
    "urls": [
      "..."
    ]
  },
  "updateTime": 1674670336679
}
```

我们从中发现`Json`内`"data"`的`"title"`,`"url"`,`"copyright"`为我们所需要的内容

可是我们该怎么解析这个字符串呢？

我们注意到它是`Json`格式的，所以我们可以找到`BASE`的`parseJson`功能来解析`Json`

```yaml
- use: BASE
  call: parseJson
  args:
    - '%call-content%'
    - data
    - title
  name: name

- use: BASE
  call: parseJson
  args:
    - '%call-content%'
    - data
    - url
  name: downlandUrl

- use: BASE
  call: parseJson
  args:
    - '%call-content%'
    - data
    - copyright
  name: from
```

这样，我们就成功获取了我们想要的内容；同时，我们还声明了`name`,`downlandUrl`,`from`几个变量

可是我们只是得到了它的下载链接，我们还需要把它下载到存储中

```yaml
- use: HTTP
  call: downland
  args:
    - '%call-downlandUrl%'
  name: imagePath
```

这样我们就得到了下载到系统中的链接

接下来我们再上传一个图片到指定群聊里

```yaml
 - use: MIRAI
   call: upload
   args:
     - '%call-imagePath%'
     - image
   name: image
```

这样子，我们需要做的就基本完成了

最后再加上条件的判断，因为它不需要条件，所以我们可以用`[]`代替

```yaml
condition: [ ]
```

这样子，我们就成功构建了一个指令，让我们来试试吧！
![img.png](docs/img.png)

可以看到，指令成功运行了，同时用对应的数据代替了原本的`%call-变量名%`

我们的任务就到此为止了

如果在运行中，你遇到了相关的bug，可以使日志等级为`ALL`,查看相关的debug信息

最终，我们就得到了以下内容

```yaml
COMMAND:
  - name:
      - 每日一图
      - bing一图
      - '/image bing'
    answeringMethod: QUOTE
    answerContent: '『%call-name%』%call-image%\n%call-from%'
    run:
      - use: HTTP
        call: content
        args:
          - 'https://api.xygeng.cn/bing/'
        name: content
      - use: BASE
        call: parseJson
        args:
          - '%call-content%'
          - data
          - title
        name: name
      - use: BASE
        call: parseJson
        args:
          - '%call-content%'
          - data
          - url
        name: downlandUrl
      - use: BASE
        call: parseJson
        args:
          - '%call-content%'
          - data
          - copyright
        name: from
      - use: HTTP
        call: downland
        args:
          - '%call-downlandUrl%'
        name: imagePath
      - use: MIRAI
        call: upload
        args:
          - '%call-imagePath%'
          - image
        name: image
    condition: [ ]
```

以上内容你可以在[bingimage-Command.yml](https://github.com/PigeonYuze/YamlBot/blob/master/your-config/bingimage-Command.yml)找到