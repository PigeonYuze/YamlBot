package com.pigeonyuze.command.element

import com.pigeonyuze.command.Command
import com.pigeonyuze.listener.impl.template.EventTemplate
import com.pigeonyuze.template.Parameter
import com.pigeonyuze.template.Template
import com.pigeonyuze.template.asParameter
import com.pigeonyuze.template.data.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.command.descriptor.CommandArgumentParserException
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.yamlkt.Comment

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

    suspend fun invoke(event: MessageEvent, lastCondition: Condition, templateCall: MutableMap<String, Any?>) {
        val callValue =
            this.call?.let { Command.callFunction(it, event, templateCall).toString().toBooleanStrictOrNull() } ?: false
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
open class TemplateYML private constructor() {
    lateinit var use: ImportType
    lateinit var call: String

    @kotlinx.serialization.Transient
            /**
             * @suppress Serializable transient
             * */
    var parameter = Parameter()

    lateinit var args: List<String>
    lateinit var name: String

    constructor(
        use: ImportType,
        call: String,
        args: List<String>,
        name: String,
    ) : this() {
        if (name.contains("%")) illegalArgument("${use.name}.${call}名称不应该含有 '%'")
        //region Init
        this.name = name
        this.use = use
        this.call = call
        this.args = args
        this.parameter = args.asParameter()
        //endregion
    }

    constructor(
        use: ImportType,
        call: String,
        args: Parameter,
        name: String,
    ) : this() {
        if (name.contains("%")) illegalArgument("${use.name}.${call}名称不应该含有 '%'")
        //region Init
        this.name = name
        this.use = use
        this.call = call
        this.args = args.stringValueList
        this.parameter = args
        //endregion
    }

    override fun toString(): String {
        return "TemplateYML(use=$use, call='$call', parameter=$parameter, args=$args, name='$name')"
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
    },
    MESSAGE {
        override fun getProjectClass(): Template = MessageTemplate
    },
    GROUP_ANNOUNCEMENTS {
        override fun getProjectClass(): Template = GroupAnnouncementsTemplate
    },
    GROUP_ACTIVE {
        override fun getProjectClass(): Template = GroupActiveTemplate
    },
    /**
     * @see EventTemplate
     * */
    EVENT {
        override fun getProjectClass(): Template {
            throw NotImplementedError()
        }
    },
    MESSAGE_MANAGER {
        override fun getProjectClass(): Template = MessageManagerTemplate
    },
    REFLECTION {
        override fun getProjectClass(): Template = JvmReflectionTemplate
    },
    ROAMING_SUPPORTED {
        override fun getProjectClass(): Template = RoamingSupportedTemplate
    }

    ;

    abstract fun getProjectClass(): Template
}
