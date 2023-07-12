package com.pigeonyuze

import com.pigeonyuze.util.setting.LoggerConfig
import kotlinx.coroutines.*
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.PlainText
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Suppress("UNUSED")
object LoggerManager : CoroutineScope {

    override val coroutineContext: CoroutineContext =
        CoroutineExceptionHandler { _, error -> loggingError(error) } +
                if (!isDebugging0) YamlBot.coroutineContext else EmptyCoroutineContext +
                        SupervisorJob(if (!isDebugging0) YamlBot.coroutineContext[Job] else null)

    private val miraiLogger by lazy { YamlBot.logger }

    private const val defaultfrom = "YamlBot"

    private val dateFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @JvmOverloads
    @JvmStatic
    fun loggingError(from: Any = defaultfrom, message: String?) {
        if (!LoggerConfig.open) return
        if (isDebugging0) {
            System.err.println("ERROR ${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)} [$from] $message")
            return
        }
        miraiLogger.error("[$from] $message")
        trySendLogMessage("ERROR", from.toString(), message ?: "")
    }

    fun loggingError(error: Throwable) {
        if (!LoggerConfig.open) return
        if (isDebugging0) {
            System.err.println("ERROR ${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)} [${error.javaClass.simpleName}] ${error.message ?: ""}")
            return
        }
        miraiLogger.error(error)
        trySendLogMessage("ERROR", error.javaClass.simpleName, error.message ?: "")
    }

    @JvmOverloads
    @JvmStatic
    fun loggingWarn(from: Any = defaultfrom, message: String?) {
        if (!LoggerConfig.open) return
        if (isDebugging0) {
            println("WARN ${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)} [$from] $message")
            return
        }
        miraiLogger.warning("[$from] $message")
    }

    @JvmOverloads
    @JvmStatic
    fun loggingInfo(from: Any = defaultfrom, message: String?) {
        if (!LoggerConfig.open) return
        if (isDebugging0) {
            println("INFO ${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)} [$from] $message")
            return
        }
        miraiLogger.info("[$from] $message")
    }

    @JvmOverloads
    @JvmStatic
    fun loggingDebug(from: Any = defaultfrom, message: String?) {
        if (!LoggerConfig.open) return
        if (isDebugging0) {
            println("DEBUG ${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)} [$from] $message")
            return
        }
        miraiLogger.debug("[$from] $message")
    }

    @JvmOverloads
    @JvmStatic
    fun loggingTrace(from: Any = defaultfrom, message: String?) {
        if (!LoggerConfig.open) return
        if (isDebugging0) {
            println("TRACE ${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)} [$from] $message")
            return
        }
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
