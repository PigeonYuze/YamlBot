package com.pigeonyuze.listener.impl.template

import com.pigeonyuze.template.Parameter
import com.pigeonyuze.util.DslEventTemplateBuilder
import net.mamoe.mirai.event.Event
import kotlin.reflect.KClass

/**
 * ## DSL 构建模板
 *
 * 使用 [execute] 增加模板
 *
 * 在添加完模板后使用 [build] 构建为 [EventTemplateValues]
 *
 * 可使用顶层函数 [buildEventTemplate] 直接调用
 *
 * ### 添加方法
 *
 * - 直接使用 [execute] 扩展函数来添加内容
 *    - 扩展函数的`String`为名称
 *    - 参数为`call`体
 *    - 会自动生成`type`
 * - 使用 [add] 设置内部来添加内容
 *    - 使用 [named] 设置名称
 *    - 使用 [typed] 设置类型
 *    - 使用 [called] 设置`called`体
 *
 * 实例代码
 * ```kotlin
 * eventTemplateBuilder {
 *  "name" execute {}
 *  add {
 *      named "" typed Any::class called {}
 *  }
 * }
 * ```
 * @property K 当前模板的事件
 *
 * @see EventTemplate
 * @see EventTemplateValues
 * ```
 * */
class EventTemplateBuilder<K : Event> {
    val elements: MutableList<TemplateNode> = mutableListOf()

    inner class TemplateNode {
        var name: String = ""
        var type: KClass<out Any> = Any::class
        var call: suspend K.(Parameter) -> Any = {}

        @DslEventTemplateBuilder
        infix fun named(name: String) = apply { this.name = name }

        @DslEventTemplateBuilder
        infix fun typed(type: KClass<out Any>) = apply { this.type = type }

        @DslEventTemplateBuilder
        infix fun called(call: suspend K.(Parameter) -> Any) = apply { this.call = call }
    }

    @DslEventTemplateBuilder
    inline infix fun <reified R : Any> String.execute(noinline call: suspend K.(Parameter) -> R) {
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

    /**
     * 构建为 [EventTemplateValues]
     * */
    fun build(): EventTemplateValues<K> {
        val list = mutableListOf<EventTemplate<K, out Any>>()
        for (node in elements) {
            list.add(EventTemplate(name = node.name, type = node.type, call = node.call))
        }
        return EventTemplateValues(list)
    }

}

/**
 * 构建并生成 [EventTemplateValues]
 *
 * @see EventTemplateValues
 * @see EventTemplateBuilder
 * */
fun <K : Event> buildEventTemplate(setting: EventTemplateBuilder<K>.() -> Unit): EventTemplateValues<K> {
    val builder = EventTemplateBuilder<K>()
    setting.invoke(builder)
    return builder.build()
}

