## UserConfig

这是关于自带的账号内容的配置

---
首先是关于是否开启的设置：

```yaml
# 是否开启用户设置
open: false
```

该项如果被关闭了 那么所有关于用户的设置都会被抛出错误`IllegalStateException`
并且提示`User does not open` 如果出现了该错误 则意味除了保存信息意外
还有其他的函数调用了`User`的内置函数 你可以通过查看指令config进行寻找

---

```yaml
# 用户号的开始位
# 如果为1000 则注册时展示的UID为 1001(1000+1)
userStartIndex: 1000
```

该项是对用户id的设置 在注册时**id取该项加用户总量**

如共有10个用户，该项设置为1000，在新有人注册时，他的id为1011(1000+10+1)

---

```yaml
# 默认用户名的选择
# 当为 "nick" 时 采用用户的qq昵称
# 当为 "name" 时 采用用户的注册群昵称(如果不是在群内 则采取机器人的备注/昵称)
# 如果为其他则以值作为标注
# 
userNickSource: nick
```

---

如注释所示，在`CommandReg.yml`处可通过模板:

```yaml
     - use: USER
       call: value
       args:
         - name
       name: any
```

进行调用

关于`CommandReg.yml`的设置请看下文

---

```yaml
# 其他的元素
# 你可以提供提供设置此项来为你的bot的User增加一个参数
# 
# 你需要提供name,type,defaultValue三个参数
# 
# defaultValue是赋值时的默认参数 如果你希望他是默认值 你可以使用new代替
# 
# type是这一个变量的类型 它可以为Java的八大基本类型 外加list,set,map,string,date
# 
# name为这一个变量的名称 在调用时它会默认采取该项为调取名 此项不可重复
otherElements:
  - name: regDate
    type: date
    defaultValue: new
  - name: coin
    type: int
    defaultValue: 0
```

参数说明：

**`name`为该参数的名称 可通过该名称在外部调用**

**`type`为参数的类型**

该项`type`支持`List`,`Map`,`Set`,`String`(`str`),`Date`,
`int`,`long`,`byte`,`short`,`float`,`double`,`boolean`(`bool`)

以上代码在`Java`中等同与：

```java
import java.util.Date;

public class OtherElements {
    public OtherElements() {
        Date date = new Date();
        int coin = 0;
    }
}
```

可以发现 defaultValue的值就是量的默认值，且需要满足要求
如果你想要使用`List`,`Map`,`Set`的话，请不要直接在yml写出相关的内容，
而是使用如`[a,b,c]`这样的方式

例如:

```yaml
- name: test
  type: list
  defaultValue: [ a,b,c,1,2,3 ]
- name: test2
  type: map
  defaultValue: [ a=b,b=1,1=c ]
```

则等同于:

```java
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OtherElements {
    public OtherElements() {
        List<Object> test = List.of("a", "b", "c", 1, 2, 3);
        Map<Object, Object> test2 = new HashMap<>() {{
            this.put("a", "b"); //key="a",value="v"
            this.put("b", 1); //key="b",value=1
            this.put(1, "c"); //key=1,value="c"
        }};
    }
}
```

可以发现，当运行时会自动转换其类型
如果它是纯数字，会转换为`long`或`int`；
如果它是小数类型，会转换为`double`；
如果都不是，则为字符串

并且，只要内容`defaultValue`并且`type`为`Map`,`List`,`Set`中包含`=`**该项就会被判断为一个`Map`(可嵌套)**!

`Map`的`Key`或者`Value`中都不能包含`=`（会把前后判断为Key和Value）

该项不支持泛型
