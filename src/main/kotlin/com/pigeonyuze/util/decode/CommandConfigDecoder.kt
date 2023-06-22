package com.pigeonyuze.util.decode

import com.pigeonyuze.LoggerManager
import com.pigeonyuze.command.Command
import com.pigeonyuze.command.Command.*
import com.pigeonyuze.command.element.AnsweringMethod
import com.pigeonyuze.command.element.Condition
import com.pigeonyuze.command.element.ImportType
import com.pigeonyuze.command.element.TemplateYML
import com.pigeonyuze.isDebugging0
import com.pigeonyuze.util.SerializerData
import com.pigeonyuze.util.logger.BeautifulError
import com.pigeonyuze.util.logger.ErrorAbout
import com.pigeonyuze.util.logger.ErrorTrace
import com.pigeonyuze.util.logger.ErrorType
import com.sksamuel.hoplite.*
import com.sksamuel.hoplite.fp.Validated
import com.sksamuel.hoplite.fp.invalid
import kotlinx.coroutines.launch

/**
 * 针对 [Command] 的解析器
 *
 * @see ConfigDecoder
 * */
internal object CommandConfigDecoder : ConfigDecoder<Command>() {

    //////////////////////////
    // Override functions  ///
    //////////////////////////
    override val instanceObj: String by lazy { "Command" }

    override fun handle(objects: ArrayList<Command>) {
        if (isDebugging0) {
            println(objects)
        }else {
            Command.commands = objects
        }
    }

    override fun parseImpl(mapNode: MapNode): Command {
        return argCommand(mapNode) ?: normalCommandOrOnlyRunCommand(mapNode)
    }


    //////////////////////////
    // Impl functions      ///
    //////////////////////////

    //priority:
    //  argCommand -> normalCommand -> onlyRunCommand
    // If node contains 'argSize', it will be parsed as ArgCommand.
    // If node contains 'answeringMethod' and 'answerContent', it will be parsed as NormalCommand.
    // Else, it should as OnlyRunCommand or throw an error.

    /**
     * Parsed as a NormalCommand or an OnlyRunCommand object
     *
     * @return
     *  It will return parsed object.
     *
     * - If node **missing** `answeringMethod` and `answerContent`, it will return [OnlyRunCommand]
     *
     * - If `answeringMethod` and `answerContent` **consist in** node, it will return [NormalCommand]
     *
     * - Else, it will throw [BeautifulError]
     * */
    private fun normalCommandOrOnlyRunCommand(mapping: MapNode): Command {
        val commandCallerErr =
            BeautifulError(
                errorType = ErrorType.RUNNING_FUNCTION,
                errorAbout = ErrorAbout.ByInstance("NormalCommand & OnlyRunCommand")
            )
        var failedRun = false
        var isOnlyRun = false
        val run = mapping[runFieldName].checkType<ArrayNode>(runFieldName, mapping)
            .checkCauseMissing(commandCallerErr) {
                byDefaultLogger(it, runFieldName, templateCallerFieldName)
                failedRun = true
            }
            .transformOrDefault(defaultValue = listOf(), transform = { array ->
                array.elements.map { it.decodeToTemplateYaml() }
            })
        val condition = mapping[conditionFieldName].checkType<ArrayNode>(conditionFieldName, mapping)
            .checkCauseMissing(commandCallerErr) { byDefaultLogger(it, conditionFieldName, conditionObjectFieldName)  }
            .transformOrDefault(defaultValue = listOf(), transform = { array ->
                array.elements.map { it.decodeToCondition() }
            })
        val name0 = mapping[nameFieldName].checkType<ArrayNode>(nameFieldName, mapping) byThrow commandCallerErr
        // For normalCommand, if it cannot find,will parse to OnlyRunData
        val answeringMethod0 =
            mapping[answeringMethodFieldName].checkType<StringNode>(answeringMethodFieldName, mapping)
                .checkCauseMissing(commandCallerErr) {
                    LoggerManager.loggingWarn(
                        "Parse-NormalCommand",
                        "Missing parameter: '$answeringMethodFieldName', Automatically parse by OnlyRunCommand"
                    )
                    isOnlyRun = true
                }
                .transformOrDefault(defaultValue = AnsweringMethod.SEND_MESSAGE.invalid(), transform = {
                it.decodeToEnum(AnsweringMethod.values()) byThrow commandCallerErr
            })
        val answerContent =
            mapping[answerContentField].checkType<StringNode>(answerContentField, mapping)
                .checkCauseMissing(commandCallerErr) {
                    LoggerManager.loggingWarn(
                        "Parse-NormalCommand",
                        "Missing parameter: '$answerContentField', Automatically parse by OnlyRunCommand"
                    )
                    isOnlyRun = true
                }
                .transformOrDefault(defaultValue = "", transform = {
                // Decoding to OnlyRunCommand
                it.value
            })
        if (isOnlyRun xor failedRun) {
            commandCallerErr.addCause(
                ErrorTrace.MissingParameter(mapping.pos, runFieldName),
            )
        }

        commandCallerErr.isEmpty() || throw commandCallerErr
        val name = name0.unsafeByThrow().elements.map {
            it.checkType<StringNode>("<name-element>").unsafeByThrow().value
        }
        return if (isOnlyRun) {
            OnlyRunCommand(
                name = name,
                run = run,
                condition = condition
            )
        } else NormalCommand(
            name = name,
            run = run,
            condition = condition,
            answerContent = answerContent,
            answeringMethod = answeringMethod0.getUnsafe()
        )
    }

