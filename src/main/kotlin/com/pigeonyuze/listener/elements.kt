@file:JvmName("EventListenerElementsKt_")

package com.pigeonyuze.listener

import com.pigeonyuze.YamlBot
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.job
import kotlin.coroutines.CoroutineContext

private val scopeList = hashSetOf<CoroutineContext>()

sealed interface EventParentScopeType : CoroutineScope {

    object PluginScope : EventParentScopeType {
        override val coroutineContext: CoroutineContext
            get() = YamlBot.coroutineContext
    }

    class NewScopeFromPlugin : EventParentScopeType {
        override val coroutineContext: CoroutineContext
            get() {
                val context = YamlBot.coroutineContext.job + CoroutineName("EventListenerNewContext#${scopeList.size}")
                scopeList.add(context)
                return context
            }

        override fun equals(other: Any?): Boolean {
            return this === other
        }

        override fun hashCode(): Int {
            return System.identityHashCode(this)
        }
    }

    class UseEitherScope(val name: String) : EventParentScopeType {
        override val coroutineContext: CoroutineContext
            get() {
                return scopeList.firstOrNull {
                    it[CoroutineName]?.name == this.name
                }.let {
                    if (it == null) {
                        val context = NewScopeFromPlugin().coroutineContext
                        scopeList.add(context)
                        context
                    } else it
                }
            }
    }

    companion object {
        fun parseEventScope(setting: String) =
            when (setting) {
                "PLUGIN_SCOPE" -> PluginScope
                "NEW_SCOPE_FROM_PLUGIN" -> NewScopeFromPlugin()
                else -> if (setting.startsWith("USE_SCOPE")) {
                    val name = setting.drop(10).dropLast(1)
                    UseEitherScope(name)
                } else error("Cannot build scope type: $setting")
            }
    }

}