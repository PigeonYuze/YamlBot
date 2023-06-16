package com.pigeonyuze.template

import com.pigeonyuze.util.DslTemplateBuilder
import com.pigeonyuze.util.SerializerData
import kotlin.reflect.KClass

class TemplateBuilder {
    val elements = mutableListOf<TemplateNode>()

    inner class TemplateNode {
        lateinit var name: String
        lateinit var type: KClass<out Any>
        lateinit var call: suspend (Parameter) -> Any
        var serializerData: Array<SerializerData>? = null

        @DslTemplateBuilder
        infix fun named(name: String) = apply { this.name = name }

        @DslTemplateBuilder
        infix fun typed(type: KClass<out Any>) = apply { this.type = type }

        @DslTemplateBuilder
        inline infix fun <reified K : Any> called(noinline call: suspend (Parameter) -> K) = apply {
            this.call = call
            @Synchronized
            if (!::type.isInitialized) {
                type = K::class
            }
        }

        @DslTemplateBuilder
        infix fun provided(annotations: Array<SerializerData>) = apply { this.serializerData = annotations }

        @DslTemplateBuilder
        infix fun provided(annotationObj: SerializerData) = apply { this.serializerData = arrayOf(annotationObj) }
    }


    /**
     * 构建一个 [TemplateNode] 并加入元素列表
     * */
    @DslTemplateBuilder
    inline infix fun <reified R : Any> String.executed(noinline call: suspend (Parameter) -> R) {
        elements.add(
            TemplateNode()
                    named this
                    typed R::class
                    called call
        )
    }

    /**
     *  获取 [TemplateNode] 体设置内容
     * - 使用 [TemplateNode.named] 设置名称
     * - 使用 [TemplateNode.typed] 设置类型
     * - 使用 [TemplateNode.called] 设置`called`体
     * */
    fun add(setting: TemplateNode.() -> Unit) {
        val node = TemplateNode()
        setting(node)
        elements.add(node)
    }

    infix fun add(templateNode: TemplateNode) {
        elements.add(templateNode)
    }

    /**
     * 构建
     * */
    fun build(): List<TemplateImpl<*>> {
        val list = mutableListOf<TemplateImpl<out Any>>()
        for (node in elements) {
            list.add(TemplateUnits(name = node.name, type = node.type, execute = node.call).also {
                it.serializerDataArrayOrNull = node.serializerData
            })
        }
        return list
    }
}

fun buildTemplates(run: TemplateBuilder.() -> Unit): List<TemplateImpl<*>> {
    val builder = TemplateBuilder()
    run.invoke(builder)
    return builder.build()
}