    /**
     * Parsed as an ArgCommand object
     * @return It will return parsed object. If node **missing** `argSize`, it will return `null`
     * */
    private fun argCommand(mapping: MapNode): ArgCommand? {
        val commandCallerErr: BeautifulError by lazy {
            BeautifulError(
                errorType = ErrorType.RUNNING_FUNCTION,
                errorAbout = ErrorAbout.ByInstance("ArgCommand")
            )
        }
        val argSize = mapping[argSizeFieldName].checkType<LongNode>(argSizeFieldName).onFailureInline {
            return null
        }.getUnsafe().value.toInt()
        val name0 = mapping[nameFieldName].checkType<ArrayNode>(nameFieldName,mapping) byThrow commandCallerErr
        val answeringMethod0 =
            mapping[answeringMethodFieldName].checkType<StringNode>(answeringMethodFieldName,mapping) byThrow commandCallerErr
        val answerContent0 =
            mapping[answerContentField].checkType<StringNode>(answerContentField,mapping) byThrow commandCallerErr
        /* Default value */
        val condition = mapping[conditionFieldName].checkType<ArrayNode>(conditionFieldName,mapping)
            .checkCauseMissing(commandCallerErr) { byDefaultLogger(it, conditionFieldName, conditionObjectFieldName) }
            .transformOrDefault(defaultValue = listOf(), transform = { array ->
                array.elements.map { it.decodeToCondition() }
            })
        val run = mapping[runFieldName].checkType<ArrayNode>(runFieldName,mapping)
            .checkCauseMissing(commandCallerErr) { byDefaultLogger(it, runFieldName, templateCallerFieldName) }
            .transformOrDefault(defaultValue = listOf(), transform = { array ->
                array.elements.map { it.decodeToTemplateYaml() }
            })
        val isPrefixForAll = mapping[isPrefixForAllFieldName].checkType<BooleanNode>(isPrefixForAllFieldName,mapping)
            .transformOrDefault(defaultValue = true, transform = { it.value })
        val argsSplit = mapping[argsSplitFieldName].checkType<StringNode>(argsSplitFieldName)
            .transformOrDefault(transform = { it.value }, " ")
        val useLaterAddParams = mapping[useLaterAddParamsField].checkType<BooleanNode>(useLaterAddParamsField,mapping)
            .transformOrDefault(transform = { it.value }, true)
        val laterAddParamsTimeoutSecond = mapping[timeoutSecondFieldName].checkType<LongNode>(timeoutSecondFieldName,mapping)
            .transformOrDefault(transform = { it.value.toInt() }, 60)
        val request = mapping[requestFieldName].checkType<MapNode>(requestFieldName,mapping)
            .transformOrDefault(transform = { node ->
                implToArgCommandRequest(node, commandCallerErr)
            }, defaultValue = mapOf())
        val describe = mapping[describeFieldName].checkType<MapNode>(describeFieldName,mapping)
            .transformOrDefault(transform = { node ->
                node.map
                    .mapKeys { it.key.toInt() }
                    .mapValues { it.value.checkType<StringNode>("<$describeFieldName-elements>").unsafeByThrow().value }
            }, mapOf())
        commandCallerErr.isEmpty() || throw commandCallerErr
        val name = name0.unsafeByThrow().toStringList(nameFieldName)
        val answeringMethod = answeringMethod0.unsafeByThrow().decodeToEnum(AnsweringMethod.values()).unsafeByThrow()
        return ArgCommand(
            name = name,
            answeringMethod = answeringMethod,
            answerContent = answerContent0.unsafeByThrow().value,
            run = run,
            condition = condition,
            argsSplit = argsSplit,
            useLaterAddParams = useLaterAddParams,
            laterAddParamsTimeoutSecond = laterAddParamsTimeoutSecond,
            argsSize = argSize,
            request = request,
            describe = describe,
            isPrefixForAll = isPrefixForAll
        )
    }


