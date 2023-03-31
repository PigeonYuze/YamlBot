@file:OptIn(MiraiInternalApi::class)
@file:JvmName("BotEventsImplKt")

package com.pigeonyuze.listener.impl.data

import com.pigeonyuze.command.element.NullObject
import com.pigeonyuze.listener.impl.BaseListenerImpl
import com.pigeonyuze.listener.impl.EventSubclassImpl
import com.pigeonyuze.listener.impl.Listener
import com.pigeonyuze.listener.impl.ListenerImpl
import net.mamoe.mirai.event.AbstractEvent
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.utils.MiraiInternalApi
import kotlin.reflect.KClass

/* BotEvents from: https://docs.mirai.mamoe.net/EventList.html
Bot 登录完成: BotOnlineEvent
Bot 离线: BotOfflineEvent
 |-主动: Active
 |-被挤下线: Force
 |-被服务器断开或因网络问题而掉线: Dropped
 |-服务器主动要求更换另一个服务器: RequireReconnect
Bot 重新登录: BotReloginEvent
Bot 头像改变: BotAvatarChangedEvent
Bot 昵称改变: BotNickChangedEvent
Bot 被戳: NudgeEvent
*/
internal interface BotEventListenerImpl<K> where K : BotEvent, K : AbstractEvent {
    fun addBaseBotTemplate(event: K, template: MutableMap<String, Any>) {
        template["bot"] = event.bot
        template["botid"] = event.bot.id
        template["isIntercepted"] = event.isIntercepted
        template["it"] = event
        template["isCancelled"] = event.isCancelled
    }

    companion object BotListener : Listener {
        override fun searchBuildListenerOrNull(name: String, template: MutableMap<String, Any>): ListenerImpl<*>? {
            return when (name) {
                "BotOnlineEvent" -> BotOnlineEventListener(template)
                "BotOfflineEvent" -> BotOfflineEventListener(template)
                "BotReloginEvent" -> BotReloginEventListener(template)
                "BotAvatarChangedEvent" -> BotAvatarChangedEventListener(template)
                "BotNickChangedEvent" -> BotNickChangedEventListener(template)
                "NudgeEvent" -> NudgeEventListener(template)
                else -> null
            }
        }
    }

}

private class BotOnlineEventListener(template: MutableMap<String, Any>) :
    BaseListenerImpl<BotOnlineEvent>(template), BotEventListenerImpl<BotOnlineEvent> {

    override val eventClass: KClass<BotOnlineEvent>
        get() = BotOnlineEvent::class

    override fun addTemplateImpl(event: BotOnlineEvent) {
        addBaseBotTemplate(event, template)
    }
}

private class BotOfflineEventListener(template: MutableMap<String, Any>) :
    BaseListenerImpl<BotOfflineEvent>(template), BotEventListenerImpl<BotOfflineEvent>,
    EventSubclassImpl<BotOfflineEvent> {

    override val subclassList: List<ListenerImpl<out BotOfflineEvent>>
        get() = listOf(Active(), Force(), Dropped(), RequireReconnect())

    override fun findSubclass(name: String): ListenerImpl<out BotOfflineEvent> {
        return when (name) {
            "Active" -> subclassList[0]
            "Force" -> subclassList[1]
            "Dropped" -> subclassList[2]
            "RequireReconnect" -> subclassList[3]
            else -> throw IllegalArgumentException("Cannot find $name")
        }
    }

    inner class Active : BaseListenerImpl<BotOfflineEvent.Active>(template) {
        override val eventClass: KClass<BotOfflineEvent.Active>
            get() = BotOfflineEvent.Active::class

        override fun addTemplateImpl(event: BotOfflineEvent.Active) {
            addBaseBotTemplate(event, template)
            template["isCancelled"] = event.isCancelled
            template["reconnect"] = event.reconnect
            template["cause"] = event.cause ?: NullObject
        }
    }

    inner class Force : BaseListenerImpl<BotOfflineEvent.Force>(template) {
        override val eventClass: KClass<BotOfflineEvent.Force>
            get() = BotOfflineEvent.Force::class

        override fun addTemplateImpl(event: BotOfflineEvent.Force) {
            addBaseBotTemplate(event, template)
            template["title"] = event.title
            template["message"] = event.message
            template["msg"] = event.message
            template["isCancelled"] = event.isCancelled
            template["reconnect"] = event.reconnect
        }
    }

    inner class Dropped : BaseListenerImpl<BotOfflineEvent.Dropped>(template) {
        override val eventClass: KClass<BotOfflineEvent.Dropped>
            get() = BotOfflineEvent.Dropped::class

        override fun addTemplateImpl(event: BotOfflineEvent.Dropped) {
            addBaseBotTemplate(event, template)
            template["reconnect"] = event.reconnect
            template["cause"] = event.cause ?: NullObject
        }
    }

    inner class RequireReconnect : BaseListenerImpl<BotOfflineEvent.RequireReconnect>(template) {
        override val eventClass: KClass<BotOfflineEvent.RequireReconnect>
            get() = BotOfflineEvent.RequireReconnect::class

        override fun addTemplateImpl(event: BotOfflineEvent.RequireReconnect) {
            template["cause"] = event.cause ?: NullObject
            template["reconnect"] = event.reconnect
        }
    }

    override val eventClass: KClass<BotOfflineEvent>
        get() = BotOfflineEvent::class

    override fun addTemplateImpl(event: BotOfflineEvent) {
        addBaseBotTemplate(event, template)
    }
}

private class BotReloginEventListener(template: MutableMap<String, Any>) :
    BaseListenerImpl<BotReloginEvent>(template), BotEventListenerImpl<BotReloginEvent> {
    override val eventClass: KClass<BotReloginEvent>
        get() = BotReloginEvent::class

    override fun addTemplateImpl(event: BotReloginEvent) {
        addBaseBotTemplate(event, template)
        template["cause"] = event.cause ?: NullObject
    }
}

private class BotAvatarChangedEventListener(template: MutableMap<String, Any>) :
    BaseListenerImpl<BotAvatarChangedEvent>(template), BotEventListenerImpl<BotAvatarChangedEvent> {
    override val eventClass: KClass<BotAvatarChangedEvent>
        get() = BotAvatarChangedEvent::class

    override fun addTemplateImpl(event: BotAvatarChangedEvent) {
        addBaseBotTemplate(event, template)
    }
}

private class BotNickChangedEventListener(template: MutableMap<String, Any>) :
    BaseListenerImpl<BotNickChangedEvent>(template), BotEventListenerImpl<BotNickChangedEvent> {
    override val eventClass: KClass<BotNickChangedEvent>
        get() = BotNickChangedEvent::class

    override fun addTemplateImpl(event: BotNickChangedEvent) {
        addBaseBotTemplate(event, template)
        template["from"] = event.from
        template["to"] = event.to
    }
}

private class NudgeEventListener(template: MutableMap<String, Any>) : BaseListenerImpl<NudgeEvent>(template),
    BotEventListenerImpl<NudgeEvent> {
    override val eventClass: KClass<NudgeEvent>
        get() = NudgeEvent::class

    override fun addTemplateImpl(event: NudgeEvent) {
        addBaseBotTemplate(event, template)
        template["from"] = event.target
        template["target"] = event.target
        template["subject"] = event.subject
        template["suffix"] = event.suffix
        template["action"] = event.action
    }
}