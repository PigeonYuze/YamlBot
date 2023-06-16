package com.pigeonyuze.template

import com.pigeonyuze.util.SerializerData
import kotlin.reflect.KClass

class TemplateUnits(
    override val type: KClass<out Any>,
    override val name: String,
    val execute: suspend (Parameter) -> Any,
) : TemplateImpl<Any> {
    var serializerDataArrayOrNull: Array<SerializerData>? = null
    override suspend fun execute(args: Parameter): Any {
        return execute.invoke(args)
    }

}