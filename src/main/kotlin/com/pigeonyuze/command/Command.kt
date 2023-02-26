package com.pigeonyuze.command

import com.pigeonyuze.com.pigeonyuze.LoggerManager
import com.pigeonyuze.command.element.AnsweringMethod
import com.pigeonyuze.command.element.AnsweringMethod.*
import com.pigeonyuze.command.element.Condition
import com.pigeonyuze.command.element.TemplateYML
import com.pigeonyuze.command.element.illegalArgument
import com.pigeonyuze.template.Parameter
import com.pigeonyuze.template.Parameter.Companion.addAny
import com.pigeonyuze.template.Parameter.Companion.removeFirst
import com.pigeonyuze.template.asParameter
import com.pigeonyuze.util.SerializerData
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.code.CodableMessage
import net.mamoe.mirai.message.code.MiraiCode
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.nextMessageOrNull
import net.mamoe.yamlkt.Comment
import kotlin.reflect.jvm.jvmName

@Serializable
sealed interface Command {
    @Comment("指令的全称 使用者在发出该内容后会触发指令 (使用contentToString比较)")
    val name: List<String>

    @Comment(
        """
        回复的方式 可选为QUOTE,SEND_MESSAGE,AT_SEND
        QUOTE 为回复信息
        SEND_MESSAGE 为直接发送信息
        AT_SEND 为at发送者后发送信息
    """
    )
    val answeringMethod: AnsweringMethod

    @Comment(
        """
        回答的内容
        可提供%call-value%调用参数 (详细可见 readme.md 所标注的方法)
    """
    )
    val answerContent: String

    @Comment(
        """
        会同时执行的操作 如有需要调用参数 需要在此处进行编写
        详细可见 readme.md
    """
    )
    val run: List<TemplateYML>

    @Comment(
        """
        运行还需要的额外条件(该项暂时没有用 因为还没有写好的返回布尔值的内容)
        若无需条件请使用以下内容：
        - request: none
          call: null
          runRequest: false
    """
    )
    val condition: List<Condition>

    val templateCallName: MutableMap<String, Any?>
        get() = mutableMapOf()

    fun isThis(commandMessage: String): Boolean

    suspend fun run(event: MessageEvent) {
       this.runImpl(event, templateCallName)
    }

