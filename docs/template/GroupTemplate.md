# GroupTemplate

**`GroupTemplate` 包含有两大 `Template`**

- 一个为提供群公告调用的 `GroupAnnouncementsTemplate`

- 一个是提供群荣誉的调用的 `GroupActiveTemplate`

---

## GroupAnnouncementsTemplate

|        use调用命名         |   类别    | 
|:----------------------:|:-------:|
| `GROUP_ANNOUNCEMENTS`  | 群公告相关功能 | 

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
    - `confirmedMembersCount`, `readCount` 阅读 / 确认 了公告的成员数量 返回 `Int`
    - `sender` 发送者 如果发送者已退群返回 `NULL` 反之则返回一个 `NormalMember` 对象
    - `publicationTime`, `time` 发布时间 返回秒级时间戳 `Long`
    - `parameters` 公告的内部参数 返回 `AnnouncementParameters` 对象
   - `confirmedMember` 所有 已确认 / 已阅读 的成员名单(`List`)
   - `unconfirmedMember` 所有 未确认 / 未阅读 的成员名单(`List`)

### IsMemberReadAnnouncement

查询某个用户是否已 阅读 / 确认

|           name           | returnType |
|:------------------------:|:----------:|
| isMemberReadAnnouncement |  Boolean   |

**参数说明**

1. 群公告的`OnlineAnnouncement`对象 或 群公告的`fid`
2. 查询成员对象的`Member`对象 或 `qq`号


## GroupActiveTemplate

|    use调用命名     | 类别  | 
|:--------------:|:---:|
| `GROUP_ACTIVE` | 群荣誉 | 

关于群荣誉功能的实现，如：龙王 的获取记录

在实现中，会出现`Impl`函数，一般来说，这些函数需要手动提供`GroupActive`对象在第一位第一位

[//]: # (-----------------------------)
### GetInstance

获取当前群聊的`GroupActive`对象

| name | returnType  |
|:----:|:-----------:|
| get  | GroupActive |

**无需参数**

### ReadActiveObj

读取当前群聊的`GroupActive`对象的属性

|     name      | returnType  |
|:-------------:|:-----------:|
| readActiveObj | GroupActive |

**参数说明**

只需提供读取的参数对象，可选以下内容

- `isHonorVisible`, `showActive`  是否在群聊中显示荣誉
- `isTemperatureVisible`, `showTemperature` 是否在群聊中显示活跃度
- `isTitleVisible`, `showTitle` 是否在群聊中显示头衔
- `rankTitles` 等级头衔列表，返回`Map`其`key`是等级，`value`是头衔
- `temperatureTitles` 活跃度头衔列表，返回`Map`其`key`是等级，`value`是头衔。操作成功时会同时刷新活跃度头衔信息
- `thisChart`, `chart` 获取活跃度图表数据（返回`ActiveChart`对象）
- `queryActiveRank`, `thisRank` 获取活跃度排行榜(返回`List`)，通常是前五十名




### QueryHonorHistory

查询当前群聊某个荣誉的当前持有历史数据

|       name        |   returnType    |
|:-----------------:|:---------------:|
| queryHonorHistory | ActiveHonorList |

**参数说明**

需要提供对应的荣誉名或者名称

- `龙王` (`1`)
- `群聊之火` (`2`)
- `群聊炽焰` (`3`)
- `冒尖小春笋` (`4`)
- `快乐源泉` (`5`)
- `学术新星` (`6`)
- `顶尖学霸` (`7`)
- `至尊学神` (`8`)
- `一笔当先` (`9`)
- `壕礼皇冠` (`10`)
- `善财福禄寿` (`11`)


### ReadActiveHonorList

|        name         | returnType |
|:-------------------:|:----------:|
| readActiveHonorList |    Any     |

**参数说明**

1. `ActiveHonorList` 对象
2. 读取的值

可读取的值
- `current`: `ActiveHonorInfo & NullObject`
  - 当前荣耀持有者 (龙王，壕礼皇冠, 善财福禄寿) 不存在时返回`NULL`
- `type`: `GroupHonorType`
  - 群荣耀历史记录
- `records`: `List<ActiveHonorInfo>`
  - 群荣耀历史记录
- `last`: `ActiveHonorInfo(or throw NoSuchElementException)`
  - 该荣誉的最后获得者 **(使用`records`获取，结果可能不准确)**
- `first`: `ActiveHonorInfo(or throw NoSuchElementException)`
  - 第一个获取该荣誉的用户 **(使用`records`获取，结果可能不准确)**
- `size`: `Int`
  - 有多少人获取了该荣誉 **(使用`records`获取，结果可能不准确)**

### SettingActiveObjImpl

**暂无自动提供`GroupActive`实现**

设置群荣誉的相关设置，需要权限(`群聊管理员(ADMINISTRATOR)`)

当权限不足时，抛出异常`PermissionDeniedException`

|         name         | returnType |
|:--------------------:|:----------:|
| settingActiveObjImpl |    Any     |

**参数说明**

1. 提供`GroupActive`对象
2. 修改字段名称
3. 新值

可修改的字段
- `isHonorVisible`, `showActive` 需要新值（`Boolean`）
  - 设置是否在群聊中显示荣誉。
- `isTemperatureVisible`, `showTemperature` 需要新值（`Boolean`）
  - 设置是否在群聊中显示活跃度。操作成功时会同时刷新等级头衔信息。
- `isTitleVisible`, `showTitle` 需要新值（`Boolean`）
  - 设置是否在群聊中显示头衔。操作成功时会同时刷新等级头衔信息。
- `rankTitles` 需要新值 (`Map`)
  - 设置等级头衔列表，**键是等级(数字)，值是头衔(字符串)**。操作成功时会同时刷新等级头衔信息。
- `temperatureTitles` 需要新值 (`Map`)
  - 设置活跃度头衔列表，**键是等级(数字)，值是头衔(字符串)**。操作成功时会同时刷新活跃度头衔信息。

### ReadActiveChartImpl



读取一个`ActiveChart`对象信息

|        name        | returnType |
|:------------------:|:----------:|
| readActiveChatImpl |    Int     |

**参数说明**

1. `ActiveChart`对象
2. 读取的范围，
   - 你需要传递`yyyy-MM`格式的字符串(如`2023-6`)
   - 或者此处传递月份，年份选自当前系统时间年份(如传入`6`效果等同于`2023-6`)
   - 当然，你也可以连续提供2个参数，第一个为年份，第二个为月份
3. 读取的对象
   - `actives` 每日活跃人数
   - `exit` 每日退群人数
   - `join` 每日入群人数
   - `member` 每日总人数
   - `sentences`每日申请人数

### ReadActiveHonorInfoImpl

读取一个`ActiveHonorInfo`对象信息

|          name           | returnType |
|:-----------------------:|:----------:|
| readActiveHonorInfoImpl |    Any     |

**参数说明**

1. `ActiveHonorInfo`对象
2. 读取的属性名
   - `avatarUrl`, `avatar` 群员头像链接
   - `memberName`, `name` 群员昵称
   - `member` 群员实例, 当群员已退出时返回`null`
   - `historyDays`, `days` 历史获得天数
   - `maxTermDays`, `max` 最大蝉联天数
   - `nowTermDays`, `termDays`, `now` 当前蝉联天数
   - `memberId`, `id` 群员 ID