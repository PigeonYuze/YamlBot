package com.pigeonyuze.template

import com.pigeonyuze.util.SerializerData
import kotlin.reflect.KClass

interface Template {

    /**
     *
     * */
    suspend fun callValue(functionName: String,args: List<String>) : Any // TODO: 2022/12/29  maybe we can make args be List<Any> ?

    fun functionExist(functionName: String) : Boolean

    fun findOrNull(functionName: String) : TemplateImpl<*>?

    /**
     * function will not return null!
     * if this return null then the function is not exist
     * */
    suspend fun callOrNull(functionName: String,args: List<String>) : Any?{
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
    suspend fun call(functionName: String, args: List<String>) : Any{
        return callOrNull(functionName,args) ?: error("Cannot find $functionName")
    }
    companion object{
        /**
         * **说明**
         *
         * 该函数用于并不实现但是为了继承必须存在的函数
         *
         * 运行后抛出错误[UnsupportedOperationException] : `Not implemented, should not be called`
         *
         * 如果你遇到该函数请检查该项是否继承自[SerializerData.EventAllRun] 并直接调用 [SerializerData.EventAllRun.eventExecuteRun]
         *
         * 一般来说 该项伴随有注解[Deprecated]
         * */
        internal fun noImpl(): Nothing =
            throw UnsupportedOperationException("Not implemented, should not be called")
    }

}


interface TemplateImpl<K : Any>{
    suspend fun execute(args: List<String>): K
    val type: KClass<K>
    val name: String
}

// TODO (Pigeon_Yuze): 2023/1/3  封装一个用于存储参数的类