    companion object {
        private suspend fun Command.runImpl(event: MessageEvent,templateCallName: MutableMap<String, Any?>) {
            LoggerManager.loggingDebug("Command-run", "Start run command.....")
            LoggerManager.loggingTrace("Command-run", "Judgment running condition.")
            judgment(event)
            LoggerManager.loggingTrace("Command-run", "Foreach `run` and call function.")
            for (templateYML in run) {
                LoggerManager.loggingDebug("Command-run", "Call ${templateYML.use}.${templateYML.call}")
                LoggerManager.loggingTrace("Command-run", "Args: ${templateYML.args}")
                val callFunction = callFunction(templateYML, event, templateCallName)
                templateCallName[templateYML.name] = callFunction
                LoggerManager.loggingTrace(
                    "Command-run",
                    "Function ${templateYML.call} return: $callFunction return type: ${callFunction::class.jvmName} "
                )
                LoggerManager.loggingDebug("Command-run", "Call ${templateYML.name} function is finished")
            }
            if (answerContent.isEmpty()) {
                LoggerManager.loggingInfo("Command-run", "The `${name[0]}` command is finished(send message finish).")
                return
            }
            LoggerManager.loggingTrace("Command-run", "Parse (message: String) to (miraiMessage: Message)")
            val answer = parseData(answerContent, templateCallName).toMiraiMessage()
            LoggerManager.loggingDebug("Command-run", answer.serializeToMiraiCode())
            LoggerManager.loggingTrace("Command-run", "Answering method : $answeringMethod")
            when (answeringMethod) {
                QUOTE -> event.subject.sendMessage(event.message.quote() + answer)
                SEND_MESSAGE -> event.subject.sendMessage(answer)
                AT_SEND -> event.subject.sendMessage(At(event.sender) + answer)
            }
            LoggerManager.loggingInfo("Command-run", "The `${name[0]}` command is finished(send message finish).")
        }
        private suspend fun Command.judgment(event: MessageEvent) {
            condition.filterIndexed { index, it ->
                it.invoke(event, condition[if (index == 0) 0 else index - 1], templateCallName)
                it.runRequest
            }
        }

        private fun String.toMiraiMessage() = MiraiCode.deserializeMiraiCode(this)

        suspend fun MessageEvent.quote(context: String) = this.subject.sendMessage(this.message.quote().plus(context))


        fun parseData(messageContent: String, templateCallName: MutableMap<String, Any?>): String {
            LoggerManager.loggingDebug("Command-parseMessage", "Converts the set template to values.")
            var startIndex = 0
            var inCommand = false
            var retString = messageContent
            for ((index, char) in messageContent.withIndex()) {
                if (char == '%' && messageContent.getOrNull(index + 1) == 'c' &&
                    messageContent.getOrNull(index + 2) == 'a' &&
                    messageContent.getOrNull(index + 3) == 'l' &&
                    messageContent.getOrNull(index + 4) == 'l' &&
                    messageContent.getOrNull(index + 5) == '-'
                ) {
                    startIndex = index
                    inCommand = true
                    continue
                }
                if (inCommand && char == '%') {
                    val callName = messageContent.substring((startIndex + 6), index)
                    val value = templateCallName[callName]
                    val replaceRange = retString.replaceFirst(
                        "%call-${callName}%",
                        (if (value is CodableMessage) value.serializeToMiraiCode() else value?.toString())
                            ?: illegalArgument("Cannot find $callName in 'run'!")
                    )
                    LoggerManager.loggingTrace("Command-parseData", "Converts -> `$callName`,replace value!")
                    retString = replaceRange
                    inCommand = false
                    continue
                }
            }
            return retString
        }


        suspend fun callFunction(
            templateYML: TemplateYML,
            event: MessageEvent,
            templateCall: MutableMap<String, Any?>,
        ): Any {
            val template = templateYML.use.getProjectClass().findOrNull(templateYML.call)
                ?: illegalArgument("Cannot find function ${templateYML.call}")
            LoggerManager.loggingTrace(
                "Command-callFunction",
                "Template: ${template::class.jvmName} name: ${template.name}"
            )
            var args = templateYML.parameter.parseElement(templateCall)
            val annotationList = template::class.annotations
            for (annotation in annotationList) {
                if (annotation is SerializerData) {
                    args = args.setValueByCommand(annotation, event)
                }
            }
            return template.execute(args)
        }

        private fun Command._initImpl() {
            val run by lazy { this.run }
            val name by lazy { this.name }
            val templateCallMap = templateCallName
            for (templateYML in run) {
                if (templateCallMap.contains(templateYML.name)) {
                    LoggerManager.loggingError(
                        "Command-run",
                        "${name[0]} CommandSetting IllegalArgument! $templateCallMap"
                    )
                    illegalArgument("${templateYML.name} 重复了(${templateYML.use}.${templateYML.call})")
                }
                templateCallMap[templateYML.name] = null
            }
        }
    }

    /////////////////////////
    ////////Implement////////
    /////////////////////////

    @Serializable
    data class NormalCommand(
        override val name: List<String>,

        override val answeringMethod: AnsweringMethod,

        override val answerContent: String,

        override val run: List<TemplateYML>,

        override val condition: List<Condition> = listOf(),
    ) : Command {
        init {
            _initImpl()
        }
        override fun isThis(commandMessage: String): Boolean {
            return commandMessage in name
        }
    }

    @Serializable
    data class OnlyRunCommand(
        override val name: List<String>,

        override val run: List<TemplateYML>,

        override val condition: List<Condition> = listOf(),
    ) : Command {

        init {
            _initImpl()
        }


        override val answerContent: String
            get() = ""

        override val answeringMethod: AnsweringMethod
            get() = SEND_MESSAGE

        override fun isThis(commandMessage: String): Boolean {
            return commandMessage in name
        }
    }

