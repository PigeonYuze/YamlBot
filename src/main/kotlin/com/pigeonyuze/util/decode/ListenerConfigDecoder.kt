package com.pigeonyuze.util.decode

import com.pigeonyuze.util.setting.ListenerConfigs
import com.pigeonyuze.listener.EventListener
import com.pigeonyuze.util.decode.CommandConfigDecoder.checked
import com.pigeonyuze.util.decode.CommandConfigDecoder.decodeToTemplateYaml
import com.sksamuel.hoplite.*
import com.sksamuel.hoplite.fp.valid
import net.mamoe.mirai.event.EventPriority

/**
 * 针对 [EventListener] 的解析器
 * @see ConfigDecoder
 * */
internal object ListenerConfigDecoder : ConfigDecoder<EventListener>() {

    //////////////////////////
    // Override functions  ///
    //////////////////////////
    override val instanceObj: String by lazy { "EventListener" }

    override fun handle(objects: ArrayList<EventListener>) {
        ListenerConfigs.listener = objects
    }

    override fun parseImpl(mapNode: MapNode): EventListener {
        return impl(mapNode)
    }

    //////////////////////////
    // Impl functions      ///
    //////////////////////////
    private fun impl(mapNode: MapNode): EventListener =
        parseBuilder("EventListener",mapNode) {
            val type0 = get<StringNode>(typeField)
            val objectBotId0 = get<LongNode>(objectBotField,true)
            val filter0 = get<StringNode>(filterField,true)
            val provideEventValue0 = get<BooleanNode>(provideValueField,true)
            val priority0 = get<StringNode>(priorityField,true)
            val readSubclassObjectNames0 = get<ArrayNode>(subclassObjectField)
            val runningScope0 = get<StringNode>(scopeField,true)
            val isListenOnce0 = get<BooleanNode>(isRunningOnceField,true)
            val run0 = get<ArrayNode>(runField,true)

            val run = run0(listOf()) { value ->
                value.elements.map { it.decodeToTemplateYaml().checked(value.pos) }
            }
            val priority = priority0(EventPriority.NORMAL.valid()) { it.decodeToEnum(EventPriority.values()) }
            val readSubclassObjectNames = readSubclassObjectNames0
                .map { value ->
                    value.elements
                        .map { it.checkType<StringNode>("<$readSubclassObjectNames0-elements>") }
                        .map { it.invoke().value }
                }

            checkError()
            return@parseBuilder EventListener(
                type = type0.invoke().value,
                objectBotId = objectBotId0(0L) { it.value },
                filter = filter0("true") { it.value },
                provideEventAllValue = provideEventValue0(true) { it.value },
                priority = priority.invoke(),
                readSubclassObjectName = readSubclassObjectNames.invoke(),
                parentScope = runningScope0.invoke("PLUGIN_SCOPE") { it.value },
                isListenOnce = isListenOnce0(true) { it.value },
                run = run
            )
        }


    //////////////////////////
    // Field Names          //
    //////////////////////////
    private const val typeField = "type"
    private const val objectBotField = "objectBotId"
    private const val filterField = "filter"
    private const val provideValueField = "provideEventAllValue"
    private const val priorityField = "priority"
    private const val subclassObjectField = "readSubclassObjectName"
    private const val scopeField = "parentScope"
    private const val isRunningOnceField = "isListenOnce"
    private const val runField = "run"

}