package com.pigeonyuze.listener.impl.template

import com.pigeonyuze.listener.EventListener
import com.pigeonyuze.listener.impl.BaseListenerImpl
import com.pigeonyuze.listener.impl.ListenerImpl
import com.pigeonyuze.template.Parameter
import com.pigeonyuze.template.TemplateImpl
import net.mamoe.mirai.event.Event
import kotlin.reflect.KClass

/**
 *
 * ## 事件模板
 *
 * 用于构建一个用于 [ListenerImpl] 的 [TemplateImpl] 模板
 *
 * 该项独立于一般的模板 即只会在 [EventListener] 中支持的`run`项
 *
 * 每一个 [EventTemplate] 都应该对应 [K] 中所含有的函数
 *
 * 你可以由此构建，也可用 [EventTemplateBuilder] 以 `DSL` 语法构建
 *
 * ### 注意
 *
 * 在调用 [execute] 时的 [Parameter] 内**必须有**一个属于 [K] 的事件对象
 *
 * @param name 调用的名称
 * @param type 返回的 [KClass] 类型
 * @param call 运行模板时会调用的值，与 [TemplateImpl] 中的 [TemplateImpl.execute] 实现相同
 *
 * @property Return 返回值类型
 * @property K 支持的事件
 *
 * @see BaseListenerImpl
 * @see EventTemplateBuilder
 *
 * */
class EventTemplate<K : Event, Return : Any>(
    override val name: String,
    override val type: KClass<out Return>,
    private val call: suspend K.(Parameter) -> Return,
) : TemplateImpl<Return> {

    override suspend fun execute(args: Parameter): Return {
        return call.invoke(args.getEventAndDrop(), args)
    }
}