    @Serializable
    data class ArgCommand @JvmOverloads constructor(
        override val name: List<String>,
        override val answeringMethod: AnsweringMethod,
        override val answerContent: String,
        override val run: List<TemplateYML>,
        override val condition: List<Condition> = listOf(),
        private val argsSplit: String = " ",
        private val useLaterAddParams: Boolean = true,
        private val laterAddParamsTimeoutSecond: Int = 60,
        private val argsSize: Int,
        private val request: Map<Int, Type>? = null,
        private val describe: Map<Int, String>? = null,
        private val isPrefixForAll: Boolean = true,
    ) : Command {

        @Transient
        val templateCallNameImpl: MutableMap<String, Any?> = mutableMapOf()
        @Transient
        override val templateCallName: MutableMap<String, Any?> = templateCallNameImpl

        init {
            _initImpl()
        }

        @Serializable
        enum class Type {
            STRING {
                override fun checkType(context: Message): Boolean {
                    return (context is PlainText) && context.content.isNotEmpty()
                }

                override fun switchType(context: Message): Any {
                    return context.content
                }
            },
            INT {
                override fun checkType(context: Message): Boolean {
                    return (context is PlainText) && context.content.toIntOrNull() != null
                }

                override fun switchType(context: Message): Any {
                    return context.content.toInt()
                }
            },
            LONG {
                override fun checkType(context: Message): Boolean {
                    return (context is PlainText) && context.content.toLongOrNull() != null
                }

                override fun switchType(context: Message): Any {
                    return context.content.toLong()
                }
            },
            DOUBLE {
                override fun checkType(context: Message): Boolean {
                    return (context is PlainText) && context.content.toDoubleOrNull() != null
                }

                override fun switchType(context: Message): Any {
                    return context.content.toDouble()
                }
            },
            BOOLEAN {
                override fun checkType(context: Message): Boolean {
                    return (context is PlainText) && context.content.toBooleanStrictOrNull() != null
                }

                override fun switchType(context: Message): Any {
                    return context.content.toBoolean()
                }
            },
            IMAGE {
                override fun checkType(context: Message): Boolean {
                    return context is Image
                }

                override fun switchType(context: Message): Any {
                    return context as Image
                }
            },
            AT {
                override fun checkType(context: Message): Boolean {
                    return context is At
                }

                override fun switchType(context: Message): Any {
                    return context as At
                }
            },
            FLASH_IMAGE {
                override fun checkType(context: Message): Boolean {
                    return context is FlashImage
                }

                override fun switchType(context: Message): Any {
                    return context as FlashImage
                }
            },
            XML_JSON_MESSAGE {
                override fun checkType(context: Message): Boolean {
                    return context is RichMessage
                }

                override fun switchType(context: Message): Any {
                    return context as RichMessage
                }
            },
            MUSIC_SHARE {
                override fun checkType(context: Message): Boolean {
                    return context is MusicShare
                }

                override fun switchType(context: Message): Any {
                    return context as MusicShare
                }
            },
            POKE_MESSAGE {
                override fun checkType(context: Message): Boolean {
                    return context is PokeMessage
                }

                override fun switchType(context: Message): Any {
                    return context as PokeMessage
                }
            },
            FORWARD_MESSAGE {
                override fun checkType(context: Message): Boolean {
                    return context is ForwardMessage
                }

                override fun switchType(context: Message): Any {
                    return context as ForwardMessage
                }
            },
            AUDIO {
                override fun checkType(context: Message): Boolean {
                    return context is Audio
                }

                override fun switchType(context: Message): Any {
                    return context as Audio
                }
            }

            ;

            abstract fun checkType(context: Message): Boolean
            abstract fun switchType(context: Message): Any
        }

        private fun describeMessage(size: Int) =
            if (describe?.get(size) != null) "描述: ${describe[size]} " + //含有描述
                    (if (request?.get(size) == null) "" else "该项需要 ${request[size]} 格式的信息")
            else if (request?.get(size) != null) "该项需要 ${request[size]} 格式的信息" //默认描述
            else "该项无任何要求" //找不到东西描述

        override suspend fun run(event: MessageEvent) {
            var msg = event.message.contentToString()

            LoggerManager.loggingDebug("ArgCommand-run", "Start get args from native message..")

            for (commandName in name) {
                msg = checkCommandName(msg, commandName) {
                    msg.replace(commandName, "")//get only value command
                } ?: continue
            }

            LoggerManager.loggingTrace("ArgCommand-run", "Pure parameter part: $msg")

            val args = /* Get values from native message*/
                if (argsSplit.isEmpty() && argsSize == 1) {
                    println("IT IS CAN BE NATIVE MSG")
                    msg.asParameter()
                } else msg.split(argsSplit).asParameter().removeFirst()

            LoggerManager.loggingDebug("ArgCommand-run", "Args from native message: $args")

            kotlin.runCatching {
                if (args.size == argsSize) {
                    addTemplateFromArgs(args)
                } else if (args.size < argsSize) {
                    if (!useLaterAddParams) {
                        event.message.quote().plus("您所提供的参数不足 [${args.size} / $argsSize]")
                        LoggerManager.loggingWarn(
                            "Command-run",
                            "The `${name[0]}` command is finished(Args size ${args.size} < $argsSize,it should > $argsSize)."
                        )
                        return
                    }
                    LoggerManager.loggingDebug("ArgCommand-run", "Start get args from later add params..")
                    runLaterAddParams(event, args)
                    addTemplateFromArgs(args)
                } else { //args.size > argsSize
                    addTemplateFromArgs(args.subArgs(0, argsSize))
                }
            }.recoverCatching {
                LoggerManager.loggingWarn("ArgCommand-run",it.message ?: "Error ${it::class.qualifiedName ?: "<anonymous class>"}")
                return
            }
            LoggerManager.loggingDebug("ArgCommand-run", "Added all parameters to TemplateCallMap >> super")

            this.runImpl(event, templateCallNameImpl)
        }

        private suspend fun runLaterAddParams(event: MessageEvent, args: Parameter) {
            val startSize = args.size
            LoggerManager.loggingDebug("ArgCommand-laterAddParam", "Start add param")
            for (i in startSize until argsSize) { //new msg to data
                event.subject.sendMessage(
                    "请为 ${event.message.contentToString()} 指令提供值 [$i/$argsSize]\n${
                        describeMessage(
                            i
                        )
                    }\n(${laterAddParamsTimeoutSecond}秒后未响应则自动停止)"
                )
                val nextMessage = event.nextMessageOrNull(laterAddParamsTimeoutSecond * 1000L) {
                    true
                } ?: kotlin.run {
                    event.message.quote().plus("已超时")
                    return
                }
                var isType = request == null || request.isEmpty() //if is null ,then mean do not have request
                if (request != null) for (messageElement in nextMessage) {
                    if (messageElement is MessageContent) {
                        if (request[i]?.checkType(messageElement) == true) isType = true
                    }
                }
                if (!isType) errorArg(i, event, args)
                LoggerManager.loggingTrace(
                    "ArgCommand-laterAddParams",
                    "Added param: ${nextMessage.contentToString()}"
                )
                if (request?.get(i) != null) args.addAny(request[i]!!.switchType(nextMessage))
                else args.add(nextMessage)
            }
        }
        
