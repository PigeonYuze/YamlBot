@file:JvmName("FriendEventsImplKt")

package com.pigeonyuze.listener.impl.data

import com.pigeonyuze.command.element.NullObject
import com.pigeonyuze.listener.impl.BaseListenerImpl
import com.pigeonyuze.listener.impl.Listener
import com.pigeonyuze.listener.impl.ListenerImpl
import com.pigeonyuze.listener.impl.template.EventTemplateValues
import com.pigeonyuze.listener.impl.template.buildEventTemplate
import net.mamoe.mirai.event.AbstractEvent
import net.mamoe.mirai.event.events.*
import kotlin.reflect.KClass

/*
好友昵称改变: FriendRemarkChangeEvent
成功添加了一个新好友: FriendAddEvent
好友已被删除: FriendDeleteEvent
一个账号请求添加机器人为好友: NewFriendRequestEvent
好友头像改变: FriendAvatarChangedEvent
好友昵称改变: FriendNickChangedEvent
好友输入状态改变: FriendInputStatusChangedEvent
* */

internal interface FriendEventsListenerImpl<K> : BotEventListenerImpl<K> where K : FriendEvent, K : AbstractEvent {
    override fun addBaseBotTemplate(event: K, template: MutableMap<String, Any>) {
        super.addBaseBotTemplate(event, template)
        template["friend"] = event.friend
        template["user"] = event.user
    }

    companion object EventListener : Listener {
        override fun searchBuildListenerOrNull(name: String, template: MutableMap<String, Any>): ListenerImpl<*>? {
            return when (name) {
                "FriendRemarkChangeEvent" -> FriendRemarkChangeEventListener(template)
                "FriendAddEvent" -> FriendAddEventListener(template)
                "FriendDeleteEvent" -> FriendDeleteEventListener(template)
                "NewFriendRequestEvent" -> NewFriendRequestEventListener(template)
                "FriendAvatarChangedEvent" -> FriendAvatarChangedEventListener(template)
                "FriendNickChangedEvent" -> FriendNickChangedEventListener(template)
                "FriendInputStatusChangedEvent" -> FriendInputStatusChangedEventListener(template)
                else -> null
            }
        }
    }
}

private class FriendRemarkChangeEventListener(template: MutableMap<String, Any>) :
    FriendEventsListenerImpl<FriendRemarkChangeEvent>, BaseListenerImpl<FriendRemarkChangeEvent>(template) {
    override val eventClass: KClass<FriendRemarkChangeEvent>
        get() = FriendRemarkChangeEvent::class

    override fun addTemplateImpl(event: FriendRemarkChangeEvent) {
        super.addTemplateImpl(event, template)
        template["oldRemark"] = event.oldRemark
        template["newRemark"] = event.newRemark
    }
}

private class FriendAddEventListener(template: MutableMap<String, Any>) : FriendEventsListenerImpl<FriendAddEvent>,
    BaseListenerImpl<FriendAddEvent>(template) {
    override val eventClass: KClass<FriendAddEvent>
        get() = FriendAddEvent::class

    override fun addTemplateImpl(event: FriendAddEvent) {
        super.addTemplateImpl(event, template)
    }
}

private class FriendDeleteEventListener(template: MutableMap<String, Any>) :
    FriendEventsListenerImpl<FriendDeleteEvent>, BaseListenerImpl<FriendDeleteEvent>(template) {
    override val eventClass: KClass<FriendDeleteEvent>
        get() = FriendDeleteEvent::class

    override fun addTemplateImpl(event: FriendDeleteEvent) {
        super.addTemplateImpl(event, template)
    }
}

// It doesn't extend FriendEvent...
private class NewFriendRequestEventListener(template: MutableMap<String, Any>) :
    BotEventListenerImpl<NewFriendRequestEvent>, BaseListenerImpl<NewFriendRequestEvent>(template) {
    override val eventClass: KClass<NewFriendRequestEvent>
        get() = NewFriendRequestEvent::class

    override val eventTemplate: EventTemplateValues<NewFriendRequestEvent>
        get() = buildEventTemplate {
            "intercept" execute {
                intercept()
            }
            "accept" execute {
                this.accept()
            }
            "reject" execute {
                this.reject(it.getOrNull(0)?.toBoolean() ?: false)
            }
        }

    override fun addTemplateImpl(event: NewFriendRequestEvent) {
        super.addBaseBotTemplate(event, template)
        template["eventId"] = event.eventId
        template["fromId"] = event.fromId
        template["message"] = event.message
        template["fromNick"] = event.fromNick
        template["fromGroupId"] = event.fromGroupId
        template["fromGroup"] = event.fromGroup ?: NullObject
    }
}

private class FriendAvatarChangedEventListener(template: MutableMap<String, Any>) :
    FriendEventsListenerImpl<FriendAvatarChangedEvent>, BaseListenerImpl<FriendAvatarChangedEvent>(template) {
    override val eventClass: KClass<FriendAvatarChangedEvent>
        get() = FriendAvatarChangedEvent::class

    override fun addTemplateImpl(event: FriendAvatarChangedEvent) {
        super.addBaseBotTemplate(event, template)
    }
}

private class FriendNickChangedEventListener(template: MutableMap<String, Any>) :
    FriendEventsListenerImpl<FriendNickChangedEvent>, BaseListenerImpl<FriendNickChangedEvent>(template) {
    override val eventClass: KClass<FriendNickChangedEvent>
        get() = FriendNickChangedEvent::class

    override fun addTemplateImpl(event: FriendNickChangedEvent) {
        super.addBaseBotTemplate(event, template)
        template["from"] = event.from
        template["to"] = event.to
    }
}

private class FriendInputStatusChangedEventListener(template: MutableMap<String, Any>) :
    FriendEventsListenerImpl<FriendInputStatusChangedEvent>, BaseListenerImpl<FriendInputStatusChangedEvent>(template) {
    override val eventClass: KClass<FriendInputStatusChangedEvent>
        get() = FriendInputStatusChangedEvent::class

    override fun addTemplateImpl(event: FriendInputStatusChangedEvent) {
        super.addBaseBotTemplate(event, template)
        template["inputting"] = event.inputting
        template["from"] = !event.inputting
        template["to"] = event.inputting
    }
}