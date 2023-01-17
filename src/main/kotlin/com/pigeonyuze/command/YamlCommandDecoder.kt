package com.pigeonyuze.command

import com.pigeonyuze.CommandConfigs
import com.pigeonyuze.CommandPolymorphism
import com.pigeonyuze.YamlBot
import com.pigeonyuze.YamlBot.reload
import com.pigeonyuze.command.Command.*
import com.pigeonyuze.command.Command.ArgCommand.Type.*
import com.pigeonyuze.command.Condition.JudgmentMethod.*
import com.pigeonyuze.command.YamlCommandDecoder.load
import com.pigeonyuze.template.asParameter
import com.pigeonyuze.toPolymorphismObject
import kotlinx.serialization.modules.plus
import net.mamoe.mirai.console.data.PluginData
import net.mamoe.mirai.console.data.PluginDataHolder
import net.mamoe.mirai.console.data.PluginDataStorage
import net.mamoe.mirai.console.plugin.jvm.AbstractJvmPlugin
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.message.MessageSerializers
import net.mamoe.yamlkt.*
import java.io.File
import java.util.*

@OptIn(ConsoleExperimentalApi::class)
/**
 * 避免多 [NormalCommand] 无法序列化或者 [YamlElement] 无法反序列化的问题
 *
 * 在需要调用[CommandConfigs] 的 [reload] 时，应优先考虑[load]
 * */
object YamlCommandDecoder : PluginDataStorage {
    const val saveName = "CommandReg"

    private const val normalCommandName = "Command"
    private const val onlyRunCommandName = "OnlyRunCommand"
    private const val argsCommandName = "ArgCommand"


    //region Decoder
    private fun readToCommand(commandElement: YamlMap): NormalCommand? {
        // name: kotlin.collections.List<kotlin.String>
        // answeringMethod: com.pigeonyuze.command.AnsweringMethod
        // answerContent: kotlin.String
        // run: kotlin.collections.List<com.pigeonyuze.command.TemplateYML>
        // condition: kotlin.collections.List<com.pigeonyuze.command.Condition>
        val nameYaml = commandElement["name"] as? YamlList ?: return null
        val answeringMethodYaml = commandElement["answeringMethod"] as? YamlLiteral ?: return null
        val answerContentYaml = commandElement["answerContent"] as? YamlLiteral ?: return null
        val runYaml = commandElement["run"] as? YamlList ?: return null
        val conditionYaml = commandElement["condition"] as? YamlList ?: return null

        val name = mutableListOf<String>()
        for (oneNameElement in nameYaml) {
            if (oneNameElement !is YamlLiteral) name.add(oneNameElement.toString())
            else name.add(oneNameElement.content)
        }

        val answeringMethod = AnsweringMethod.valueOf(answeringMethodYaml.content)
        val answerContent = answerContentYaml.content
        val run = mutableListOf<TemplateYML>()
        getRun(runYaml, run, normalCommandName)
        val condition = mutableListOf<Condition>()
        getCondition(conditionYaml, condition, normalCommandName)

        return NormalCommand(name, answeringMethod, answerContent, run, condition)
    }