    //////////////////////////
    // Field Names          //
    //////////////////////////
    private const val runFieldName = "run"
    private const val nameFieldName = "name"
    private const val answeringMethodFieldName = "answeringMethod"
    private const val templateCallerFieldName = "TemplateCaller"
    private const val answerContentField = "answerContent"
    private const val conditionFieldName = "condition"
    private const val conditionObjectFieldName = "Condition"
    private const val argSizeFieldName = "argsSize"
    private const val isPrefixForAllFieldName = "isPrefixForAll"
    private const val argsSplitFieldName = "argsSplit"
    private const val useLaterAddParamsField = "useLaterAddParams"
    private const val timeoutSecondFieldName = "laterAddParamsTimeoutSecond"
    private const val requestFieldName = "request"
    private const val describeFieldName = "describe"

    //////////////////////////
    // Tools                //
    //////////////////////////
    /**
     * @suppress The data may have wrong data. Please check your [commandCallerErr]!
     * */
    private fun implToArgCommandRequest(
        node: MapNode,
        commandCallerErr: BeautifulError,
    ) = node.map.mapKeys { it.key.toInt() }
        .mapValues {
            when (val value = it.value.checkType<StringNode>("<$requestFieldName.elements>").unsafeByThrow().value) {
                "double" -> ArgCommand.Type.DOUBLE
                "int" -> ArgCommand.Type.INT
                "string" -> ArgCommand.Type.STRING
                "long" -> ArgCommand.Type.LONG
                "boolean" -> ArgCommand.Type.BOOLEAN
                "forwardMessage" -> ArgCommand.Type.FORWARD_MESSAGE
                "flashImage" -> ArgCommand.Type.FLASH_IMAGE
                "image" -> ArgCommand.Type.IMAGE
                "pokeMessage" -> ArgCommand.Type.POKE_MESSAGE
                "audio" -> ArgCommand.Type.AUDIO
                "at" -> ArgCommand.Type.AT
                "musicShare" -> ArgCommand.Type.MUSIC_SHARE
                "xmlJsonMessage" -> ArgCommand.Type.XML_JSON_MESSAGE
                else -> {
                    commandCallerErr.addCause(
                        ErrorTrace.UnknownParameter(
                            node.pos, value,
                            listOf(
                                "double", "int", "string", "long", "boolean", "forwardMessage",
                                "flashImage", "Image", "pokeMessage", "audio", "at", "musicShare", "xmlJsonMessage"
                            )
                        )
                    )
                    ArgCommand.Type.NOTHING
                }
            }
        }

