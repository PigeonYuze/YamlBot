## MessageManagerTemplate

|     `use`调用命名     |      类别      | 
|:-----------------:|:------------:|
| `MESSAGE_MANAGER` |  向特定的目标发送信息  |

### SendMessageToGroup

向一个群聊发送信息

|        name        | returnType |
|:------------------:|:----------:|
| sendMessageToGroup |    Unit    |

**参数说明**

1. 目标
   - 传递目标群号，运行时采取在线`Bot`查找群聊对象，当
     无法寻找到对象时抛出`NullPointerException`错误
   - 传递`Group`对象，可提供群聊事件监听时的`%call-group%`
     或者类型为`Group`的值
2. 信息
   - 传递`Message`对象，使用[MessageTemplate](MessageTemplate.md)构建获取信息 
    **由于设计缺陷，这可能没什么用**
   - 传递字符串对象，自动转换为`PlainText`的信息对象（也可以传递**非字符串对象**，调用`toString()`后构建）

### SendMessageToFriend

向一个群聊发送信息

|        name         | returnType |
|:-------------------:|:----------:|
| sendMessageToFriend |    Unit    |

**参数说明**

1. 目标
    - 传递目标`qq`号，运行时采取在线`Bot`查找好友对象，当
      无法寻找到对象时抛出`NullPointerException`错误
    - 传递`Friend`对象，类型为`Group`的值
2. 信息
    - 传递`Message`对象，使用[MessageTemplate](MessageTemplate.md)构建获取信息
      **由于设计缺陷，这可能没什么用**
    - 传递字符串对象，自动转换为`PlainText`的信息对象（也可以传递**非字符串对象**，调用`toString()`后构建）

### SendMessageToAllGroups

向`Bot`所在的所有群聊发送一条信息，返回成功发送的群聊数量

当发生错误时并不会提示，而是直接跳过该群聊继续发送

|          name          | returnType |
|:----------------------:|:----------:|
| sendMessageToAllGroups |    Int     |

**参数说明**

1. 信息
   - 传递`Message`对象，使用[MessageTemplate](MessageTemplate.md)构建获取信息
   **由于设计缺陷，这可能没什么用**
   - 传递字符串对象，自动转换为`PlainText`的信息对象（也可以传递**非字符串对象**，调用`toString()`后构建）

### SendMessageToAllFriends

向`Bot`所有好友发送一条信息，返回成功发送的好友数量

当发生错误时并不会提示，而是直接跳过该好友继续发送

|          name           | returnType |
|:-----------------------:|:----------:|
| sendMessageToAllFriends |    Int     |

**参数说明**

1. 信息
    - 传递`Message`对象，使用[MessageTemplate](MessageTemplate.md)构建获取信息
      **由于设计缺陷，这可能没什么用**
    - 传递字符串对象，自动转换为`PlainText`的信息对象（也可以传递**非字符串对象**，调用`toString()`后构建）

### NudgeGroupMember

戳一戳群聊中的成员

|       name       | returnType |
|:----------------:|:----------:|
| nudgeGroupMember |    Unit    |

**参数说明**

1. 群聊号
2. 目标成员的`qq`号

### NudgeFriend

戳一戳好友

|    name     | returnType |
|:-----------:|:----------:|
| nudgeFriend |    Unit    |

**参数说明**

1. 好友的`qq`号