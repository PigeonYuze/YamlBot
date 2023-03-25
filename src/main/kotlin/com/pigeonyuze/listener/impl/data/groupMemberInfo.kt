@file:OptIn(MiraiExperimentalApi::class)

package com.pigeonyuze.listener.impl.data

import com.pigeonyuze.listener.impl.BaseListenerImpl
import com.pigeonyuze.listener.impl.EventSubclassImpl
import com.pigeonyuze.listener.impl.Listener
import com.pigeonyuze.listener.impl.ListenerImpl
import com.pigeonyuze.template.data.GroupActiveTemplate.GroupActiveTemplateImpl.QueryHonorHistoryImpl.nameIdMapping
import net.mamoe.mirai.event.AbstractEvent
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.utils.MiraiExperimentalApi
import kotlin.reflect.KClass

/*
成员群名片改动： MemberCardChangeEvent
成员群特殊头衔改动： MemberSpecialTitleChangeEvent
成员权限改变： MemberPermissionChangeEvent
群成员被禁言： MemberMuteEvent
群成员被取消禁言： MemberUnmuteEvent
群荣誉改动： MemberHonorChangeEventListener
成员已经加入群： MemberJoinEvent
成员离开群： MemberLeaveEvent
*/

internal interface GroupMemberInfoEventsListenerImpl<K> :
    BotEventListenerImpl<K> where K : AbstractEvent, K : GroupMemberEvent {
    override fun addBaseBotTemplate(event: K, template: MutableMap<String, Any>) {
        super.addBaseBotTemplate(event, template)
        template["group"] = event.group
        template["member"] = event.member
        template["user"] = event.user
        template["groupId"] = event.group.id
    }

    companion object EventListener : Listener {
        override fun searchBuildListenerOrNull(name: String, template: MutableMap<String, Any>): ListenerImpl<*>? {
            return when (name) {
                "MemberCardChangeEvent" -> MemberCardChangeEventListener(template)
                "MemberSpecialTitleChangeEvent" -> MemberSpecialTitleChangeEventListener(template)
                "MemberPermissionChangeEvent" -> MemberPermissionChangeEventListener(template)
                "MemberMuteEvent" -> MemberMuteEventListener(template)
                "MemberUnmuteEvent" -> MemberUnmuteEventListener(template)
                "MemberHonorChangeEvent" -> MemberHonorChangeEventListener(template)
                "MemberLeaveEvent" -> MemberLeaveEventListener(template)
                "MemberJoinEvent" -> MemberJoinEventListener(template)
                else -> null
            }
        }
    }
}

private class MemberCardChangeEventListener(template: MutableMap<String, Any>) :
    GroupMemberInfoEventsListenerImpl<MemberCardChangeEvent>, BaseListenerImpl<MemberCardChangeEvent>(template) {
    override val eventClass: KClass<MemberCardChangeEvent>
        get() = MemberCardChangeEvent::class

    override fun addTemplateImpl(event: MemberCardChangeEvent) {
        super.addBaseBotTemplate(event, template)
        template["new"] = event.new
        template["origin"] = event.origin
    }
}

private class MemberSpecialTitleChangeEventListener(template: MutableMap<String, Any>) :
    GroupMemberInfoEventsListenerImpl<MemberSpecialTitleChangeEvent>,
    BaseListenerImpl<MemberSpecialTitleChangeEvent>(template) {
    override val eventClass: KClass<MemberSpecialTitleChangeEvent>
        get() = MemberSpecialTitleChangeEvent::class

    override fun addTemplateImpl(event: MemberSpecialTitleChangeEvent) {
        super.addBaseBotTemplate(event, template)
        template["new"] = event.new
        template["origin"] = event.origin
        template["operator"] = event.operatorOrBot
        template["isByBot"] = event.isByBot

    }
}

private class MemberPermissionChangeEventListener(template: MutableMap<String, Any>) :
    GroupMemberInfoEventsListenerImpl<MemberPermissionChangeEvent>,
    BaseListenerImpl<MemberPermissionChangeEvent>(template) {
    override val eventClass: KClass<MemberPermissionChangeEvent>
        get() = MemberPermissionChangeEvent::class

    override fun addTemplateImpl(event: MemberPermissionChangeEvent) {
        super.addBaseBotTemplate(event, template)
        template["new"] = event.new
        template["origin"] = event.origin
        template["newName"] = event.new.name
        template["oldName"] = event.origin.name
        template["isUp"] = event.new > event.origin
        template["newLevel"] = event.new.level
        template["oldLevel"] = event.origin.level
    }
}

private class MemberMuteEventListener(template: MutableMap<String, Any>) :
    GroupMemberInfoEventsListenerImpl<MemberMuteEvent>, BaseListenerImpl<MemberMuteEvent>(template) {
    override val eventClass: KClass<MemberMuteEvent>
        get() = MemberMuteEvent::class

    override fun addTemplateImpl(event: MemberMuteEvent) {
        super.addBaseBotTemplate(event, template)
        template["durationSeconds"] = event.durationSeconds
        template["operator"] = event.operatorOrBot
        template["isByBot"] = event.isByBot
    }
}

private class MemberUnmuteEventListener(template: MutableMap<String, Any>) :
    GroupMemberInfoEventsListenerImpl<MemberUnmuteEvent>, BaseListenerImpl<MemberUnmuteEvent>(template) {
    override val eventClass: KClass<MemberUnmuteEvent>
        get() = MemberUnmuteEvent::class

    override fun addTemplateImpl(event: MemberUnmuteEvent) {
        super.addBaseBotTemplate(event, template)
        template["operator"] = event.operatorOrBot
        template["isByBot"] = event.isByBot
    }
}

