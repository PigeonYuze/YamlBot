package com.pigeonyuze.template

import kotlin.reflect.KClass

interface Template {

    /**
     *
     * */
    suspend fun callValue(functionName: String,args: Parameter) : Any

    fun functionExist(functionName: String) : Boolean

    fun findOrNull(functionName: String) : TemplateImpl<*>?

    /**
     * function will not return null!
     * if this return null then the function is not exist
     * */
    suspend fun callOrNull(functionName: String,args: Parameter) : Any?{
        if (functionName.startsWith("%")) functionName.drop(1)
        if (functionName.endsWith("%")) functionName.dropLast(1)
        if (functionName.endsWith("()")) functionName.dropLast(2)
        if (!functionExist(functionName)) return null
        return callValue(functionName,args)
    }

    fun values() : List<TemplateImpl<*>>

    /**
     * 该项适用于已知的函数调用
     * */
    suspend fun call(functionName: String, args: Parameter) : Any{
        return callOrNull(functionName,args) ?: error("Cannot find $functionName")
    }


}


interface TemplateImpl<K : Any>{
    suspend fun execute(args: Parameter): K
    val type: KClass<K>
    val name: String
}
