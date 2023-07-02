package com.pigeonyuze

import com.pigeonyuze.command.Command
import com.pigeonyuze.util.setting.runConfigsReload
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.BotOfflineEvent
import net.mamoe.mirai.event.events.BotOnlineEvent
import net.mamoe.mirai.event.events.MessageEvent

val runningBots: MutableList<Bot> = mutableListOf()

internal var isDebugging0 = false

object YamlBot : KotlinPlugin(
    JvmPluginDescription(
        id = "com.pigeonyuze.yaml-bot",
        name = "YamlBot",
        version = "1.7.0",
    ) {
        author("Pigeon_Yuze")
    }
) {
    private val commandList = mutableListOf<Command>()

    override fun onEnable() {
        logger.info("start init")
        runConfigsReload()

        commandList.clear()
        commandList.addAll(Command.commands)

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

    /**
     * 当 [isRunAllBots] 为 `true`时始终返回`null`
     * */
    suspend fun <R> runWithAllBots(isRunAllBots: Boolean = false, run: suspend (Bot) -> R): R? {
        for (bot in runningBots) {
            if (isRunAllBots) run.invoke(bot)
            else return run.invoke(bot) ?: continue
        }
        return null
    }

    fun <R> runWithAllBotsJava(run: suspend (Bot) -> R) = runBlocking {
        runWithAllBots(run = run)
    }

    fun getGroupOrNullJava(groupId: Long) =
        runWithAllBotsJava {
            return@runWithAllBotsJava it.getGroup(groupId)
        }

    suspend fun getGroupOrNull(groupId: Long) =
        runWithAllBots {
            return@runWithAllBots it.getGroup(groupId)
        }

    suspend fun getFriendOrNull(friendId: Long) =
        runWithAllBots {
            return@runWithAllBots it.getFriend(friendId)
        }

    suspend fun getAllGroup(): Set<Group> {
        val groups = mutableSetOf<Group>()
        runWithAllBots(true) {
            groups += it.groups
        }
        return groups
    }

    suspend fun getAllFriend(): Set<Friend> {
        val groups = mutableSetOf<Friend>()
        runWithAllBots(true) {
            groups += it.friends
        }
        return groups
    }
}