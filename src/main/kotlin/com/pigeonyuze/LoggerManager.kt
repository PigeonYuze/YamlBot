package com.pigeonyuze.com.pigeonyuze

import com.pigeonyuze.BotsTool
import com.pigeonyuze.YamlBot
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.PlainText
import java.text.SimpleDateFormat
import java.util.*

object LoggerManager {


    private val miraiLogger = YamlBot.logger

    private const val defaultfrom = "YamlBot"


    @JvmOverloads
    @JvmStatic
    fun loggingError(from: Any = defaultfrom, message: String?) {
        miraiLogger.error("[$from] $message")
        trySendLogMessage(message ?: "")
    }

    fun loggingWarn(from: Any = defaultfrom, message: String?){
        miraiLogger.warning("[$from] $message")
    }

    @JvmOverloads
    @JvmStatic
    fun loggingInfo(from: Any = defaultfrom, message: String?) {
         miraiLogger.info("[$from] $message")
    }

    @JvmOverloads
    @JvmStatic
    fun loggingDebug(from: Any = defaultfrom, message: String?) {
        miraiLogger.debug("[$from] $message")
    }

    @JvmOverloads
    @JvmStatic
    fun loggingTrace(from: Any = defaultfrom, message: String?) {
        miraiLogger.verbose("[$from] $message")
    }

    private fun trySendLogMessage(message: String) {
        val bot = BotsTool.firstBot
        val group = BotsTool.getGroupOrNullJava(1L) ?: return
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