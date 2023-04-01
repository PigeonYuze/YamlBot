package com.pigeonyuze

import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.PlainText
import java.text.SimpleDateFormat
import java.util.*

@Suppress("UNUSED")
object LoggerManager {

    private inline val notRun get() = !LoggerConfig.open

    private val miraiLogger = YamlBot.logger

    private const val defaultfrom = "YamlBot"


    @JvmOverloads
    @JvmStatic
    fun loggingError(from: Any = defaultfrom, message: String?) {
        if (notRun) return
        miraiLogger.error("[$from] $message")
        trySendLogMessage(message ?: "")
    }

    @JvmOverloads
    @JvmStatic
    fun loggingWarn(from: Any = defaultfrom, message: String?) {
        if (notRun) return
        miraiLogger.warning("[$from] $message")
    }

    @JvmOverloads
    @JvmStatic
    fun loggingInfo(from: Any = defaultfrom, message: String?) {
        if (notRun) return
        miraiLogger.info("[$from] $message")
    }

    @JvmOverloads
    @JvmStatic
    fun loggingDebug(from: Any = defaultfrom, message: String?) {
        if (notRun) return
        miraiLogger.debug("[$from] $message")
    }

    @JvmOverloads
    @JvmStatic
    fun loggingTrace(from: Any = defaultfrom, message: String?) {
        if (notRun) return
        miraiLogger.verbose("[$from] $message")
    }

    private fun trySendLogMessage(message: String) {
        val bot = BotsTool.firstBot
        val group = BotsTool.getGroupOrNullJava(LoggerConfig.debugGroup) ?: return
        val forwardMessageBuilder = ForwardMessageBuilder(group)
        forwardMessageBuilder.add(
            bot,
            PlainText("${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())} log message\nlevel type = ERROR")
        )
        forwardMessageBuilder.add(bot, PlainText(message))
        runBlocking {
            group.sendMessage(
                forwardMessageBuilder.build()
                    .copy(title = "Log信息", summary = "level = ERROR", preview = listOf("(●'◡'●)"))
            )
        }
    }

}