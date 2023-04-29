package com.pigeonyuze

import kotlinx.coroutines.*
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.PlainText
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.CoroutineContext

@Suppress("UNUSED")
object LoggerManager : CoroutineScope {

    override val coroutineContext: CoroutineContext = CoroutineExceptionHandler { _, error ->
        loggingError(error)
    } + YamlBot.coroutineContext + SupervisorJob(YamlBot.coroutineContext[Job])

    private val miraiLogger by lazy { YamlBot.logger }

    private const val defaultfrom = "YamlBot"

    private val dateFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @JvmOverloads
    @JvmStatic
    fun loggingError(from: Any = defaultfrom, message: String?) {
        if (!LoggerConfig.open) return
        miraiLogger.error("[$from] $message")
        trySendLogMessage("ERROR", from.toString(), message ?: "")
    }

    fun loggingError(error: Throwable) {
        if (!LoggerConfig.open) return
        miraiLogger.error(error)
        trySendLogMessage("ERROR", error.javaClass.simpleName, error.message ?: "")
    }

    @JvmOverloads
    @JvmStatic
    fun loggingWarn(from: Any = defaultfrom, message: String?) {
        if (!LoggerConfig.open) return
        miraiLogger.warning("[$from] $message")
    }

    @JvmOverloads
    @JvmStatic
    fun loggingInfo(from: Any = defaultfrom, message: String?) {
        if (!LoggerConfig.open) return
        miraiLogger.info("[$from] $message")
    }

    @JvmOverloads
    @JvmStatic
    fun loggingDebug(from: Any = defaultfrom, message: String?) {
        if (!LoggerConfig.open) return
        miraiLogger.debug("[$from] $message")
    }

    @JvmOverloads
    @JvmStatic
    fun loggingTrace(from: Any = defaultfrom, message: String?) {
        if (!LoggerConfig.open) return
        miraiLogger.verbose("[$from] $message")
    }

    @Suppress("SameParameterValue")
    private fun trySendLogMessage(level: String, from: String, message: String) {
        if (LoggerConfig.debugGroup == 0L) return
        launch {
            val group = BotsTool.getGroupOrNull(LoggerConfig.debugGroup) ?: return@launch

            val forwardMessageBuilder = ForwardMessageBuilder(group)
                .add(
                    group.bot,
                    PlainText("${LocalDateTime.now().format(dateFormat)} log message")
                )
                .add(
                    group.bot,
                    PlainText("level type = $level\nfrom = $from\nmessage = $message")
                )

            group.sendMessage(
                forwardMessageBuilder.build()
                    .copy(title = "Log信息", summary = "level = $level", preview = listOf("(●'◡'●)"))
            )
        }
    }

}
