package com.pigeonyuze.template

import com.pigeonyuze.YamlBot
import com.pigeonyuze.isDebugging0
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KClass

interface Template {

    /**
     *
     * */
    suspend fun callValue(functionName: String, args: Parameter): Any {
        return findOrNull(functionName)!!.execute(args)
    }

    fun functionExist(functionName: String): Boolean {
        return findOrNull(functionName) != null
    }

    fun findOrNull(functionName: String): TemplateImpl<*>? {
        values().forEach {
            if (it.name == functionName) return it
        }
        return null
    }

    /**
     * function will not return null!
     * if this return null then the function is not exist
     * */
    suspend fun callOrNull(functionName: String, args: Parameter): Any? {
        return YamlBot.async {
            if (functionName.startsWith("%")) functionName.drop(1)
            if (functionName.endsWith("%")) functionName.dropLast(1)
            if (functionName.endsWith("()")) functionName.dropLast(2)
            if (!functionExist(functionName)) return@async null
            return@async callValue(functionName, args)
        }.await()
    }


    fun values(): List<TemplateImpl<*>>

    /**
     * 该项适用于已知的函数调用
     * */
    suspend fun call(functionName: String, args: Parameter): Any {
        return callOrNull(functionName, args) ?: error("Cannot find $functionName")
    }


}


interface TemplateImpl<K : Any> : CoroutineScope {
    suspend fun execute(args: Parameter): K
    val type: KClass<out K>
    val name: String

    override val coroutineContext: CoroutineContext
        get() = if (!isDebugging0) CoroutineScope(YamlBot.coroutineContext).coroutineContext else EmptyCoroutineContext

    companion object {
        fun TemplateImpl<out Any>.canNotFind(what: String, where: String): Nothing {
            throw NotImplementedError("Cannot find '$what' in '$where': at ${this.name} function")
        }


        fun TemplateImpl<out Any>.nonImpl(what: String, where: String, moreMessage: String = ""): Nothing {
            throw NotImplementedError("No implemented function'$what' in '$where': at ${this.name} function${if (moreMessage.isNotEmpty()) "\n$moreMessage" else ""} ")
        }
    }
}
