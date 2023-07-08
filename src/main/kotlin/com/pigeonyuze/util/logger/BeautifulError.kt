package com.pigeonyuze.util.logger

import com.pigeonyuze.isDebugging0
import com.pigeonyuze.util.logger.ErrorType.*
import net.jcip.annotations.ThreadSafe
import java.io.PrintStream
import java.io.PrintWriter

@ThreadSafe
class BeautifulError private constructor(
    private val errorType   : ErrorType,
    private val errorAbout  : ErrorAbout,
            val causes      : MutableList<ErrorTrace>,
) : Throwable() {
    override val message: String get() = beautifulStackTrace()

    override fun printStackTrace(s: PrintStream?) {
        s != null || return
        s!!.println(beautifulStackTrace())
        if (isDebugging0) {
            for (traceElement in stackTrace)
                s.println("\tat $traceElement")
        }
    }

    override fun printStackTrace(s: PrintWriter?) {
        s != null || return
        s!!.println(beautifulStackTrace())
        if (isDebugging0) {
            for (traceElement in stackTrace)
                s.println("\tat $traceElement")
        }
    }

    constructor(
        errorType: ErrorType, errorAbout: ErrorAbout, vararg causes: ErrorTrace,
    ) : this(errorType,errorAbout,causes.toMutableList())

    @Synchronized
    fun isEmpty() = synchronized(this) {
        causes.isEmpty()
    }

    @Synchronized
    fun addCause(cause: ErrorTrace) = synchronized(this) {
        causes.add(cause)
        return@synchronized this
    }

    @Synchronized
    fun addCause(cause: BeautifulError): BeautifulError {
        synchronized(this) {
            causes.addAll(cause.causes)
            return this
        }

    }

    operator fun plusAssign(cause: BeautifulError) {
        synchronized(this) {
            causes.addAll(cause.causes)
        }
    }
    @Synchronized
    fun beautifulStackTrace() : String{
        fun StringBuilder.newLine() = this.append(lineSeparator)
        fun StringBuilder.plusCauseText(text: String,innerNum: Int = 1) {
            for (i in 0 until innerNum) {
                this.append(tab)
            }
            this.append(text)
            this.newLine()
        }
        synchronized(this) {
            val sb = StringBuilder()
            sb.append(errorType.outString())
            sb.newLine()
            sb.append(space)
            sb.append("because: ")
            sb.newLine()
            sb.plusCauseText(errorAbout.msg)
            sb.newLine()
            if (causes.isEmpty()) {
                sb.plusCauseText("Unknown cause.",2)
            }
            for (cause in causes) {
                sb.plusCauseText(cause.getTraceMessage(),2)
            }
            return sb.toString()
        }
    }

    private companion object {
        val lineSeparator: String by lazy { System.lineSeparator() }
        const val tab = "\t"
        const val space = ' '

        fun ErrorType.outString() = when(this) {
            READING_CONFIG -> "Error loading config"
            RUNNING_FUNCTION -> "Error calling function"
            UNSUPPORTED_PARAMETER -> "Error calling by unsupported parameter "
            UNKNOWN -> "Error"
        }
    }
}