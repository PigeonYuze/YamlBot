package com.pigeonyuze.util.decode

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
import com.sksamuel.hoplite.fp.valid
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
        } else {
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
    private fun normalCommandOrOnlyRunCommand(mapping: MapNode): Command =
        parseBuilder("Command & OnlyRunCommand", mapping) {
            var failedRun = false
            var isOnlyRun = false

            val name0 = get<ArrayNode>(nameFieldName)
            val run0 = get<ArrayNode>(runFieldName, true)
                .onFailure { failedRun = true }
            val condition0 = get<ArrayNode>(conditionFieldName, true)
            // For normalCommand, these are unfounded,will parse to OnlyRunData
            val answeringMethod0 = get<StringNode>(answeringMethodFieldName, true)
                .onFailure { isOnlyRun = true }
            val answerContent = get<StringNode>(answerContentField, true)
                .onFailure { isOnlyRun = true }
            if (isOnlyRun xor failedRun) {
                throw parsingError.addCause(
                    ErrorTrace.CannotParseAsAnyone(
                        mapping.pos,
                        "Unable to determine the type. Missing fields"
                    )
                )
            }
            val answeringMethod =
                answeringMethod0(AnsweringMethod.SEND_MESSAGE.valid()) { it.decodeToEnum(AnsweringMethod.values()) }

            checkError()

            val run = run0.invoke(listOf()) { array ->
                array.elements.map { it.decodeToTemplateYaml() }
            }
            val condition = condition0(listOf()) { array ->
                array.elements.map { it.decodeToCondition() }
            }
            val name = name0.invoke().toStringList(nameFieldName)
            return@parseBuilder if (isOnlyRun) OnlyRunCommand(
                name, run, condition
            ) else NormalCommand(
                name, answeringMethod.invoke(), answerContent.invoke { it.value }, run, condition
            )
        }

    /**
     * Parsed as an ArgCommand object
     * @return It will return parsed object. If node **missing** `argSize`, it will return `null`
     * */
    private fun argCommand(mapping: MapNode): ArgCommand? =
        parseBuilder("ArgCommand", mapping) {
            val argSize = mapping[argSizeFieldName].checkType<LongNode>(argSizeFieldName).onFailureInline {
                return@parseBuilder null
            }.getUnsafe().value.toInt()

            val name0 = get<ArrayNode>(nameFieldName)
            val answeringMethod0 = get<StringNode>(answeringMethodFieldName)
            val answerContent0 = get<StringNode>(answerContentField)
            /* Default value */
            val condition0 = get<ArrayNode>(conditionFieldName, true)
            val run0 = get<ArrayNode>(runFieldName, true)
            val isPrefixForAll0 = get<BooleanNode>(isPrefixForAllFieldName, true)
            val argsSplit0 = get<StringNode>(argsSplitFieldName, true)
            val useLaterAddParams0 = get<BooleanNode>(useLaterAddParamsField, true)
            val laterAddParamsTimeoutSecond0 = get<LongNode>(timeoutSecondFieldName, true)
            val request0 = get<MapNode>(requestFieldName, true)
            val describe0 = get<MapNode>(describeFieldName, true)

            checkError()
            val name = name0.unsafeByThrow().toStringList(nameFieldName)
            val answeringMethod =
                answeringMethod0.unsafeByThrow().decodeToEnum(AnsweringMethod.values()).unsafeByThrow()
            return@parseBuilder ArgCommand(
                name = name,
                answeringMethod = answeringMethod,
                answerContent = answerContent0.unsafeByThrow().value,
                run = run0(listOf()) { elements ->
                    elements.elements.map { it.decodeToTemplateYaml() }
                },
                condition = condition0(listOf()) { elements ->
                    elements.elements.map { it.decodeToCondition() }
                },
                argsSplit = argsSplit0(" ") { it.value },
                useLaterAddParams = useLaterAddParams0(true) { it.value },
                laterAddParamsTimeoutSecond = laterAddParamsTimeoutSecond0(60) { it.value.toInt() },
                argsSize = argSize,
                request = request0(mapOf()) { node ->
                    implToArgCommandRequest(node, parsingError)
                },
                describe = describe0(mapOf()) { node ->
                    node.map
                        .mapKeys { it.key.toInt() }
                        .mapValues { it.value.checkType<StringNode>("<$describeFieldName-elements>").invoke().value }
                },
                isPrefixForAll = isPrefixForAll0(true) { it.value }
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
    ) = node.map
        .mapKeys { it.key.toInt() }
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

    private fun Node.decodeToTemplateYaml(): TemplateYML =
        parseBuilder(
            templateCallerFieldName,
            checkType<MapNode>("<$runFieldName-element>").unsafeByThrow()
        ) {
            val use0 = get<StringNode>("use")
            val name0 = get<StringNode>("name")
            val args0 = get<ArrayNode>("args")
            val call0 = get<StringNode>("call")
            val name = use0 { it.decodeToEnum(ImportType.values()) }
            checkError()
            return@parseBuilder TemplateYML(
                name.invoke(),
                name0.invoke().value,
                args0.invoke().toStringList("<TemplateCaller-args>"),
                call0.invoke().value
            )
        }

    private fun Node.decodeToCondition(): Condition =
        parseBuilder(conditionObjectFieldName, checkType<MapNode>("<condition-elements>").getUnsafe()) {
            val request0 = get<StringNode>("request")
            val call0 = get<MapNode>("call")
            checkError()
            val request =
                when (request0.invoke().value) {
                    "if true", "if t", "true" -> Condition.JudgmentMethod.IF_TRUE
                    "if false", "if f", "false" -> Condition.JudgmentMethod.IF_FALSE
                    "else if", "elif", "if" -> Condition.JudgmentMethod.ELSE_IF_TRUE
                    "!else if", "!elif", "!if" -> Condition.JudgmentMethod.ELSE_IF_FALSE
                    "else", "el" -> Condition.JudgmentMethod.ELSE
                    else -> Condition.JudgmentMethod.NONE
                }
            return@parseBuilder Condition(
                request,
                call0.invoke().decodeToTemplateYaml().checked(pos)
            )
        }

    private inline fun <E, A> Validated<E, A>.onFailureInline(f: (E) -> Unit): Validated<E, A> {
        when (this) {
            is Validated.Valid -> Unit
            is Validated.Invalid -> f(this.error)
        }
        return this
    }

    internal fun TemplateYML.checked(
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