    // try first!!
    // if you do not first call this function,then maybe a ArgCommand will be normal Command
    private fun readToArgsCommand(commandElement: YamlMap): ArgCommand? {
        // name: kotlin.collections.List<kotlin.String>
        // answeringMethod: com.pigeonyuze.command.AnsweringMethod
        // answerContent: kotlin.String
        // run: kotlin.collections.List<com.pigeonyuze.command.TemplateYML>
        // condition: kotlin.collections.List<com.pigeonyuze.command.Condition>
        // argsSplit: kotlin.Char
        // useLaterAddParams: kotlin.Boolean
        // laterAddParamsTimeoutSecond: kotlin.Int
        // argsSize: kotlin.Int
        // request: kotlin.collections.Map<kotlin.Int, com.pigeonyuze.command.ArgCommand.Type>?
        // describe: kotlin.collections.Map<kotlin.Int, kotlin.String>?
        val nameYaml = commandElement["name"] as? YamlList ?: return null
        val answeringMethodYaml = commandElement["answeringMethod"] as? YamlLiteral ?: return null
        val answerContentYaml = commandElement["answerContent"] as? YamlLiteral ?: return null
        val runYaml = commandElement["run"] as? YamlList ?: return null
        val conditionYaml = commandElement["condition"] as? YamlList ?: return null
        val argsSizeYaml = commandElement["argsSize"] as? YamlLiteral ?: return null

        // default value (if not have ,then use default value)
        val laterAddParamsTimeoutSecond =
            (commandElement["laterAddParamsTimeoutSecond"] as? YamlLiteral)?.toIntOrNull() ?: 60
        val useLaterAddParams = (commandElement["useLaterAddParams"] as? YamlLiteral)?.toBoolean() ?: true
        val argsSplit = (commandElement["argsSplit"] as? YamlLiteral)?.toIntOrNull()?.toChar() ?: ' '
        val requestYaml = commandElement["request"] as? YamlMap
        val describeYaml = commandElement["describe"] as? YamlMap

        val name = mutableListOf<String>()
        for (oneNameElement in nameYaml) {
            if (oneNameElement !is YamlLiteral) name.add(oneNameElement.toString())
            else name.add(oneNameElement.content)
        }
        val answeringMethod = AnsweringMethod.valueOf(answeringMethodYaml.content)
        val answerContent = answerContentYaml.content
        val run = mutableListOf<TemplateYML>()
        getRun(runYaml, run, argsCommandName)
        val condition = mutableListOf<Condition>()
        getCondition(conditionYaml, condition, argsCommandName)
        val argsSize = argsSizeYaml.toIntOrNull() ?: errorYamlType("argsSize", "int", "argsSize", argsCommandName)

        val request = if (requestYaml != null) mutableMapOf<Int, ArgCommand.Type>() else null

        if (requestYaml != null) for ((key, value) in requestYaml) {
            if (key !is YamlLiteral || value !is YamlLiteral) continue
            val index = key.toIntOrNull() ?: errorYamlType("index", "int", "request", argsCommandName)
            val type = when (value.content) {
                "double" -> DOUBLE
                "int" -> INT
                "string" -> STRING
                "long" -> LONG
                "boolean" -> BOOLEAN
                "forwardMessage" -> FORWARD_MESSAGE
                "flashImage" -> FLASH_IMAGE
                "image" -> IMAGE
                "pokeMessage" -> POKE_MESSAGE
                "audio" -> AUDIO
                "at" -> AT
                "musicShare" -> MUSIC_SHARE
                "xmlJsonMessage" -> XML_JSON_MESSAGE
                else -> errorYamlType(
                    "type",
                    "strings('double','int','string','long','boolean','forwardMessage','flashImage','Image','pokeMessage','audio','at','musicShare' or 'xmlJsonMessage')",
                    "request",
                    argsCommandName
                )
            }
            request?.set(index, type)
        }

        val describe = if (describeYaml == null) null else mutableMapOf<Int, String>()
        if (describeYaml != null) for ((key, value) in describeYaml) {
            if (key !is YamlLiteral || value !is YamlLiteral) continue
            val index = key.toIntOrNull() ?: errorYamlType("index", "int", "describe", argsCommandName)
            val type = value.content
            describe?.set(index, type)
        }
        return ArgCommand(
            name,
            answeringMethod,
            answerContent,
            run,
            condition,
            argsSplit,
            useLaterAddParams,
            laterAddParamsTimeoutSecond,
            argsSize,
            request,
            describe
        )
    }

    private fun readToOnlyCommand(commandElement: YamlMap): OnlyRunCommand? {
        // name: kotlin.collections.List<kotlin.String>
        // run: kotlin.collections.List<com.pigeonyuze.command.TemplateYML>
        // condition: kotlin.collections.List<com.pigeonyuze.command.Condition>
        val nameYaml = commandElement["name"] as? YamlList ?: return null
        val runYaml = commandElement["run"] as? YamlList ?: return null
        val conditionYaml = commandElement["condition"] as? YamlList ?: return null

        val name = mutableListOf<String>()
        for (oneNameElement in nameYaml) {
            if (oneNameElement !is YamlLiteral) name.add(oneNameElement.toString())
            else name.add(oneNameElement.content)
        }

        val run = mutableListOf<TemplateYML>()
        getRun(runYaml, run, onlyRunCommandName)
        val condition = mutableListOf<Condition>()
        getCondition(conditionYaml, condition, onlyRunCommandName)
        return OnlyRunCommand(name, run, condition)
    }