        private suspend fun errorArg(argIndex: Int, event: MessageEvent, args: Parameter,runFrequency: Int = 0) {
            if (runFrequency > 3) {
                event.quote("错误的次数太多了,已自动停止！")
                throw Throwable("Stop running ArgCommand.errorArg,too many runs!")
            }
            event.quote("错误的参数类型\n需要：${request!![argIndex]} 的类型，请重新为 $argIndex 位参数提供值！")
            val nextMessage = event.nextMessageOrNull(laterAddParamsTimeoutSecond * 1000L) {
                true
            } ?: kotlin.run {
                event.message.quote().plus("已超时")
                return
            }
            var isType = false
            for (messageElement in nextMessage) {
                if (messageElement is MessageContent) {
                    if (request[argIndex]?.checkType(messageElement) == true) isType = true
                }
            }
            if (!isType) errorArg(argIndex, event, args)
            args.add(nextMessage)
        }

        private fun addTemplateFromArgs(args: Parameter) {
            for ((index, arg) in args.withIndex()) {
                templateCallNameImpl["arg${index + 1}"] = arg
            }
        }

        /**
         * 由 [isPrefixForAll] 自动检测名称是否符合要求
         *
         * 如果符合要求执行 [run] 并返回 [run] 的内容
         *
         * 反之则返回`null`
         * */
        private inline fun <K> checkCommandName(checkNativeObj: String, shouldBe: String, run: () -> K): K? {
            if (isPrefixForAll) {
                if (checkNativeObj.startsWith(shouldBe)) {
                    return run.invoke()
                }
                return null
            }
            if (checkNativeObj.endsWith(shouldBe)) { //its suffix
                return run.invoke()
            }
            return null
        }

        override fun isThis(commandMessage: String): Boolean {
            for (commandName in name) {
                checkCommandName(commandMessage, commandName) {
                    return@checkCommandName true
                } ?: continue
                return true
            }
            return false
        }
    }
}
