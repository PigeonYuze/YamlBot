@file:OptIn(MiraiExperimentalApi::class)

package com.pigeonyuze.listener.impl.data

import com.pigeonyuze.command.element.NullObject
import com.pigeonyuze.listener.impl.BaseListenerImpl
import com.pigeonyuze.listener.impl.EventSubclassImpl
import com.pigeonyuze.listener.impl.Listener
import com.pigeonyuze.listener.impl.ListenerImpl
import net.mamoe.mirai.event.AbstractEvent
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.utils.MiraiExperimentalApi
import kotlin.reflect.KClass

/*
一个账号请求加入群: MemberJoinRequestEvent
机器人被邀请加入群: BotInvitedJoinGroupRequestEvent

机器人被踢出群或在其他客户端主动退出一个群： BotLeaveEvent
机器人主动退出一个群： Active
机器人被管理员或群主踢出群： Kick
机器人在群里的权限被改变： BotGroupPermissionChangeEvent
机器人被禁言： BotMuteEvent
机器人被取消禁言： BotUnmuteEvent
机器人成功加入了一个新群： BotJoinGroupEvent
* */
internal interface GroupEventsAboutBotListenerImpl<K> : BotEventListenerImpl<K> where  K : AbstractEvent, K : BotEvent {
    companion object EventListener : Listener {
        override fun searchBuildListenerOrNull(name: String, template: MutableMap<String, Any>): ListenerImpl<*>? {
            return when (name) {
                "MemberJoinRequestEvent" -> MemberJoinRequestEventListener(template)
                "BotInvitedJoinGroupRequestEvent" -> BotInvitedJoinGroupRequestEventListener(template)
                "BotLeaveEvent" -> BotLeaveEventListener(template)
                "BotGroupPermissionChangeEvent" -> BotGroupPermissionChangeEventListener(template)
                "BotMuteEvent" -> BotMuteEventListener(template)
                "BotUnmuteEvent" -> BotUnmuteEventListener(template)
                "BotJoinGroupEvent" -> BotJoinGroupEventListener(template)
                else -> null
            }
        }
    }
}

private class MemberJoinRequestEventListener(template: MutableMap<String, Any>) :
    GroupEventsAboutBotListenerImpl<MemberJoinRequestEvent>, BaseListenerImpl<MemberJoinRequestEvent>(template) {
    override val eventClass: KClass<MemberJoinRequestEvent>
        get() = MemberJoinRequestEvent::class

    override fun addTemplateImpl(event: MemberJoinRequestEvent) {
        super.addBaseBotTemplate(event, template)
        template["eventId"] = event.eventId
        template["message"] = event.message
        template["fromNick"] = event.fromNick
        template["fromId"] = event.fromId
        template["groupName"] = event.groupName
        template["invitor"] = event.invitor ?: NullObject
        template["invitorId"] = event.invitorId ?: -1L
        template["groupId"] = event.group?.id ?: -1L
    }
}

private class BotInvitedJoinGroupRequestEventListener(template: MutableMap<String, Any>) :
    GroupEventsAboutBotListenerImpl<BotInvitedJoinGroupRequestEvent>,
    BaseListenerImpl<BotInvitedJoinGroupRequestEvent>(template) {
    override val eventClass: KClass<BotInvitedJoinGroupRequestEvent>
        get() = BotInvitedJoinGroupRequestEvent::class


    override fun addTemplateImpl(event: BotInvitedJoinGroupRequestEvent) {
        super.addBaseBotTemplate(event, template)
        template["invitor"] = event.invitor ?: NullObject
        template["invitorId"] = event.invitorId
        template["groupName"] = event.groupName
        template["invitorNick"] = event.invitorNick
        template["groupId"] = event.groupId
    }
}

private class BotLeaveEventListener(template: MutableMap<String, Any>) : GroupEventsAboutBotListenerImpl<BotLeaveEvent>,
    BaseListenerImpl<BotLeaveEvent>(template), EventSubclassImpl<BotLeaveEvent> {
    override val subclassList: List<ListenerImpl<out BotLeaveEvent>>
        get() = listOf(Active(), Kick(), Disband())

    override fun findSubclass(name: String): ListenerImpl<out BotLeaveEvent> {
        return when (name) {
            "Active" -> subclassList[0]
            "Kick" -> subclassList[1]
            "Disband" -> subclassList[2]
            else -> throw NotImplementedError()
        }
    }

    override val eventClass: KClass<BotLeaveEvent>
        get() = BotLeaveEvent::class

    override fun addTemplateImpl(event: BotLeaveEvent) {
        super.addBaseBotTemplate(event, template)
        template["group"] = event.group
        template["groupId"] = event.groupId
    }

    inner class Kick : BaseListenerImpl<BotLeaveEvent.Kick>(template) {
        override val eventClass: KClass<BotLeaveEvent.Kick>
            get() = BotLeaveEvent.Kick::class

        override fun addTemplateImpl(event: BotLeaveEvent.Kick) {
            this@BotLeaveEventListener.addTemplateImpl(event)
            template["operator"] = event.operatorOrBot
            template["isByBot"] = event.isByBot
        }
    }

    inner class Active : BaseListenerImpl<BotLeaveEvent.Active>(template) {
        override val eventClass: KClass<BotLeaveEvent.Active>
            get() = BotLeaveEvent.Active::class

        override fun addTemplateImpl(event: BotLeaveEvent.Active) {
            this@BotLeaveEventListener.addTemplateImpl(event)
        }
    }

    inner class Disband : BaseListenerImpl<BotLeaveEvent.Disband>(template) {
        override val eventClass: KClass<BotLeaveEvent.Disband>
            get() = BotLeaveEvent.Disband::class

        override fun addTemplateImpl(event: BotLeaveEvent.Disband) {
            this@BotLeaveEventListener.addTemplateImpl(event)
            template["operator"] = event.operatorOrBot
            template["isByBot"] = event.isByBot
        }
    }
}

