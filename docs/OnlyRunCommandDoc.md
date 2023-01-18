## OnlyRunCommand

仅进行`run`和`condition`操作的指令，不回复任何信息的指令

在`NormalCommand`的基础上删去了`answerContent`与`answeringMethod`两个参数

---

### 参数说明

#### Name 必填参数

该项为指令的调用名称，与`NormalCommand`相同

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

---