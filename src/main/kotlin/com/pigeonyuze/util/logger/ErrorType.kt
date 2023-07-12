package com.pigeonyuze.util.logger

import com.pigeonyuze.template.TemplateImpl
import com.sksamuel.hoplite.Pos

enum class ErrorType {
    READING_CONFIG,
    RUNNING_FUNCTION,
    UNSUPPORTED_PARAMETER,
    UNKNOWN,
}

sealed class ErrorTrace {
    abstract val errorTraceString: String
    abstract val trace: Pos
    fun getTraceMessage() = buildString {
        append(errorTraceString)
        append(" -> ")
        append(trace.loc())
    }

    private companion object {
        fun Pos.loc() = when (this) {
            is Pos.NoPos -> "Unknown track"
            is Pos.SourcePos -> source
            is Pos.LineColPos -> "$source:${line.inc()}:$col"
            is Pos.LinePos -> "$source:${line.inc()}"
        }
    }



    data class MissingParameter(override val trace: Pos,private val missingObj: String): ErrorTrace() {
        override val errorTraceString: String
            by lazy { "'$missingObj': Missing parameter from config" }
    }

    data class UnknownParameter(override val trace: Pos,private val unknown: String, private val shouldBe: List<String>) : ErrorTrace() {
        override val errorTraceString: String
            by lazy { "'$unknown': Unknown parameter '$unknown'. This parameter requires the specified parameters, which should be: ${shouldBe.joinToString()}" }
    }

    data class FunctionThrowError(override val trace: Pos,private val error: Throwable): ErrorTrace() {
        override val errorTraceString: String
            by lazy { "Function '${error.stackTrace[0].methodName}'(in ${error.stackTrace[0].className.substringAfterLast('.')}) throws ${error::class}\n\t\t\t> ${error.message}" }
    }

    data class CannotParseAsAnyone(override val trace: Pos,private val cause: String): ErrorTrace() {
        override val errorTraceString: String
            by lazy { "Can not parse to any objects, because: $cause" }
    }

    class WrongYamlTypeError(override val trace: Pos,private val readingObj: String,private val name: String,private vararg val shouldBe: String): ErrorTrace() {
        override fun toString(): String {
            return "WrongYamlTypeError(trace=$trace,readingObj=$readingObj,name=$name,shouldBe=${shouldBe.contentToString()})"
        }

        override val errorTraceString: String
            by lazy { "'$name': Defined as a ${shouldBe.joinToString()} but a $readingObj cannot be converted to a ${shouldBe.joinToString()}" }
    }
}

sealed class ErrorAbout {
    abstract val msg: String

    data class ByInstance(val instantiate: String) : ErrorAbout() {
        override val msg: String by lazy { "Could not instantiate '$instantiate' because:" }
    }

    data class ByRunningTemplate(val function: TemplateImpl<out Any>) : ErrorAbout() {
        override val msg: String by lazy { "Could not run template '${function.name}' for return element '${function.type.simpleName}' because:" }
    }
}