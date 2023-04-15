package com.pigeonyuze.listener.impl.template

import net.mamoe.mirai.event.Event

/**
 * 事件支持的所有的模板
 *
 * @see EventTemplate
 * @see EventTemplateBuilder
 * */
@JvmInline
value class EventTemplateValues<K : Event>(val value: List<EventTemplate<K, out Any>>) {
    /**
     * 查找名称符合的 [EventTemplate] 当找不到时返回`null`
     * */
    fun findOrNull(name: String): EventTemplate<K, out Any>? {
        for (element in value) {
            if (element.name != name) {
                continue
            }
            return element
        }
        return null
    }
}