    private fun getRun(
        runYaml: YamlList,
        run: MutableList<TemplateYML>,
        initObj: String,
    ) {
        for (oneRunElement in runYaml) {
            if (oneRunElement !is YamlMap) continue

            run.add(yamlElementToTemplateYML(oneRunElement, "run", initObj))
        }
    }

    private fun getCondition(
        conditionYaml: YamlList,
        condition: MutableList<Condition>,
        initObj: String,
    ) {
        for (oneConditionElement in conditionYaml) {
            if (oneConditionElement !is YamlMap) continue
            val call = yamlElementToTemplateYML(oneConditionElement["call"] as YamlMap, "condition", initObj)
            val requestYaml = oneConditionElement["request"] as? YamlLiteral ?: errorYamlType(
                "request",
                "strings",
                "condition",
                initObj
            )
            val request = when (requestYaml.content) {
                "if true" -> IF_TRUE
                "if false" -> IF_FALSE
                "else if true" -> ELSE_IF_TRUE
                "else if false" -> ELSE_IF_FALSE
                "else" -> ELSE
                else -> NONE //other or "none"
            }
            condition.add(Condition(request, call))
        }
    }

    private fun yamlElementToTemplateYML(oneRunElement: YamlMap, callFrom: String, initObj: String): TemplateYML {
        val useYaml = oneRunElement["use"] as? YamlLiteral ?: errorYamlType("use", "strings", callFrom, initObj)
        val runNameYaml = oneRunElement["name"] as? YamlLiteral ?: errorYamlType(
            "name",
            "number, booleans and strings",
            callFrom,
            initObj
        )
        val callYaml =
            oneRunElement["call"] as? YamlLiteral ?: errorYamlType("call", "booleans and strings", callFrom, initObj)
        val argsYaml = oneRunElement["args"] as? YamlList ?: errorYamlType("args", "list", callFrom, initObj)

        val use = ImportType.valueOf(useYaml.content)
        val runName = runNameYaml.content
        val call = callYaml.content
        val args = argsYaml.asParameter()
        return TemplateYML(use, call, args, runName)
    }

    private fun errorYamlType(name: String, shouldBe: String, from: String, initObj: String): Nothing =
        illegalArgument("An error occurred while initializing the construction parameter '$from' of '$initObj' because: '$name' is malformed, it should be '$shouldBe'")
    //endregion
    /**
     *
     * */
    fun CommandConfigs.load() {
        load(YamlBot as AbstractJvmPlugin, this)
    }

    @ConsoleExperimentalApi
    override fun load(holder: PluginDataHolder, instance: PluginData) {
        if (instance != CommandConfigs) error("This class is only for CommandConfigs!")
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

        val commands = (yaml as YamlMap)["COMMAND"] as YamlList
        val commandList = mutableListOf<CommandPolymorphism>()
        for (commandYaml in commands) { //循环尝试反序列化为一个Command类
            if (commandYaml !is YamlMap) continue
            val command: Command =
                readToArgsCommand(commandYaml) ?: readToCommand(commandYaml) ?: readToOnlyCommand(commandYaml)
                ?: illegalArgument("Cannot initialize object to a command!")
            commandList.add(command.toPolymorphismObject())
        }
        CommandConfigs.COMMAND = commandList
    }

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

    @ConsoleExperimentalApi
    override fun store(holder: PluginDataHolder, instance: PluginData) {
        if (instance != CommandConfigs) error("This class is only for CommandConfigs!")
        getFile().writeText(
            Yaml {
                this.serializersModule =
                    MessageSerializers.serializersModule + instance.serializersModule
            }.encodeToString(instance.updaterSerializer, Unit).run { //删除多余的信息
                val joiner = StringJoiner("\n")
                for (line in this.lines()) {
                    if (line == "  - value: ") {
                        joiner.add("  -") //create list
                        continue
                    }
                    if (line.endsWith("value: ")) continue
                    if (line.startsWith("#")) continue //comment
                    if (line.startsWith("      type: com.pigeonyuze.command.Command")) continue
                    joiner.add(
                        line.replaceFirst("    ", "").replace("char:  ", "char:' '")
                    ) //https://github.com/Him188/yamlkt/issues/53/
                }
                joiner.toString()
            }
        )

    }
}