package com.pigeonyuze.listener

import com.pigeonyuze.command.element.TemplateYML
import net.mamoe.mirai.event.EventPriority

/**
 * 一个事件监听器对象
 *
 * 在`Yaml`中表达的映射：
 * ```yaml

type: "Name"
object_bot_id: 0 # default
filter: "%what% == %mustBe% && true || false" # default: false
callCommand: "commandName" # default null
$provide_event_all_value: true # default true
priority: 'NORMAL' # default NORMAL
read_subclass: # default [all]
- all # 读取所有的子类
parentScope: 'USE_EITHER_JOB$Name$' # PLUGIN_JOB | NEW_JOB_FROM_PLUGIN | USE_EITHER_JOB$Name$
run: [

] # like a command
```
 *
 * */
@kotlinx.serialization.Serializable
data class EventListener(
    val type: String,
    val objectBotId: Long = 0L,
    val filter: String = "true",
    val provideEventAllValue: Boolean = true,
    val priority: EventPriority,
    val readSubclassObjectName: List<String> = listOf("all"),
    val parentScope: String = "PLUGIN_JOB",
    val isListenOnce: Boolean = false,
    val run: List<TemplateYML> = listOf(),
) {

    fun botIdToFilter(): String {
        if (objectBotId == 0L) return filter
        return if (filter == "true") "%call-botid% == $objectBotId"
        else "$filter && %call-botid% == $objectBotId"
    }

    @kotlinx.serialization.Transient
    val template = mutableMapOf<String, Any>()
}