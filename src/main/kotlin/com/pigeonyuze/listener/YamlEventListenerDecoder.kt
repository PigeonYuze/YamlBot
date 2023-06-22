package com.pigeonyuze.listener

import com.pigeonyuze.ListenerConfigs
import com.pigeonyuze.YamlBot
import net.mamoe.mirai.console.data.PluginData
import net.mamoe.mirai.console.data.PluginDataHolder
import net.mamoe.mirai.console.data.PluginDataStorage
import net.mamoe.mirai.console.plugin.jvm.AbstractJvmPlugin
import net.mamoe.mirai.console.util.ConsoleExperimentalApi


@OptIn(ConsoleExperimentalApi::class)
object YamlEventListenerDecoder : PluginDataStorage {
    const val saveName = "EventListener"


    override fun load(holder: PluginDataHolder, instance: PluginData) {
        if (instance !is ListenerConfigs) error("This is only for ListenerConfigs class !")
        TODO()// unfinished.
    }

    override fun store(holder: PluginDataHolder, instance: PluginData) {
        if (instance !is ListenerConfigs) error("This is only for ListenerConfigs class !")
        TODO() // unfinished.
    }

    fun ListenerConfigs.load() {
        load(YamlBot as AbstractJvmPlugin, this)
    }
}