package com.pigeonyuze.util.setting

import com.pigeonyuze.LoggerManager
import com.pigeonyuze.YamlBot
import com.pigeonyuze.YamlBot.reload
import com.pigeonyuze.account.UserElement
import com.pigeonyuze.listener.EventListener
import com.pigeonyuze.listener.impl.Listener.Companion.execute
import com.pigeonyuze.util.decode.CommandConfigDecoder
import com.pigeonyuze.util.decode.ListenerConfigDecoder
import com.pigeonyuze.util.logger.BeautifulError
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.yaml.YamlParser
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
import java.util.jar.JarFile

fun runConfigsReload() {
    runBlocking {
        UserConfig.reload()
        readCommands()
        LoggerConfig.reload()
        readListeners()
        ListenerConfigs.startAllListener()
    }
}

private fun readCommands() {
    val codes = YamlBot.configFolderPath.resolve("command").toFile()

    if (!codes.exists()) {
        /* WARNING: you must call it form jar! */
        /* Tip: Don't use Class.getResource(...) , because it never given you a directory file object */
        /* To get resource directory files, please get it by JarFile */
        codes.mkdirs()

        val currentJarFile = JarFile(
            /* Get current jar/class running path */
            Class.forName("com.pigeonyuze.YamlBot").protectionDomain.codeSource.location.file
        )
        val jarFiles = currentJarFile.entries()
        while (jarFiles.hasMoreElements()) {
            val jarEntry = jarFiles.nextElement()
            jarEntry.name.startsWith("default_files/commands") || continue
            !jarEntry.isDirectory || continue
            val outputFile = codes.resolve(jarEntry.name.substringAfterLast('/'))
            currentJarFile.getInputStream(jarEntry)
                .copyTo(outputFile.outputStream())
        }
        LoggerManager.loggingWarn(
            "read-commands",
            "No commands in 'config/commands' found! To init with default values and create default files."
        )
    }

    require(codes.isDirectory) { "Error: File $codes must be a directory" }
    val yamlParser = YamlParser()
    val configLoader = ConfigLoaderBuilder.default()
        .addParser("yml",yamlParser)
        .addParser("yaml",yamlParser)
        .build()
    for (codeFile in codes.listFiles()!!) {
        try {
            CommandConfigDecoder.handle(configLoader.loadNodeOrThrow(codeFile.path))
        }catch (e: BeautifulError) {
            LoggerManager.loggingError(e)
            continue
        }
    }
}

private fun readListeners() {
    val codes = YamlBot.configFolderPath.resolve("listeners").toFile()

    if (!codes.exists()) {
        /* WARNING: you must call it form jar! */
        /* Tip: Don't use Class.getResource(...) , because it never given you a directory file object */
        /* To get resource directory files, please get it by JarFile */
        codes.mkdirs()

        val currentJarFile = JarFile(
            /* Get current jar/class running path */
            Class.forName("com.pigeonyuze.YamlBot").protectionDomain.codeSource.location.file
        )
        val jarFiles = currentJarFile.entries()
        while (jarFiles.hasMoreElements()) {
            val jarEntry = jarFiles.nextElement()
            jarEntry.name.startsWith("default_files/listeners") || continue
            !jarEntry.isDirectory || continue
            val outputFile = codes.resolve(jarEntry.name.substringAfterLast('/'))
            currentJarFile.getInputStream(jarEntry)
                .copyTo(outputFile.outputStream())
        }
        LoggerManager.loggingWarn(
            "read-listeners",
            "No commands in 'config/listeners' found! To init with default values and create default files."
        )
    }

    require(codes.isDirectory) { "Error: File $codes must be a directory" }
    val yamlParser = YamlParser()
    val configLoader = ConfigLoaderBuilder.default()
        .addParser("yml",yamlParser)
        .addParser("yaml",yamlParser)
        .build()
    for (codeFile in codes.listFiles()!!) {
        try {
            ListenerConfigDecoder.handle(configLoader.loadNodeOrThrow(codeFile.path))
        }catch (e: BeautifulError) {
            LoggerManager.loggingError(e)
            continue
        }
    }
}

suspend fun ListenerConfigs.startAllListener() {
    LoggerManager.loggingDebug("startAllListener", "Try to start all listeners...")
    for ((index, eventListener) in listener.withIndex()) {
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
    """
    )
    val otherElements: MutableList<UserElement> by value(mutableListOf(UserElement("regDate", "date", "new")))

}


object ListenerConfigs {
    internal var listener: List<EventListener> = arrayListOf()
}
