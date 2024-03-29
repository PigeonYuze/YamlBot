package com.pigeonyuze

import com.pigeonyuze.YamlBot.reload
import com.pigeonyuze.account.UserElement
import com.pigeonyuze.command.Command
import com.pigeonyuze.command.Command.*
import com.pigeonyuze.command.YamlCommandDecoder
import com.pigeonyuze.command.YamlCommandDecoder.load
import com.pigeonyuze.command.element.AnsweringMethod
import com.pigeonyuze.command.element.ImportType
import com.pigeonyuze.command.element.TemplateYML
import com.pigeonyuze.listener.EventListener
import com.pigeonyuze.listener.YamlEventListenerDecoder
import com.pigeonyuze.listener.YamlEventListenerDecoder.load
import com.pigeonyuze.listener.impl.Listener.Companion.execute
import com.pigeonyuze.template.parameterOf
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value


fun runConfigsReload() {
    runBlocking {
        UserConfig.reload()
        CommandConfigs.load()
        LoggerConfig.reload()
        ListenerConfigs.load()
        ListenerConfigs.startAllListener()
    }
}

suspend fun ListenerConfigs.startAllListener() {
    LoggerManager.loggingDebug("startAllListener", "Try to start all listeners...")
    for ((index, eventListener) in this.listener.withIndex()) {
        eventListener.execute()
        LoggerManager.loggingTrace("startAllListener", "Start listener --> ${eventListener.type}#$index")
    }
    LoggerManager.loggingDebug("startAllListener", "Done.")
}

object LoggerConfig : AutoSavePluginConfig("LoggerConfig") {
    @ValueDescription(
        """
        是否开启日志系统，当关闭时不再会输出主动发出的任何日志信息
        除非出现了被动的错误，如运行错误或者错误的参数时输出错误
    """
    )
    val open: Boolean by value(true)

    @ValueDescription(
        """
        当本插件出现相关报错时会向这个群聊发送报错信息
    """
    )
    val debugGroup: Long by value(114514L)
}


object UserConfig : AutoSavePluginConfig("UserConfig") {
    @ValueDescription("是否开启用户设置")
    val open: Boolean by value(false)

    @ValueDescription(
        """
     用户号的开始位
     如果为1000 则注册时展示的UID为 1001(1000+1)  
        """
    )
    val userStartIndex: Int by value(1000)

    @ValueDescription(
        """
        默认用户名的选择
        当为 "nick" 时 采用用户的qq昵称
        当为 "name" 时 采用用户的注册群卡片名称(如果不是在群内 则采取昵称)
        如果为其他则以值作为标注
        
    """
    )
    val userNickSource: String by value("nick")

    @ValueDescription(
        """
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

@kotlinx.serialization.Serializable
/**
 * 对[Command]的包装 由此实现序列化的多态化
 * */
internal class CommandPolymorphism(
    val value: Command
)

internal fun Command.toPolymorphismObject() = CommandPolymorphism(this)

@ValueDescription("指令注册")
object CommandConfigs : AutoSavePluginConfig(YamlCommandDecoder.saveName){
    @ValueDescription("指令处理")
    internal var COMMAND: List<CommandPolymorphism> by value(
        listOf(
            NormalCommand(
                name = listOf("test"),
                answeringMethod = AnsweringMethod.QUOTE,
                answerContent = """
                hello,world!
                this is a test message!
            """.trimIndent(),
                run = listOf(),
            ).toPolymorphismObject(),
            NormalCommand(
                name = listOf("/hikokoto"),
                answeringMethod = AnsweringMethod.QUOTE,
                answerContent = "『 %call-hitokoto% 』 —— %call-from%",
                run = listOf(TemplateYML(ImportType.HTTP, "content",
                    listOf("https://v1.hitokoto.cn"), name = "content"),
                    TemplateYML(ImportType.BASE,"parseJson", listOf("%call-content%","hitokoto"),"hitokoto"),
                    TemplateYML(ImportType.BASE,"parseJson", listOf("%call-content%","from"),"from")
                ),
            ).toPolymorphismObject(),
            OnlyRunCommand(
                name = listOf("/run"),
                condition = listOf(),
                run = listOf()
            ).toPolymorphismObject(),
            ArgCommand(
                name = listOf("/arg"),
                answeringMethod = AnsweringMethod.QUOTE,
                answerContent = "『 %call-arg1% 』 —— %call-arg2%",
                argsSize = 2,
                condition = listOf(),
                run = listOf()
            ).toPolymorphismObject()
        )

    )
}

object ListenerConfigs : AutoSavePluginConfig(YamlEventListenerDecoder.saveName) {
    internal var listener: List<EventListener> by value(
        listOf(
            //example
            EventListener(
                type = "MemberJoinEvent",
                run = listOf(
                    TemplateYML(
                        ImportType.MESSAGE_MANAGER,
                        "sendMessageToGroup",
                        parameterOf("%call-group%", "欢迎新人！"),
                        "only_run"
                    )
                )
            )
        )
    )
}