private class BotGroupPermissionChangeEventListener(template: MutableMap<String, Any>) :
    GroupEventsAboutBotListenerImpl<BotGroupPermissionChangeEvent>,
    BaseListenerImpl<BotGroupPermissionChangeEvent>(template) {
    override val eventClass: KClass<BotGroupPermissionChangeEvent>
        get() = BotGroupPermissionChangeEvent::class

    override fun addTemplateImpl(event: BotGroupPermissionChangeEvent) {
        super.addBaseBotTemplate(event, template)
        template["group"] = event.group
        template["groupId"] = event.groupId
        template["new"] = event.new
        template["origin"] = event.origin
        template["newName"] = event.new.name
        template["oldName"] = event.origin.name
        template["isUp"] = event.new > event.origin
        template["newLevel"] = event.new.level
        template["oldLevel"] = event.origin.level
    }
}

private class BotMuteEventListener(template: MutableMap<String, Any>) : GroupEventsAboutBotListenerImpl<BotMuteEvent>,
    BaseListenerImpl<BotMuteEvent>(template) {
    override val eventClass: KClass<BotMuteEvent>
        get() = BotMuteEvent::class

    override fun addTemplateImpl(event: BotMuteEvent) {
        super.addBaseBotTemplate(event, template)
        template["group"] = event.group
        template["groupId"] = event.groupId
        template["durationSeconds"] = event.durationSeconds
        template["operator"] = event.operator
    }
}

private class BotUnmuteEventListener(template: MutableMap<String, Any>) :
    GroupEventsAboutBotListenerImpl<BotUnmuteEvent>,
    BaseListenerImpl<BotUnmuteEvent>(template) {
    override val eventClass: KClass<BotUnmuteEvent>
        get() = BotUnmuteEvent::class

    override fun addTemplateImpl(event: BotUnmuteEvent) {
        super.addBaseBotTemplate(event, template)
        template["group"] = event.group
        template["groupId"] = event.groupId
        template["operator"] = event.operator

    }
}

private class BotJoinGroupEventListener(template: MutableMap<String, Any>) :
    GroupEventsAboutBotListenerImpl<BotJoinGroupEvent>,
    BaseListenerImpl<BotJoinGroupEvent>(template), EventSubclassImpl<BotJoinGroupEvent> {
    override val eventClass: KClass<BotJoinGroupEvent>
        get() = BotJoinGroupEvent::class

    override val subclassList: List<ListenerImpl<out BotJoinGroupEvent>>
        get() = listOf(Active(), Kick(), Retrieve())

    override fun findSubclass(name: String): ListenerImpl<out BotJoinGroupEvent> {
        return when (name) {
            "Active" -> subclassList[0]
            "Kick" -> subclassList[1]
            "Retrieve" -> subclassList[2]
            else -> throw NotImplementedError()
        }
    }

    override fun addTemplateImpl(event: BotJoinGroupEvent) {
        super.addTemplateImpl(event, template)
        template["group"] = event.group
        template["groupId"] = event.groupId
    }

    inner class Kick : BaseListenerImpl<BotJoinGroupEvent.Invite>(template) {
        override val eventClass: KClass<BotJoinGroupEvent.Invite>
            get() = BotJoinGroupEvent.Invite::class

        override fun addTemplateImpl(event: BotJoinGroupEvent.Invite) {
            this@BotJoinGroupEventListener.addTemplateImpl(event)
            template["invitor"] = event.invitor
        }
    }

    inner class Active : BaseListenerImpl<BotJoinGroupEvent.Active>(template) {
        override val eventClass: KClass<BotJoinGroupEvent.Active>
            get() = BotJoinGroupEvent.Active::class

        override fun addTemplateImpl(event: BotJoinGroupEvent.Active) {
            this@BotJoinGroupEventListener.addTemplateImpl(event)
        }
    }

    inner class Retrieve : BaseListenerImpl<BotJoinGroupEvent.Retrieve>(template) {
        override val eventClass: KClass<BotJoinGroupEvent.Retrieve>
            get() = BotJoinGroupEvent.Retrieve::class

        override fun addTemplateImpl(event: BotJoinGroupEvent.Retrieve) {
            this@BotJoinGroupEventListener.addTemplateImpl(event)
        }
    }
}