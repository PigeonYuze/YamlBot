package com.pigeonyuze.command

import com.pigeonyuze.com.pigeonyuze.LoggerManager
import com.pigeonyuze.command.AnsweringMethod.*
import com.pigeonyuze.template.*
import com.pigeonyuze.util.SerializerData
import com.pigeonyuze.util.SerializerData.SerializerType.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.command.descriptor.CommandArgumentParserException
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.code.MiraiCode
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageChain.Companion.serializeToJsonString
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.nextMessageOrNull
import net.mamoe.yamlkt.Comment
import kotlin.reflect.jvm.jvmName

@Serializable
open class Command(
    @Comment("指令的全称 使用者在发出该内容后会触发指令 (使用contentToString比较)")
    open val name: List<String>,
    @Comment(
        """
        回复的方式 可选为QUOTE,SEND_MESSAGE,AT_SEND
        QUOTE 为回复信息
        SEND_MESSAGE 为直接发送信息
        AT_SEND 为at发送者后发送信息
    """
    )
    open val answeringMethod: AnsweringMethod,
    @Comment(
        """
        回答的内容
        可提供%call-value%调用参数 (详细可见 readme.md 所标注的方法)
    """
    )
    open val answerContent: String,
    @Comment(
        """
        会同时执行的操作 如有需要调用参数 需要在此处进行编写
        详细可见 readme.md
    """
    )
    open val run: List<TemplateYML>,
    @Comment(
        """
        运行还需要的额外条件(该项暂时没有用 因为还没有写好的返回布尔值的内容)
        若无需条件请使用以下内容：
        - request: none
          call: null
          runRequest: false
    """
    )
    open val condition: List<Condition> = listOf(),
) {
    @kotlinx.serialization.Transient
    val templateCallName = mutableMapOf<String, Any?>()

    init {
        val run by lazy { this.run }
        val name by lazy { this.name }
        val templateCallMap = this.templateCallName
        for (templateYML in run) {
            if (templateCallMap.contains(templateYML.name)) {
                LoggerManager.loggingError("Command-run", "${name[0]} CommandSetting IllegalArgument! $templateCallMap")
                illegalArgument("${templateYML.name} 重复了(${templateYML.use}.${templateYML.call})")
            }
            templateCallMap[templateYML.name] = null
        }
    }

    open fun isThis(commandMessage: String) = commandMessage in name

    open suspend fun run(event: MessageEvent) {
        if (event.message.contentToString() !in name) return
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
        val answer = parseMessage(answerContent, templateCallName).toMiraiMessage()
        LoggerManager.loggingDebug("Command-run", answer.serializeToMiraiCode())
        LoggerManager.loggingTrace("Command-run", "Answering method : $answeringMethod")
        when (answeringMethod) {
            QUOTE -> event.subject.sendMessage(event.message.quote() + answer)
            SEND_MESSAGE -> event.subject.sendMessage(answer)
            AT_SEND -> event.subject.sendMessage(At(event.sender) + answer)
        }
        LoggerManager.loggingInfo("Command-run", "The `${name[0]}` command is finished(send message finish).")
    }

    private suspend fun judgment(event: MessageEvent) {
        condition.filterIndexed { index, it ->
            it.invoke(event, condition[if (index == 0) 0 else index - 1])
            it.runRequest
        }
    }

    companion object {
        fun String.toMiraiMessage() = MiraiCode.deserializeMiraiCode(this)

        fun parseMessage(messageContent: String, templateCallName: MutableMap<String, Any?>): String {
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
                        (if (value is MessageChain) value.serializeToMiraiCode() else value?.toString() )
                            ?: illegalArgument("Cannot find $callName in 'run'!")
                    )
                    LoggerManager.loggingTrace("Command-parseMessage", "Converts -> `$callName`,replace value!")
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
            templateCall: MutableMap<String, Any?>? = null,
        ): Any {
            val template = templateYML.use.getProjectClass().findOrNull(templateYML.call)
                ?: illegalArgument("Cannot find function ${templateYML.call}")
            LoggerManager.loggingTrace(
                "Command-callFunction",
                "Template: ${template::class.jvmName} name: ${template.name}"
            )
            val args = templateYML.args.toMutableList()
            for ((index, arg) in args.withIndex()) {
                if (!arg.contains("%call-")) continue
                args[index] = parseMessage(arg, templateCall!!)
            }
            val annotationList = template::class.annotations
            for (annotation in annotationList) {

                if (annotation is SerializerData) {
                    val plusJson = when (annotation.serializerJSONType) {
                        MESSAGE -> event.message.serializeToJsonString()
                        SUBJECT_ID -> event.subject.id.toString()
                        EVENT_ALL -> event.toString()
                        SENDER_NAME -> event.senderName
                        SENDER_NICK -> event.sender.nick
                        SENDER_ID -> event.sender.id.toString()
                        COMMAND_ID -> this.hashCode().toString()
                    }
                    if (annotation.serializerJSONType === EVENT_ALL && template is SerializerData.EventAllRun) {
                        return runBlocking {
                            (template as SerializerData.EventAllRun).eventExecuteRun(
                                args,
                                event
                            )
                        }
                    }

                    if (args.lastIndex >= (annotation.buildIndex) && annotation.buildIndex != -1) {
                        val oldValue = args[annotation.buildIndex]
                        args[annotation.buildIndex] = plusJson
                        args.add(oldValue)
                    } else {
                        args.add(plusJson)
                    }
                }


            }
            return template.execute(args)
        }
    }


}

