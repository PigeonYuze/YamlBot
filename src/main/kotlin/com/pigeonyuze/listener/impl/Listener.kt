package com.pigeonyuze.listener.impl

import com.pigeonyuze.command.element.ImportType
import com.pigeonyuze.listener.EventListener
import com.pigeonyuze.listener.EventParentScopeType
import com.pigeonyuze.listener.impl.ListenerImpl.Companion.addTemplate
import com.pigeonyuze.listener.impl.data.*
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.GlobalEventChannel

interface Listener {

    fun searchBuildListenerOrNull(name: String, template: MutableMap<String, Any>): ListenerImpl<*>?

    companion object {
        private val listeners = listOf(
            BotEventListenerImpl.BotListener,
            MessageEventListenerImpl.MessageListener,
            MessagePreSendEventListenerImpl.MessagePreSendEventListener,
            MessagePostSendEventListenerImpl.MessagePostSendEventListener,
            MessageRecallEventListenerImpl.MessageRecallListener,
            BeforeImageUploadEventListenerImpl.EventListener,
            ImageUploadEventListenerImpl.EventListener,
            GroupSettingChangeEventListenerImpl.EventListener,
            GroupMemberInfoEventsListenerImpl.EventListener,
            GroupEventsAboutBotListenerImpl.EventListener,
            FriendEventsListenerImpl.EventListener
        )

        suspend fun EventListener.execute(name: String = this.type) {
            val yamlEventListener = this
            var listenerObject: ListenerImpl<out Event>? = null
            for (it in listeners) {
                val obj: ListenerImpl<out Event>? = it.searchBuildListenerOrNull(name, this.template)
                if (obj != null) {
                    listenerObject = obj
                    break
                }
            }

            listenerObject
                ?: throw NotImplementedError("Cannot find $name,This may be because it does not exist or the parameter is wrong")

            val eventChannel = GlobalEventChannel.parentScope(EventParentScopeType.parseEventScope(this.parentScope))
            if (!this.readSubclassObjectName.contains("all")) {
                if (listenerObject !is EventSubclassImpl<*>) {
                    startListener(listenerObject, eventChannel, yamlEventListener)
                    return
                }
                for (subclass in readSubclassObjectName) {
                    listenerObject.findSubclass(subclass)
                    startListener(listenerObject, eventChannel, yamlEventListener)
                }
            }
            startListener(listenerObject, eventChannel, yamlEventListener)
        }

        private suspend fun EventListener.startListener(
            listenerObject: ListenerImpl<out Event>,
            eventChannel: EventChannel<Event>,
            eventListener: EventListener,
        ) {
            if (isListenOnce) {
                listenerObject.onceExecute(
                    eventChannel = eventChannel,
                    filter = botIdToFilter(),
                    run = {
                        if (this@startListener.provideEventAllValue) addTemplate(it, eventListener.template)
                        executeRun(listenerObject, eventListener)
                    },
                    priority = this.priority
                )
                return
            }
            if (objectBotId != 0L) {
                listenerObject.filterExecute(
                    eventChannel = eventChannel,
                    filter = botIdToFilter(),
                    run = {
                        if (this@startListener.provideEventAllValue) addTemplate(it, eventListener.template)
                        executeRun(listenerObject, eventListener)
                    },
                    priority = this.priority
                )
                return
            }
            listenerObject.execute(
                eventChannel = eventChannel,
                priority = this.priority,
                run = {
                    if (this@startListener.provideEventAllValue) addTemplate(it, eventListener.template)
                    executeRun(listenerObject, eventListener)
                }
            )
        }

        private suspend inline fun EventListener.executeRun(
            listenerObject: ListenerImpl<out Event>,
            eventListener: EventListener,
        ) {
            for (templateYML in run) {
                if (templateYML.use == ImportType.EVENT) {
                    val template = listenerObject.eventTemplate.findOrNull(templateYML.name)
                        ?: throw IllegalArgumentException("Cannot find function ${templateYML.name}")
                    val value = template.execute(templateYML.parameter)
                    eventListener.template[templateYML.name] = value
                }
            }
        }
    }
}