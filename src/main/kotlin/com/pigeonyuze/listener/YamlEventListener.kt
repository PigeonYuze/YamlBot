package com.pigeonyuze.listener

import net.mamoe.mirai.event.EventPriority


class YamlEventListener(
    val type: String,
    val objectBotId: Long = 0L,
    val filter: String = "true",
    val provideEventAllValue: Boolean = true,
    val priority: EventPriority,
    val readSubclassObjectName: List<String> = listOf("all"),
    val parentScope: String = "PLUGIN_JOB",
    val isListenOnce: Boolean = false,
) {

    fun botIdToFilter(): String {
        if (objectBotId == 0L) return filter
        return if (filter == "true") "%call-botid% == $objectBotId"
        else "$filter && %call-botid% == $objectBotId"
    }

    val template = mutableMapOf<String, Any>()

    fun runEvent() {
        TODO()
    }
}