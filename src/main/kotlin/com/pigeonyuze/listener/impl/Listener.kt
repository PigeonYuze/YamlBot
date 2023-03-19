package com.pigeonyuze.listener.impl

import com.pigeonyuze.listener.EventParentScopeType
import com.pigeonyuze.listener.YamlEventListener
import com.pigeonyuze.listener.impl.ListenerImpl.Companion.addTemplate
import com.pigeonyuze.listener.impl.data.BotEventListenerImpl
import com.pigeonyuze.listener.impl.data.MessageEventListenerImpl
import com.pigeonyuze.listener.impl.data.MessagePostSendEventListenerImpl
import com.pigeonyuze.listener.impl.data.MessagePreSendEventListenerImpl
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.GlobalEventChannel

interface Listener {

    fun searchBuildListenerOrNull(name: String, template: MutableMap<String, Any>): ListenerImpl<*>?

    companion object {
        private val listeners = listOf(
            BotEventListenerImpl.BotListener,
            MessageEventListenerImpl.MessageListener,
            MessagePreSendEventListenerImpl.MessagePreSendEventListener,
            MessagePostSendEventListenerImpl.MessagePostSendEventListener
        )

        fun YamlEventListener.execute(name: String) {
            val yamlEventListener = this
            var listenerObject: ListenerImpl<out Event>? = null
            listeners.forEach {
                it.searchBuildListenerOrNull(name, this.template)?.also { obj ->
                    listenerObject = obj
                }
            }

            listenerObject
                ?: throw NotImplementedError("Cannot find $name,This may be because it does not exist or the parameter is wrong")
            val eventChannel = GlobalEventChannel.parentScope(EventParentScopeType.parseEventScope(this.parentScope))
            if (isListenOnce) {
                listenerObject!!.onceExecute(
                    eventChannel = eventChannel,
                    filter = botIdToFilter(),
                    run = {
                        addTemplate(it, yamlEventListener.template)
                    },
                    priority = this.priority
                )
                return
            }
            if (objectBotId != 0L) {
                listenerObject!!.filterExecute(
                    eventChannel = eventChannel,
                    filter = botIdToFilter(),
                    run = {
                        addTemplate(it, yamlEventListener.template)
                    },
                    priority = this.priority
                )
                return
            }
            listenerObject!!.execute(
                eventChannel = eventChannel,
                priority = this.priority,
                run = {
                    addTemplate(it, yamlEventListener.template)
                }
            )
        }
    }
}