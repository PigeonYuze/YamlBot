package com.pigeonyuze

import com.pigeonyuze.command.Command
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.BotOfflineEvent
import net.mamoe.mirai.event.events.BotOnlineEvent
import net.mamoe.mirai.event.events.MessageEvent

val runningBots: MutableList<Bot> = mutableListOf()

object YamlBot : KotlinPlugin(
    JvmPluginDescription(
        id = "com.pigeonyuze.yaml-bot",
        name = "YamlBot",
        version = "1.5.0",
    ) {
        author("Pigeon_Yuze")
    }
) {
    private val commandList = mutableListOf<Command>()

    override fun onEnable() {
        logger.info("start init")
        runConfigsReload()

        for (command in CommandConfigs.COMMAND) {
            commandList.add(command.value)
        }

        val parentScope = GlobalEventChannel.parentScope(this)

        parentScope.subscribeAlways<MessageEvent> {
            commandList.filter {
                it.isThis(this.message.contentToString())
            }.getOrNull(0)?.run(this)
        }

        parentScope.subscribeAlways<BotOnlineEvent> {
            if (runningBots.contains(bot)) return@subscribeAlways
            runningBots.add(bot)
        }

        parentScope.subscribeAlways<BotOfflineEvent> {
            if (runningBots.contains(bot)) runningBots.remove(bot)
        }

    }
}

object BotsTool {
    val firstBot = runningBots.first()

    suspend fun <R> runWithAllBots(run: suspend (Bot) -> R): R? {
        for (bot in runningBots) {
            return run.invoke(bot) ?: continue
        }
        return null
    }

    fun <R> runWithAllBotsJava(run: suspend (Bot) -> R) = runBlocking {
        runWithAllBots(run)
    }

    fun getGroupOrNullJava(groupId: Long) =
        runWithAllBotsJava {
            return@runWithAllBotsJava it.getGroup(groupId)
        }

    suspend fun getGroupOrNull(groupId: Long) =
        runWithAllBots {
            return@runWithAllBots it.getGroup(groupId)
        }


}