class OnlyRunCommand(
    @Comment("指令的全称 使用者在发出该内容后会触发指令 (使用contentToString比较)")
    override val name: List<String>,
    @Comment(
        """
        会执行的操作 如有需要调用参数 需要在此处进行编写
        详细可见 readme.md
    """
    )
    override val run: List<TemplateYML>,
    @Comment(
        """
        运行还需要的额外条件
        若无需条件请使用以下内容：
        - request: none
          call: null
          runRequest: false
    """
    )
    override val condition: List<Condition>,
) : Command(name, SEND_MESSAGE, "", run, condition)


@ExperimentalStdlibApi
// TODO (Pigeon_Yuze): 2023/1/3  含有参数的指令
class ArgCommand(
    @Comment("指令的全称 不包含参数部分！！")
    override val name: List<String>,
    @Comment(
        """
        回复的方式 可选为QUOTE,SEND_MESSAGE,AT_SEND
        QUOTE 为回复信息
        SEND_MESSAGE 为直接发送信息
        AT_SEND 为at发送者后发送信息
    """
    )
    override val answeringMethod: AnsweringMethod,
    @Comment(
        """
        回答的内容
        可提供%call-value%调用参数 (详细可见 readme.md 所标注的方法)
    """
    )
    override val answerContent: String,
    @Comment(
        """
        会同时执行的操作 如有需要调用参数 需要在此处进行编写
        详细可见 readme.md
    """
    )
    override val run: List<TemplateYML>,
    @Comment(
        """
        运行还需要的额外条件
        若无需条件请使用以下内容：
        - request: none
          call: null
          runRequest: false
    """
    )
    override val condition: List<Condition>,
    private val argsSplit: Char = ' ',
    private val useLaterAddParams: Boolean = true,
    private val laterAddParamsTimeoutSecond: Int = 60,
    private val argsSize: Int,
    private val request: Map<Int, Type>? = null,
    private val describe: Map<Int, String>? = null,
) : Command(name, answeringMethod, answerContent, run, condition) {

    @Serializable
    enum class Type {
        STRING,
        INT,
        LONG,
        DOUBLE;

    }

    private fun describeMessage(size: Int) =
        if (describe?.get(size) != null) "描述: ${describe[size]} " + //含有描述
                (if (request?.get(size) == null) "" else "该项需要 ${request[size]} 格式的信息")
        else if (request?.get(size) != null) "该项需要 ${request[size]} 格式的信息" //默认描述
        else "该项无任何要求" //找不到东西描述

    override suspend fun run(event: MessageEvent) {
        // TODO (Pigeon_Yuze): 2023/1/7  对类型判断的支持
        val msg = event.message.contentToString()

        LoggerManager.loggingDebug("ArgsCommand-run", "Start get args from native message..")

        for (commandName in name) {
            if (msg.startsWith(commandName)) {
                msg.replace(commandName, "") //get only value command
            }
        }

        val args = msg.split(argsSplit).toMutableList() //get values

        LoggerManager.loggingDebug("ArgsCommand-run", "Args from native message: $args")

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
            LoggerManager.loggingDebug("ArgsCommand-run", "Start get args from later add params..")
            runLaterAddParams(event, args)
            addTemplateFromArgs(args)
        } else {
            addTemplateFromArgs(args.subList(0, argsSize))
        }
        LoggerManager.loggingDebug("ArgsCommand-run", "Added all parameters to TemplateCallMap >> super")

        super.run(event)
    }

    private suspend fun runLaterAddParams(event: MessageEvent, args: MutableList<String>) {
        val startSize = args.size
        for (i in startSize until argsSize) { //new msg to data
            event.subject.sendMessage("请为 ${event.message.contentToString()} 指令提供值 [$i/$argsSize]\n${describeMessage(i)}\n(${laterAddParamsTimeoutSecond}秒后未响应则自动停止)")
            val nextMessage = event.nextMessageOrNull(laterAddParamsTimeoutSecond * 1000L) {
                true
            } ?: kotlin.run {
                event.message.quote().plus("已超时")
                return
            }
            LoggerManager.loggingTrace("[ArgsCommand-laterAddParams]", "Added param: ${nextMessage.contentToString()}")
            args.add(nextMessage.contentToString())
        }
    }

    private fun addTemplateFromArgs(args: List<Any>) {
        for ((index, arg) in args.withIndex()) {
            super.templateCallName["arg${index + 1}"] = arg
        }

    }

    override fun isThis(commandMessage: String): Boolean {
        for (commandName in name) {
            if (commandMessage.startsWith(commandName)) {
                return true
            }
        }
        return false
    }
}

