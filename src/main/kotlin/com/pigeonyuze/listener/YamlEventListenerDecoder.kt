package com.pigeonyuze.listener

import com.pigeonyuze.ListenerConfigs
import com.pigeonyuze.YamlBot
import com.pigeonyuze.command.element.ImportType
import com.pigeonyuze.command.element.TemplateYML
import com.pigeonyuze.command.element.illegalArgument
import com.pigeonyuze.template.asParameter
import net.mamoe.mirai.console.data.PluginData
import net.mamoe.mirai.console.data.PluginDataHolder
import net.mamoe.mirai.console.data.PluginDataStorage
import net.mamoe.mirai.console.plugin.jvm.AbstractJvmPlugin
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.event.EventPriority
import net.mamoe.yamlkt.*
import java.io.File


@OptIn(ConsoleExperimentalApi::class)
object YamlEventListenerDecoder : PluginDataStorage {
    const val saveName = "EventListener"

    @Suppress("SameParameterValue")
    fun readToYamlEventListener(yamlMap: YamlMap): EventListener {
        // type: kotlin.String,
        // objectBotId: kotlin.Long,
        // filter: kotlin.String,
        // provideEventAllValue: kotlin.Boolean,
        // priority: net.mamoe.mirai.event.EventPriority,
        // readSubclassObjectName: kotlin.collections.List<kotlin.String>,
        // parentScope: kotlin.String,
        // isListenOnce: kotlin.Boolean,
        // run: kotlin.collections.List<com.pigeonyuze.command.element.TemplateYML>
        val type = (yamlMap["type"] as? YamlLiteral)?.content
            ?: throw ExceptionInInitializerError("Cannot read to EventListener,because can't find `type` field.\ncontext: $yamlMap")
        val objectBotId = (yamlMap["objectBotId"] as? YamlLiteral)?.toLongOrNull() ?: 0L
        val filter = (yamlMap["filter"] as? YamlLiteral)?.content ?: "true"
        val provideEventAllValue = (yamlMap["provideEventAllValue"] as? YamlLiteral)?.toBoolean() ?: true
        val priorityString = (yamlMap["priority"] as? YamlLiteral)?.content ?: "NORMAL"
        val priority = EventPriority.valueOf(priorityString)
        val readSubclassObjectNameYaml = (yamlMap["readSubclassObjectName"] as? YamlList) ?: listOf("All")
        val readSubclassObjectName = if (readSubclassObjectNameYaml is YamlList) {
            val tempList = mutableListOf<String>()
            for (element in readSubclassObjectNameYaml) {
                tempList.add(element.content.toString())
            }
            tempList
        } else listOf("All")
        val parentScope = (yamlMap["parentScope"] as? YamlLiteral)?.content ?: "PLUGIN_JOB"
        val isListenOnce = (yamlMap["isListenOnce"] as? YamlLiteral)?.toBoolean() ?: false
        val runYaml = yamlMap["run"] as? YamlList ?: listOf()
        val run = mutableListOf<TemplateYML>()
        for (element in runYaml) {
            run.add(yamlElementToTemplateYML(element as YamlMap))
        }

        return EventListener(
            type = type,
            objectBotId = objectBotId,
            filter = filter,
            provideEventAllValue = provideEventAllValue,
            priority = priority,
            readSubclassObjectName = readSubclassObjectName,
            parentScope = parentScope,
            isListenOnce = isListenOnce,
            run = run
        )
    }

    private fun getSerializedString(eventListeners: List<EventListener>): String {
        return Yaml {
            mapSerialization = YamlBuilder.MapSerialization.BLOCK_MAP
            listSerialization = YamlBuilder.ListSerialization.BLOCK_SEQUENCE
        }.encodeToString(eventListeners)
    }

    private fun yamlElementToTemplateYML(oneRunElement: YamlMap): TemplateYML {
        val callFrom = "readToYamlEventListener"
        val initObj = "YamlEventListener"
        val useYaml = oneRunElement["use"] as? YamlLiteral ?: errorYamlType(
            "use",
            "strings",
            callFrom,
            initObj
        )
        val runNameYaml = oneRunElement["name"] as? YamlLiteral ?: errorYamlType(
            "name",
            "number, booleans and strings",
            callFrom,
            initObj
        )
        val callYaml =
            oneRunElement["call"] as? YamlLiteral ?: errorYamlType(
                "call",
                "booleans and strings",
                callFrom,
                initObj
            )
        val argsYaml = oneRunElement["args"] as? YamlList ?: errorYamlType(
            "args",
            "list",
            callFrom,
            initObj
        )

        val use = ImportType.valueOf(useYaml.content)
        val runName = runNameYaml.content
        val call = callYaml.content
        val args = argsYaml.asParameter()
        return TemplateYML(use, call, args, runName)
    }

    @Suppress("SameParameterValue")
    private fun errorYamlType(name: String, shouldBe: String, from: String, initObj: String): Nothing =
        illegalArgument("An error occurred while initializing the construction parameter '$from' of '$initObj' because: '$name' is malformed, it should be '$shouldBe'")

    private fun getFile(): File {
        val name = this.saveName
        val dir = YamlBot.configFolderPath.toFile()
        if (dir.isFile) {
            error("Target directory $dir for holder $YamlBot is occupied by a file therefore data ${YamlBot::class.qualifiedName ?: "<anonymous class>"} can't be saved.")
        }
        dir.mkdir()

        val file = dir.resolve("$name.yml")
        if (file.isDirectory) {
            error("Target File $file is occupied by a directory therefore data ${YamlBot::class.qualifiedName ?: "<anonymous class>"} can't be saved.")
        }
        return file.also {
            it.parentFile?.mkdirs()
            it.createNewFile()
        }
    }

    override fun load(holder: PluginDataHolder, instance: PluginData) {
        if (instance !is ListenerConfigs) error("This is only for ListenerConfigs class !")
        instance.onInit(holder, this)

        val file = getFile()
        val text = file.readText().removePrefix("\uFEFF")

        if (text.isBlank()) {
            this.store(holder, instance) //生成文件
            return
        }
        val yaml = try {
            Yaml.decodeYamlFromString(text)
        } catch (e: Exception) {
            file.copyTo(file.resolveSibling("${file.name}.${System.currentTimeMillis()}.bak"))
            throw e
        }
        val yamlList = yaml as YamlList
        val resultList = mutableListOf<EventListener>()
        for (element in yamlList) {
            resultList.add(readToYamlEventListener(element as YamlMap))
        }

        instance.listener = resultList
    }

    override fun store(holder: PluginDataHolder, instance: PluginData) {
        if (instance !is ListenerConfigs) error("This is only for ListenerConfigs class !")
        getFile().writeText(
            getSerializedString(instance.listener)
        )
    }

    fun ListenerConfigs.load() {
        load(YamlBot as AbstractJvmPlugin, this)
    }
}