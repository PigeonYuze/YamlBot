package com.pigeonyuze.listener.impl

import com.pigeonyuze.listener.EventParentScopeType
import com.pigeonyuze.listener.YamlEventListener
import net.mamoe.mirai.event.GlobalEventChannel

interface Listener {

    fun searchBuildListenerOrNull(name: String, template: MutableMap<String, Any>): ListenerImpl<*>?

    companion object {
        private val listeners = listOf(
            BotEventListenerImpl.BotListener
        )

        fun YamlEventListener.execute(name: String) {
            var listenerObject: ListenerImpl<*>? = null
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
                    run = {},
                    priority = this.priority
                )
            }
            if (objectBotId != 0L) {
                listenerObject!!.filterExecute(
                    eventChannel = eventChannel,
                    filter = botIdToFilter(),
                    run = {},
                    priority = this.priority
                )
            }
            TODO()
        }
    }
}