internal fun illegalArgument(
    message: String,
    cause: Throwable? = null,
): Nothing =
    throw CommandArgumentParserException(message, cause)

@Serializable
class Condition(
    @Comment("选择判断的类型 无需判断传值 none")
    val request: JudgmentMethod,
    @Comment("提供此处获取Boolean")
    val call: TemplateYML?,
) {
    @kotlinx.serialization.Transient
    var runRequest: Boolean = false //可以运行
        private set

    @Serializable
    enum class JudgmentMethod {
        @SerialName("else if true")
        ELSE_IF_TRUE {
            override fun isNested(): Boolean {
                return true
            }

            override fun need(): Boolean {
                return true
            }
        },

        @SerialName("if true")
        IF_TRUE {
            override fun isNested(): Boolean {
                return false
            }

            override fun need(): Boolean {
                return true
            }
        },

        @SerialName("else")
        ELSE {
            override fun isNested(): Boolean {
                return true
            }

            override fun need(): Boolean = true
        },

        @SerialName("else if false")
        ELSE_IF_FALSE {
            override fun isNested(): Boolean {
                return true
            }

            override fun need(): Boolean {
                return false
            }
        },

        @SerialName("none")
        NONE {
            override fun isNested(): Boolean {
                return false
            }

            override fun need(): Boolean {
                return true
            }

        },

        @SerialName("if false")
        IF_FALSE {
            override fun isNested(): Boolean {
                return false
            }

            override fun need(): Boolean {
                return false
            }
        };

        abstract fun isNested(): Boolean
        abstract fun need(): Boolean
    }

    suspend fun invoke(event: MessageEvent, lastCondition: Condition) {
        val callValue = this.call?.let { Command.callFunction(it, event).toString().toBooleanStrictOrNull() } ?: false
        if (this.request == JudgmentMethod.NONE && this.request == JudgmentMethod.ELSE) {
            runRequest = true
            return
        }
        runRequest = if (this.request.isNested()) {
            if (lastCondition.runRequest) { //上一个已判断且通过检测
                false
            } else {
                callValue == request.need() //call
            }
        } else callValue == request.need()
    }
}


@Serializable
enum class AnsweringMethod {
    QUOTE,
    SEND_MESSAGE,
    AT_SEND
}

@Serializable
data class TemplateYML(
    @Comment("使用的包 可选USER,BASEM,HTTP,MIRAI")
    val use: ImportType, //type
    @Comment("调用的函数名")
    val call: String, //value
    @Comment("传参")
    val args: List<String>,
    @Comment("命名")
    val name: String,
) {
    init {
        if (name.contains("%")) illegalArgument("${use.name}.${call}名称不应该含有 '%'")
    }
}


@Serializable
enum class ImportType {
    USER {
        override fun getProjectClass(): Template = UserTemplate
    },
    BASE {
        override fun getProjectClass(): Template = BaseTemplate
    },
    HTTP {
        override fun getProjectClass(): Template = HttpTemplate
    },
    MIRAI {
        override fun getProjectClass(): Template = MiraiTemplate
    },
    FEATURES {
        override fun getProjectClass(): Template = FeaturesTemplate
    };

    abstract fun getProjectClass(): Template
}