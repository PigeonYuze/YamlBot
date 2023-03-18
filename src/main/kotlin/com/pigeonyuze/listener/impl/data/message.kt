@file:OptIn(MiraiInternalApi::class)

package com.pigeonyuze.listener.impl.data

import com.pigeonyuze.listener.impl.BaseListenerImpl
import com.pigeonyuze.listener.impl.Listener
import com.pigeonyuze.listener.impl.ListenerImpl
import net.mamoe.mirai.contact.Platform
import net.mamoe.mirai.event.AbstractEvent
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.utils.MiraiInternalApi
import java.util.*
import kotlin.reflect.KClass

/* message events (from: https://docs.mirai.mamoe.net/EventList.html)
被动收到消息：MessageEvent
|- 群消息：GroupMessageEvent
|- 好友消息：FriendMessageEvent
|- 群临时会话消息：GroupTempMessageEvent
|- 陌生人消息：StrangerMessageEvent
|- 其他客户端消息：OtherClientMessageEvent
主动发送消息前: MessagePreSendEvent
|- 群消息: GroupMessagePreSendEvent
|- 好友消息: FriendMessagePreSendEvent
|- 群临时会话消息: GroupTempMessagePreSendEvent
|- 陌生人消息：StrangerMessagePreSendEvent
|- 其他客户端消息：OtherClientMessagePreSendEvent
主动发送消息后: MessagePostSendEvent
|- 群消息: GroupMessagePostSendEvent
|- 好友消息: FriendMessagePostSendEvent
|- 群临时会话消息: GroupTempMessagePostSendEvent
|- 陌生人消息：StrangerMessagePostSendEvent
|- 其他客户端消息：OtherClientMessagePostSendEvent
消息撤回: MessageRecallEvent
|- 好友撤回: FriendRecall
|- 群撤回: GroupRecall
|- 群临时会话撤回: TempRecall
图片上传前: BeforeImageUploadEvent
图片上传完成: ImageUploadEvent
|- 图片上传成功: Succeed
|- 图片上传失败: Failed
* */

//-----------------------------------------------------------------------
// The bot passively receives the message
internal interface MessageEventListenerImpl<K> : BotEventListenerImpl<K> where K : AbstractEvent, K : MessageEvent {
    override fun addBaseBotTemplate(event: K, template: MutableMap<String, Any>) {
        super.addBaseBotTemplate(event, template)
        template["message"] = event.message
        template["subject"] = event.subject
        template["subjectId"] = event.subject.id
        template["senderId"] = event.sender.id
        template["sender"] = event.sender
        template["senderName"] = event.senderName
        template["time"] = event.time
        template["date"] = Date(event.time * 100L)
        template["source"] = event.source
    }

    companion object MessageListener : Listener {
        override fun searchBuildListenerOrNull(name: String, template: MutableMap<String, Any>): ListenerImpl<*>? {
            return when (name) {
                "OtherClientMessageEvent" -> OtherClientMessageEventListenerImpl(template)
                "StrangerMessageEvent" -> StrangerMessageEventListenerImpl(template)
                "GroupTempMessageEvent" -> GroupTempMessageEventListenerImpl(template)
                "FriendMessageEvent" -> FriendMessageEventListenerImpl(template)
                "GroupMessageEvent" -> GroupMessageEventListenerImpl(template)
                else -> null
            }
        }
    }
}

private class GroupMessageEventListenerImpl(template: MutableMap<String, Any>) :
    MessageEventListenerImpl<GroupMessageEvent>,
    BaseListenerImpl<GroupMessageEvent>(template) {

    override val eventClass: KClass<GroupMessageEvent>
        get() = GroupMessageEvent::class

    override fun addTemplateImpl(event: GroupMessageEvent) {
        super.addBaseBotTemplate(event, template)
        template["permission"] = event.permission
        template["group"] = event.group
    }
}

private class FriendMessageEventListenerImpl(template: MutableMap<String, Any>) :
    MessageEventListenerImpl<FriendMessageEvent>,
    BaseListenerImpl<FriendMessageEvent>(template) {

    override val eventClass: KClass<FriendMessageEvent>
        get() = FriendMessageEvent::class

    override fun addTemplateImpl(event: FriendMessageEvent) {
        super.addBaseBotTemplate(event, template)
        template["friend"] = event.friend
        template["user"] = event.user
        template["friendGroup"] = event.friend.friendGroup
    }
}

private class GroupTempMessageEventListenerImpl(template: MutableMap<String, Any>) :
    MessageEventListenerImpl<GroupTempMessageEvent>,
    BaseListenerImpl<GroupTempMessageEvent>(template) {

    override val eventClass: KClass<GroupTempMessageEvent>
        get() = GroupTempMessageEvent::class

    override fun addTemplateImpl(event: GroupTempMessageEvent) {
        super.addBaseBotTemplate(event, template)
        template["group"] = event.group
    }
}

private class StrangerMessageEventListenerImpl(template: MutableMap<String, Any>) :
    MessageEventListenerImpl<StrangerMessageEvent>,
    BaseListenerImpl<StrangerMessageEvent>(template) {

    override val eventClass: KClass<StrangerMessageEvent>
        get() = StrangerMessageEvent::class

    override fun addTemplateImpl(event: StrangerMessageEvent) {
        super.addBaseBotTemplate(event, template)
        template["stranger"] = event.stranger
        template["user"] = event.user
    }
}

private class OtherClientMessageEventListenerImpl(template: MutableMap<String, Any>) :
    MessageEventListenerImpl<OtherClientMessageEvent>,
    BaseListenerImpl<OtherClientMessageEvent>(template) {

    override val eventClass: KClass<OtherClientMessageEvent>
        get() = OtherClientMessageEvent::class

    override fun addTemplateImpl(event: OtherClientMessageEvent) {
        super.addBaseBotTemplate(event, template)
        template["client"] = event.client
        template["clientInfo"] = event.client.info
        template["clientAppId"] = event.client.info.appId
        template["deviceKind"] = event.client.info.deviceKind
        template["deviceName"] = event.client.info.deviceName
        template["platform"] = event.client.info.platform ?: Platform.MOBILE
    }
}



