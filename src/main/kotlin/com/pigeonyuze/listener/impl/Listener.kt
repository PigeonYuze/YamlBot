package com.pigeonyuze.listener.impl

import com.pigeonyuze.command.element.ImportType
import com.pigeonyuze.listener.EventListener
import com.pigeonyuze.listener.EventParentScopeType
import com.pigeonyuze.listener.impl.ListenerImpl.Companion.addTemplate
import com.pigeonyuze.listener.impl.data.*
import com.pigeonyuze.util.SerializerData
import com.pigeonyuze.util.mapCast
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.coroutines.*
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent

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
            val yamlEventListener = this@execute
            val listenerObject = coroutineScope {
                val jobs: MutableList<Job> = mutableListOf()
                var listenerObject: ListenerImpl<out Event>? = null
                for (it in listeners) {
                    jobs.add(launch {
                        it.searchBuildListenerOrNull(name, template)?.also {obj: ListenerImpl<out Event> ->
                            listenerObject = obj
                            cancel()
                        }
                    })

                    // 当达到最大协程数时，等待所有子任务完成后统一处理结果
                    while (jobs.size >= 4) {
                        // 挂起当前协程，直到有任务完成才继续执行
                        yield()
                        jobs.removeAll { it.isCompleted }
                    }
                }
                jobs.joinAll()

                return@coroutineScope listenerObject
                    ?: throw NotImplementedError("Cannot find $name,This may be because it does not exist or the parameter is wrong")
            }

            val eventChannel = GlobalEventChannel.parentScope(EventParentScopeType.parseEventScope(parentScope))
            if (!this@execute.readSubclassObjectName.contains("all")) {
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
                        executeRun(
                            listenerObject,
                            if (this@startListener.provideEventAllValue) addTemplate(
                                it,
                                eventListener.template
                            ) else mutableMapOf(),
                            it
                        )
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
                        executeRun(
                            listenerObject,
                            if (this@startListener.provideEventAllValue) addTemplate(
                                it,
                                eventListener.template
                            ) else mutableMapOf(),
                            it
                        )
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
                    executeRun(
                        listenerObject,
                        if (this@startListener.provideEventAllValue) addTemplate(
                            it,
                            eventListener.template
                        ) else mutableMapOf(),
                        it
                    )
                }
            )
        }

        private suspend inline fun EventListener.executeRun(
            listenerObject: ListenerImpl<out Event>,
            values: MutableMap<String, Any>,
            event: Event,
        ) {
            for (templateYML in run) {
                if (templateYML.use == ImportType.EVENT) {
                    val template = listenerObject.eventTemplate.findOrNull(templateYML.call)
                        ?: throw IllegalArgumentException("Cannot find function ${templateYML.call}")
                    val value = template.execute(templateYML.parameter)
                    values[templateYML.name] = value
                    continue
                }
                val args = templateYML.parameter.parseElement(values.mapCast())
                val templateObj = templateYML.use.getProjectClass()
                val serializerDataOrNull =
                    templateObj::class.annotations.filterIsInstance<SerializerData>().firstOrNull()
                if (serializerDataOrNull != null && event is MessageEvent) args.setValueByCommand(
                    serializerDataOrNull, event
                )

                values[templateYML.name] = templateObj.call(
                    templateYML.call, args
                )
            }
        }
    }
}