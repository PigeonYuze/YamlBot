## FeaturesTemplate

| use调用命名  |   类别    | 
|:--------:|:-------:|
| FEATURES | 一些现成的功能 |

### RandomFile

从一个文件夹内随机抽取一个文件 并返回这个文件的**绝对路径**

| name | returnType |
|:----:|:----------:|
| file |   String   |

**参数说明**

1. 文件夹的绝对路径 如果是文件则返回这个文件的绝对路径

### RandomCutImage

从一张图片中随机裁剪一部分，并保存在指定路径，返回保存路径

|      name      | returnType |
|:--------------:|:----------:|
| randomCutImage |   String   |

**参数说明**

1. 随机裁剪的图片的路径
2. 输出路径
3. 随机裁剪的`x`值 不可大于实际图片的`x`
4. 随机裁剪的`y`值 **可选，默认同`x`**
5. 后缀名 **可选，默认会自动生成**

### RepetitionFunction

复读

| name | returnType |
|:----:|:----------:|
|  复读  |    Unit    |

**无须任何参数**

### DataFunction

自行保存关于**发送者**的数据 可进行相关操作

保存路径在`data/.../SAVE_DATA_指令的哈希值.yml`

| name | returnType |
|:----:|:----------:|
| data |    Any     |

**参数说明**

分以下几种传参方法

##### 读值操作

1. read

返回存储的值(`List<String>`) 当没有找到时返回字符串`"null"`

##### 保存操作

1. put
2. 保存的信息 可继续提供<br> 如：
    1. put
    2. 114514
    3. 1919810

返回存入的值(`List<String>`)

##### 修改操作

1. set
2. 修改的数据 <br>按照填入的数据相对应 如果不想修改则填入`"null"` 例如：
    1. set
    2. 114514
    3. null

当这个用户不存在时返回`false` 若成功修改返回`true`

##### 删除操作

1. rm

返回`Unit`