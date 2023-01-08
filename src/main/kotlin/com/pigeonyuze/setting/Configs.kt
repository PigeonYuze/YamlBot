
package com.pigeonyuze

import com.pigeonyuze.YamlBot.reload
import com.pigeonyuze.account.UserElement
import com.pigeonyuze.command.*
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value


fun runConfigsReload()  {
    UserConfig.reload()
    CommandConfigs.reload()
}


object UserConfig : AutoSavePluginConfig("UserConfig"){
    @ValueDescription("是否开启用户设置")
    val open : Boolean by value(false)
    @ValueDescription("""
     用户号的开始位
     如果为1000 则注册时展示的UID为 1001(1000+1)  
        """)
    val userStartIndex : Int by value(1000)
    @ValueDescription("""
        默认用户名的选择
        当为 "nick" 时 采用用户的qq昵称
        当为 "name" 时 采用用户的注册群卡片名称(如果不是在群内 则采取昵称)
        如果为其他则以值作为标注
        
    """)
    val userNickSource : String by value("nick")
    @ValueDescription("""
        其他的元素
        你可以提供提供设置此项来为你的bot的User增加一个参数
        
        你需要提供name,type,defaultValue三个参数
        
        defaultValue是赋值时的默认参数 如果你希望他是默认值 你可以使用new代替
        
        type是这一个变量的类型 它可以为Java的八大基本类型 外加list,set,map,string,date
        
        name为这一个变量的名称 在调用时它会默认采取该项为调取名 此项不可重复
        
        备注： 如果你想调用该项 请使用%value-${'$'}name% 如： %value-regDate%
        该项依赖反射以运行 可能会带来初始化时性能上的损失
    """)
    val otherElements : MutableList<UserElement> by value(mutableListOf(UserElement("regDate","date","new")))

}
@ValueDescription("指令注册")
object CommandConfigs : AutoSavePluginConfig("CommandReg"){
    @ValueDescription("指令处理")
    val COMMAND: List<Command> by value(
        listOf(
            Command(
                name = listOf("test"),
                answeringMethod = AnsweringMethod.QUOTE,
                answerContent = """
                hello,world!
                this is a test message!
            """.trimIndent(),
                run = listOf(),
                condition = listOf(Condition(Condition.JudgmentMethod.NONE, null)),
            ),
            Command(
                name = listOf("/hikokoto"),
                answeringMethod = AnsweringMethod.QUOTE,
                answerContent = "『 %call-hitokoto% 』 —— %call-from%",
                run = listOf(TemplateYML(ImportType.HTTP, "content", listOf("https://v1.hitokoto.cn"), name = "content"),
                    TemplateYML(ImportType.BASE,"parseJson", listOf("%call-content%","hitokoto"),"hitokoto"),
                    TemplateYML(ImportType.BASE,"parseJson", listOf("%call-content%","from"),"from")
                ),
                condition = listOf(Condition(Condition.JudgmentMethod.NONE, null)),
            )
        )

    )
}