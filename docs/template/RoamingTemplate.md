## RoamingSupported

|       use调用命名       |   类别   | 
|:-------------------:|:------:|
| `ROAMING_SUPPORTED` | 获取漫游记录 | 

### Get

获取当前上下文的 `RoamingMessages` 对象

| name |    returnType     |
|:----:|:-----------------:|
| get  | `RoamingMessages` |

**无需提供参数**

### GetRoamingMessages

根据上下文获取这期间所有的漫游信息

返回查询到的漫游消息记录, 顺序为由新到旧

|      name       |      returnType      |
|:---------------:|:--------------------:|
| roamingMessages | `Flow<MessageChain>` |

**参数说明**

1. 起始时间, `UTC+8` 时间戳, 单位为秒. 可以为 `0`, 即表示从可以获取的最早的消息起. 负数将会被看是 `0`.
2. 结束时间, `UTC+8` 时间戳, 单位为秒. 可以为 `9223372036854775807(最大值)`, 即表示到可以获取的最晚的消息为止. 低于 第一项参数 的值将会被看作是 第一项参数 的值.
3. 过滤器描述(可选)
   - `All` 全部信息
   - `RECEIVED` 筛选 `bot` 接收的消息
   - `SENT` 筛选 `bot` 发送的消息
   - 或者，你可以使用`JavaScript`表达式制作解析过滤器。<br/> 提供以下变量
     - `botId` Bot 的 QQ 号
     - `senderId` 发送人 `id`
     - `contactId` 聊天环境 `id`
     - `ids` 消息 ids (序列号). 
     - `target` 收信人或群的 id
     - `time` 时间戳, 单位为秒, 服务器时间.
     - `internalIds` 内部 ids. **仅用于协议模块使用**

### GetAllRoamingMessages

查询所有漫游消息记录.

返回查询到的漫游消息记录, 顺序为由新到旧

|      name      |      returnType      |
|:--------------:|:--------------------:|
| getAllMessages | `Flow<MessageChain>` |

**参数说明**

可提供以下一个参数

1. 过滤器描述(可选)
    - `All` 全部信息
    - `RECEIVED` 筛选 `bot` 接收的消息
    - `SENT` 筛选 `bot` 发送的消息
    - 或者，你可以使用`JavaScript`表达式制作解析过滤器。<br/> 提供以下变量
        - `botId` Bot 的 QQ 号
        - `senderId` 发送人 `id`
        - `contactId` 聊天环境 `id`
        - `ids` 消息 ids (序列号).
        - `target` 收信人或群的 id
        - `time` 时间戳, 单位为秒, 服务器时间.
        - `internalIds` 内部 ids. **仅用于协议模块使用**

### CreateRoamingMessageFilter

读取为一个 `RoamingMessageFilter` 过滤器

|            name            |       returnType       |
|:--------------------------:|:----------------------:|
| createRoamingMessageFilter | `RoamingMessageFilter` |

**参数说明**

提供过滤器描述
- `All` 全部信息
- `RECEIVED` 筛选 `bot` 接收的消息
- `SENT` 筛选 `bot` 发送的消息
- 或者，你可以使用`JavaScript`表达式制作解析过滤器。<br/> 提供以下变量
    - `botId` Bot 的 QQ 号
    - `senderId` 发送人 `id`
    - `contactId` 聊天环境 `id`
    - `ids` 消息 ids (序列号).
    - `target` 收信人或群的 id
    - `time` 时间戳, 单位为秒, 服务器时间.
    - `internalIds` 内部 ids. **仅用于协议模块使用**

### ReadRoamingMessage

读取指定的信息

|        name        |    returnType    |
|:------------------:|:----------------:|
| readRoamingMessage | `RoamingMessage` |

**参数说明**

1. `Flow<RoamingMessage>` 对象
2. 下标(可选，默认为最后一位)