private class MemberHonorChangeEventListener(template: MutableMap<String, Any>) :
    GroupMemberInfoEventsListenerImpl<MemberHonorChangeEvent>, BaseListenerImpl<MemberHonorChangeEvent>(template),
    EventSubclassImpl<MemberHonorChangeEvent> {
    override val eventClass: KClass<MemberHonorChangeEvent>
        get() = MemberHonorChangeEvent::class

    override val subclassList: List<ListenerImpl<out MemberHonorChangeEvent>>
        get() = listOf(Achieve(), Lose())

    override fun addTemplateImpl(event: MemberHonorChangeEvent) {
        super.addBaseBotTemplate(event, template)
        template["honorType"] = event.honorType
        template["name"] = nameIdMapping.entries.forEach {
            if (it.value != event.honorType.id) {
                return@forEach
            }
            it.key
        }
    }

    override fun findSubclass(name: String): ListenerImpl<out MemberHonorChangeEvent> {
        return when (name) {
            "Achieve" -> subclassList[0]
            "Lose" -> subclassList[1]
            else -> throw NotImplementedError()
        }
    }

    inner class Achieve : BaseListenerImpl<MemberHonorChangeEvent.Achieve>(template) {
        override val eventClass: KClass<MemberHonorChangeEvent.Achieve>
            get() = MemberHonorChangeEvent.Achieve::class

        override fun addTemplateImpl(event: MemberHonorChangeEvent.Achieve) {
            this@MemberHonorChangeEventListener.addTemplateImpl(event)
        }
    }

    inner class Lose : BaseListenerImpl<MemberHonorChangeEvent.Lose>(template) {
        override val eventClass: KClass<MemberHonorChangeEvent.Lose>
            get() = MemberHonorChangeEvent.Lose::class

        override fun addTemplateImpl(event: MemberHonorChangeEvent.Lose) {
            this@MemberHonorChangeEventListener.addTemplateImpl(event)
        }
    }
}

private class MemberJoinEventListener(template: MutableMap<String, Any>) :
    GroupMemberInfoEventsListenerImpl<MemberJoinEvent>, BaseListenerImpl<MemberJoinEvent>(template),
    EventSubclassImpl<MemberJoinEvent> {
    override val eventClass: KClass<MemberJoinEvent>
        get() = MemberJoinEvent::class

    override fun addTemplateImpl(event: MemberJoinEvent) {
        super.addBaseBotTemplate(event, template)
    }

    override val subclassList: List<ListenerImpl<out MemberJoinEvent>>
        get() = listOf(Invite(), Active(), Retrieve())

    override fun findSubclass(name: String): ListenerImpl<out MemberJoinEvent> {
        return when (name) {
            "Invite" -> subclassList[0]
            "Active" -> subclassList[1]
            "Retrieve" -> subclassList[2]
            else -> throw NotImplementedError()
        }
    }

    inner class Retrieve : BaseListenerImpl<MemberJoinEvent.Retrieve>(template) {
        override val eventClass: KClass<MemberJoinEvent.Retrieve>
            get() = MemberJoinEvent.Retrieve::class

        override fun addTemplateImpl(event: MemberJoinEvent.Retrieve) {
            this@MemberJoinEventListener.addTemplateImpl(event)
        }
    }

    inner class Active : BaseListenerImpl<MemberJoinEvent.Retrieve>(template) {
        override val eventClass: KClass<MemberJoinEvent.Retrieve>
            get() = MemberJoinEvent.Retrieve::class

        override fun addTemplateImpl(event: MemberJoinEvent.Retrieve) {
            this@MemberJoinEventListener.addTemplateImpl(event)
        }
    }

    inner class Invite : BaseListenerImpl<MemberJoinEvent.Invite>(template) {
        override val eventClass: KClass<MemberJoinEvent.Invite>
            get() = MemberJoinEvent.Invite::class

        override fun addTemplateImpl(event: MemberJoinEvent.Invite) {
            this@MemberJoinEventListener.addTemplateImpl(event)
            template["invitor"] = event.invitor
        }
    }
}

private class MemberLeaveEventListener(template: MutableMap<String, Any>) :
    GroupMemberInfoEventsListenerImpl<MemberLeaveEvent>, BaseListenerImpl<MemberLeaveEvent>(template),
    EventSubclassImpl<MemberLeaveEvent> {
    override val eventClass: KClass<MemberLeaveEvent>
        get() = MemberLeaveEvent::class

    override fun addTemplateImpl(event: MemberLeaveEvent) {
        super.addBaseBotTemplate(event, template)
    }

    override val subclassList: List<ListenerImpl<out MemberLeaveEvent>>
        get() = listOf(Quit(), Kick())

    override fun findSubclass(name: String): ListenerImpl<out MemberLeaveEvent> {
        return when (name) {
            "Quit" -> subclassList[0]
            "Kick" -> subclassList[1]
            else -> throw NotImplementedError()
        }
    }

    inner class Kick : BaseListenerImpl<MemberLeaveEvent.Kick>(template) {
        override val eventClass: KClass<MemberLeaveEvent.Kick>
            get() = MemberLeaveEvent.Kick::class

        override fun addTemplateImpl(event: MemberLeaveEvent.Kick) {
            this@MemberLeaveEventListener.addTemplateImpl(event)
            template["operator"] = event.operatorOrBot
            template["isByBot"] = event.isByBot
        }
    }

    inner class Quit : BaseListenerImpl<MemberLeaveEvent.Quit>(template) {
        override val eventClass: KClass<MemberLeaveEvent.Quit>
            get() = MemberLeaveEvent.Quit::class

        override fun addTemplateImpl(event: MemberLeaveEvent.Quit) {
            this@MemberLeaveEventListener.addTemplateImpl(event)
        }
    }
}