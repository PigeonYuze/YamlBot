package com.pigeonyuze.listener.impl

import net.mamoe.mirai.event.Event

internal interface EventSubclassImpl<SuperEvent : Event> {
    val subclassList: List<ListenerImpl<out SuperEvent>>

    fun findSubclass(name: String): ListenerImpl<out SuperEvent>
}