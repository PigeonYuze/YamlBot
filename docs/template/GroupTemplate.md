# GroupTemplate

**`GroupTemplate` 包含有两大 `Template`**

- 一个为提供群公告调用的 `GroupAnnouncementsTemplate`

- 另一个是提供群荣誉的调用的 `GroupActiveTemplate` **未完工 预计在下个版本加入**

---

## GroupAnnouncementsTemplate

|        use调用命名        |    类别    | 
|:---------------------:|:--------:|
|  GROUP_ANNOUNCEMENTS  | 群公告求相关功能 | 

### Push

上传一个群公告 **需要管理员及以上的权限**

发布公告后群内将会出现 "有新公告" 系统提示.

| name |     returnType     |
|:----:|:------------------:|
| push | OnlineAnnouncement |

**参数说明**

1. 公告内容
2. 公告的设置 需要 `Map` 其` Key` 为修改项 `Value` 为是否启用(`true` 或 `false`)<br> 下文将展示可用 `key` 项
    - `pinned` 是否置顶. 可以有多个置顶公告
    - `sendToNewMember` 是否发送给新成员
    - `nameEdit` 显示能够引导群成员修改昵称的窗口
    - `show` 使用弹窗
    - `require` 需要群成员确认
    - `useImage` 提供图片 当` value` 为 `true` 时你可以在群公告中添加图片
3. 上传的图片路径 需要`useImage`设定为`true`

### Read

读取群公告内容

| name | returnType |
|:----:|:----------:|
| read |    Any     |

**参数说明**

1. 唯一识别属性或者文章的第一段话
2. 读取的内容
    - `fid` 唯一表示属性
    - `content` 群公告内容 **不包括图片**
    - `group` 发送的群聊
    - `allConfirmed`, `allRead` 是否所有全员都阅读了 返回 `true` (所有人都阅读)或 `false` (含有成员没有查看)
    - `confirmedMembersCount`, `readCount` 阅读了公告的成员数量 返回 `Int`
    - `sender` 发送者 如果发送者已退群返回 `NULL` 反之则返回一个 `NormalMember` 对象
    - `publicationTime`, `time` 发布时间 返回秒级时间戳 `Long`
    - `parameters` 公告的内部参数 返回 `AnnouncementParameters` 对象

### Delete

删除群公告 **需要管理员及以上的权限**

|  name  | returnType |
|:------:|:----------:|
| delete |    Unit    |

**参数说明**

1. 目标公告唯一表示属性

### ReadParameter

读取群公告参数的内容

|     name      | returnType |
|:-------------:|:----------:|
| readParameter |    Unit    |

**参数说明**

1. 群公告参数 (需要 `AnnouncementParameters` 对象)
2. 读取的目标 可选以下内容
    - `image` 获取群公告的图片 如果获取失败返回字符串`NULL` 反之则为一个 `AnnouncementImage` 对象
    - `isPinned` 是否置顶
    - `sendToNewMember` 是否发送给新成员
    - `showPopup` 使用弹窗
    - `showEditCard` 显示能够引导群成员修改昵称的窗口
    - `requireConfirmation` 需要群成员确认

### ReadOnlineAnnouncement

读取群公告信息

|          name          | returnType |
|:----------------------:|:----------:|
| readOnlineAnnouncement |    Unit    |

**参数说明**

1. 群公告 (需要 `OnlineAnnouncement` 对象)
2. 读取的目标 可选以下内容
    - `fid` 唯一表示属性
    - `content` 群公告内容 **不包括图片**
    - `group` 发送的群聊
    - `allConfirmed`, `allRead` 是否所有全员都阅读了 返回 `true` (所有人都阅读)或 `false` (含有成员没有查看)
    - `confirmedMembersCount`, `readCount` 阅读了公告的成员数量 返回 `Int`
    - `sender` 发送者 如果发送者已退群返回 `NULL` 反之则返回一个 `NormalMember` 对象
    - `publicationTime`, `time` 发布时间 返回秒级时间戳 `Long`
    - `parameters` 公告的内部参数 返回 `AnnouncementParameters` 对象