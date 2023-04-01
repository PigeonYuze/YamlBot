@file:JvmName("GroupSettingEventsImplKt")

package com.pigeonyuze.listener.impl.data

import com.pigeonyuze.LoggerManager
import com.pigeonyuze.listener.impl.BaseListenerImpl
import com.pigeonyuze.listener.impl.Listener
import com.pigeonyuze.listener.impl.ListenerImpl
import net.mamoe.mirai.event.AbstractEvent
import net.mamoe.mirai.event.events.*
import kotlin.reflect.KClass

/*
群设置改变: GroupSettingChangeEvent
|- 群名改变: GroupNameChangeEvent
|- 入群公告改变: GroupEntranceAnnouncementChangeEvent
|- 全员禁言状态改变: GroupMuteAllEvent
|- 匿名聊天状态改变: GroupAllowAnonymousChatEvent
|- 允许群员邀请好友加群状态改变: GroupAllowMemberInviteEvent
* */

internal interface GroupSettingChangeEventListenerImpl<K, ValueType : Any> :
    BotEventListenerImpl<K> where K : GroupSettingChangeEvent<ValueType>, K : AbstractEvent {

    override fun addBaseBotTemplate(event: K, template: MutableMap<String, Any>) {
        super.addBaseBotTemplate(event, template)
        template["group"] = event.group
        template["shouldBroadcast"] = event.shouldBroadcast
        template["origin"] = event.origin
        template["new"] = event.new
        template["groupId"] = event.group.id
    }

    companion object EventListener : Listener {
        override fun searchBuildListenerOrNull(name: String, template: MutableMap<String, Any>): ListenerImpl<*>? {
            return when (name) {
                "GroupNameChangeEvent" -> GroupNameChangeEventListener(template)
                "GroupEntranceAnnouncementChangeEvent" -> GroupEntranceAnnouncementChangeEventListener(template)
                "GroupMuteAllEvent" -> GroupMuteAllEventListener(template)
                "GroupAllowConfessTalkEvent" -> GroupAllowConfessTalkEventListener(template)
                "GroupAllowAnonymousChatEvent" -> GroupAllowAnonymousChatEventListener(template)
                "GroupAllowMemberInviteEvent" -> GroupAllowMemberInviteEventListener(template)
                else -> null
            }
        }
    }
}

private class GroupNameChangeEventListener(template: MutableMap<String, Any>) :
    GroupSettingChangeEventListenerImpl<GroupNameChangeEvent, String>,
    BaseListenerImpl<GroupNameChangeEvent>(template) {
    override val eventClass: KClass<GroupNameChangeEvent>
        get() = GroupNameChangeEvent::class

    override fun addTemplateImpl(event: GroupNameChangeEvent) {
        super.addBaseBotTemplate(event, template)
        template["operator"] = event.operator ?: event.bot
    }
}

@Suppress("DEPRECATION_ERROR")
// @DeprecatedSinceMirai(warningSince = "2.12")
private class GroupEntranceAnnouncementChangeEventListener(template: MutableMap<String, Any>) :
    GroupSettingChangeEventListenerImpl<GroupEntranceAnnouncementChangeEvent, String>,
    BaseListenerImpl<GroupEntranceAnnouncementChangeEvent>(template) {
    override val eventClass: KClass<GroupEntranceAnnouncementChangeEvent>
        get() {
            LoggerManager.loggingWarn(
                "GroupEntranceAnnouncementChangeEvent-class-getter",
                "This event is not being triggered anymore."
            )
            return GroupEntranceAnnouncementChangeEvent::class
        }

    override fun addTemplateImpl(event: GroupEntranceAnnouncementChangeEvent) {
        // This function is never called because the event is not being triggered anymore.
        super.addBaseBotTemplate(event, template)
    }
}

private class GroupMuteAllEventListener(template: MutableMap<String, Any>) :
    GroupSettingChangeEventListenerImpl<GroupMuteAllEvent, Boolean>, BaseListenerImpl<GroupMuteAllEvent>(template) {
    override val eventClass: KClass<GroupMuteAllEvent>
        get() = GroupMuteAllEvent::class

    override fun addTemplateImpl(event: GroupMuteAllEvent) {
        super.addBaseBotTemplate(event, template)
        template["muting"] = event.new
        template["operator"] = event.operator ?: event.bot
    }
}

private class GroupAllowAnonymousChatEventListener(template: MutableMap<String, Any>) :
    GroupSettingChangeEventListenerImpl<GroupAllowAnonymousChatEvent, Boolean>,
    BaseListenerImpl<GroupAllowAnonymousChatEvent>(template) {
    override val eventClass: KClass<GroupAllowAnonymousChatEvent>
        get() = GroupAllowAnonymousChatEvent::class

    override fun addTemplateImpl(event: GroupAllowAnonymousChatEvent) {
        super.addBaseBotTemplate(event, template)
        template["operator"] = event.operator ?: event.bot
    }
}

private class GroupAllowConfessTalkEventListener(template: MutableMap<String, Any>) :
    GroupSettingChangeEventListenerImpl<GroupAllowConfessTalkEvent, Boolean>,
    BaseListenerImpl<GroupAllowConfessTalkEvent>(template) {
    override val eventClass: KClass<GroupAllowConfessTalkEvent>
        get() = GroupAllowConfessTalkEvent::class

    override fun addTemplateImpl(event: GroupAllowConfessTalkEvent) {
        super.addBaseBotTemplate(event, template)
        template["isByBot"] = event.isByBot
    }
}

private class GroupAllowMemberInviteEventListener(template: MutableMap<String, Any>) :
    GroupSettingChangeEventListenerImpl<GroupAllowMemberInviteEvent, Boolean>,
    BaseListenerImpl<GroupAllowMemberInviteEvent>(template) {
    override val eventClass: KClass<GroupAllowMemberInviteEvent>
        get() = GroupAllowMemberInviteEvent::class

    override fun addTemplateImpl(event: GroupAllowMemberInviteEvent) {
        super.addBaseBotTemplate(event, template)
        template["operator"] = event.operator ?: event.bot
    }
}