    private fun Node.decodeToTemplateYaml(): TemplateYML {
        val templateCallerErr =
            BeautifulError(
                errorType = ErrorType.RUNNING_FUNCTION,
                errorAbout = ErrorAbout.ByInstance("TemplateCaller")
            )
        return checkType<MapNode>("<$runFieldName-elements>")
            .runOrThrow { templateYML0 ->
                val use0 = templateYML0["use"].checkType<StringNode>("<run-elements-use>").runOrThrow {
                    it.decodeToEnum(ImportType.values())
                } byThrow templateCallerErr
                val name0 = templateYML0[nameFieldName].checkType<StringNode>(
                    "<$runFieldName-elements-$nameFieldName>"
                ) byThrow templateCallerErr
                val args0 =
                    templateYML0["args"].checkType<ArrayNode>("<$runFieldName-elements-args>") byThrow templateCallerErr
                val call0 = templateYML0["call"].checkType<StringNode>(
                    "<$runFieldName-elements-call>"
                ) byThrow templateCallerErr
                templateCallerErr.isEmpty() || throw templateCallerErr
                val use = use0.getUnsafe()
                val name = name0.getUnsafe().value
                val args = args0.getUnsafe().toStringList("<TemplateCaller-args>")
                val call = call0.getUnsafe().value
                return@runOrThrow TemplateYML(
                    use = use,
                    name = name,
                    args = args,
                    call = call
                ).checked(pos)
            }
    }

    private fun Node.decodeToCondition(): Condition =
        checkType<MapNode>("<condition-elements>").runOrThrow { conditionObject0 ->
            val request0 = conditionObject0["request"].checkType<StringNode>("<condition-elements-request>")
                .unsafeByThrow()
            val request =
                when (request0.value) {
                    "if true", "if t", "true" -> Condition.JudgmentMethod.IF_TRUE
                    "if false", "if f", "false" -> Condition.JudgmentMethod.IF_FALSE
                    "else if", "elif", "if" -> Condition.JudgmentMethod.ELSE_IF_TRUE
                    "!else if", "!elif", "!if" -> Condition.JudgmentMethod.ELSE_IF_FALSE
                    "else", "el" -> Condition.JudgmentMethod.ELSE
                    else -> Condition.JudgmentMethod.NONE
                }
            Condition(
                request,
                conditionObject0["call"].decodeToTemplateYaml().checked(pos)
            )
        }

    private inline fun <E, A> Validated<E, A>.onFailureInline(f: (E) -> Unit): Validated<E, A> {
        when (this) {
            is Validated.Valid -> Unit
            is Validated.Invalid -> f(this.error)
        }
        return this
    }

    private fun TemplateYML.checked(
        pos: Pos,
    ) = apply {
        val caller = this.objectTmp ?: throw BeautifulError(
            errorType = ErrorType.RUNNING_FUNCTION,
            errorAbout = ErrorAbout.ByInstance("TemplateCaller"),
            ErrorTrace.UnknownParameter(pos, call, listOf("any function names of template>"))
        )
        caller.launch {
            try {
                if (caller::class.annotations.filterIsInstance<SerializerData>().isEmpty())
                    caller.execute(parameter) // Don't run function with SerializerData,it may throw error because wrong args.
            } catch (e: Throwable) {
                throw BeautifulError(
                    errorType = ErrorType.RUNNING_FUNCTION,
                    errorAbout = ErrorAbout.ByInstance("TemplateCaller"),
                    ErrorTrace.FunctionThrowError(pos, e)
                ).apply { addSuppressed(e) }
            }
        }
    }

}