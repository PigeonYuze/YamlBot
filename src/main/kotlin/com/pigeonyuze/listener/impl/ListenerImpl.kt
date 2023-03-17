package com.pigeonyuze.listener.impl

import com.pigeonyuze.listener.YamlEventListener
import com.pigeonyuze.listener.impl.ListenerImpl.NoTemplateImpl
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.EventPriority
import kotlin.reflect.KClass

/**
 * ## 监听功能的实现
 *
 * 由此支持事件的监听功能
 *
 * 内置有对 [YamlEventListener] 中的支持
 *
 * 使用 [addTemplate] 实现向预定模板添加内容
 *
 * 如果不需要添加模板可参照 [NoTemplateImpl] 或直接不调用 [addTemplate]
 *
 * @see NoTemplateImpl
 * @see BaseListenerImpl
 * */
interface ListenerImpl<K : Event> {

    fun addTemplate(event: K, template: MutableMap<String, Any>)

    fun execute(
        eventChannel: EventChannel<Event>,
        run: () -> Unit,
        priority: EventPriority, /* = EventPriority.NORMAL */
    )

    fun filterExecute(
        filter: String,
        eventChannel: EventChannel<Event>,
        run: () -> Unit,
        priority: EventPriority,/* = EventPriority.NORMAL */
    )

    fun onceExecute(
        filter: String = "",
        eventChannel: EventChannel<Event>,
        run: () -> Unit,
        priority: EventPriority,
    )

    fun filterCheck(
        filter: String,
        template: MutableMap<String, Any>,
    ): Boolean {
        // a==b && b==c || c==d
        return evaluate(
            filter, template
        )
    }

    /**
     * 不提供模板列表的 [addTemplateImpl]
     * */
    interface NoTemplateImpl<K : Event> : ListenerImpl<K> {
        override fun addTemplate(event: K, template: MutableMap<String, Any>) {
            addTemplateImpl(event)
        }

        fun addTemplateImpl(event: K)
    }

    companion object {
        const val equalExpression = "=="
        const val unequalExpression = "!="
        const val instanceOfExpression = "is"

        fun evaluate(expression: String, template: MutableMap<String, Any>): Boolean {
            var result = false
            var leftOperand = false
            var leftValue: String? = null
            var rightValue: String? = null
            var expressionKind: EvaluateTokenKind? = null

            for (token in expression.split(' ')) {
                when (token) {
                    "!" -> leftOperand = !leftOperand
                    "true" -> {
                        if (leftOperand) {
                            result = true
                            continue
                        } else {
                            leftOperand = true
                        }
                        expressionKind = EvaluateTokenKind.BOOLEAN
                    }
                    "false" -> {
                        if (leftOperand) {
                            leftOperand = false
                        } else {
                            result = false
                            continue
                        }
                        expressionKind = EvaluateTokenKind.BOOLEAN
                    }
                    /* 运算符 */
                    "||" -> {
                        if (expressionKind == null) {
                            throw IllegalArgumentException("Illegal expression: cannot found value")
                        }
                        if (leftOperand) {
                            result = true
                            continue
                        }
                    }
                    "&&" -> {
                        if (expressionKind == null) {
                            throw IllegalArgumentException("Illegal expression: cannot found value")
                        }
                        if (!leftOperand) {
                            result = false
                            continue
                        }
                    }
                    else -> when {
                        /* 判断完左值，运算符，右值 */
                        leftValue != null && rightValue != null -> {
                            expressionKind!!.execute(
                                leftValue.getValueFromTemplate(template),
                                rightValue.getValueFromTemplate(template)
                            ).also { leftOperand = it }
                        }
                        /* 此处应该为运算符 */
                        leftValue != null && rightValue == null -> expressionKind = EvaluateTokenKind.ofValue(token)
                        /* 补充值 */
                        rightValue == null -> rightValue = token
                        else -> leftValue = token
                    }
                }
            }
            return result
        }

        private fun String.getValueFromTemplate(template: MutableMap<String, Any>): Any {
            return if (this.startsWith("%call-") && this.endsWith("%")) template[this.drop(6).dropLast(1)]
                ?: this else this
        }

        private enum class EvaluateTokenKind {
            EQUAL {
                override fun execute(leftValue: Any, rightValue: Any): Boolean {
                    return leftValue == rightValue
                }
            },
            UNEQUAL {
                override fun execute(leftValue: Any, rightValue: Any): Boolean {
                    return leftValue != rightValue
                }
            },
            INSTANCE_OF {
                override fun execute(leftValue: Any, rightValue: Any): Boolean {
                    if (rightValue !is CharSequence) {
                        return leftValue::class == rightValue::class
                    }
                    return leftValue::class.simpleName == rightValue
                }
            },
            BOOLEAN {
                override fun execute(leftValue: Any, rightValue: Any): Boolean {
                    throw NotImplementedError()
                }
            }
            ;

            companion object {
                fun ofValue(token: String): EvaluateTokenKind {
                    return when (token) {
                        equalExpression -> EQUAL
                        unequalExpression -> UNEQUAL
                        instanceOfExpression -> INSTANCE_OF
                        else -> throw IllegalArgumentException("Unknown token '$token'")
                    }
                }
            }


            abstract fun execute(leftValue: Any, rightValue: Any): Boolean
        }
    }

}

/**
 * ### 所有监听器实现的顶层类
 *
 * 所有监听器实现类**都应该继承实现此类**
 *
 * **注意：**
 *
 * - 修改模板请直接修改 [template] 而不是 [addTemplate]
 * - 子类直接实现 [addTemplateImpl] 直接修改本类的 [template]
 *
 * 可参照以下代码
 * ```kotlin
 * val baseListenerImpl: BaseListenerImpl = ...
 * // 子类实现
 * baseListenerImpl.template[""] = /* value */
 * // 外部调用修改模板
 * baseListenerImpl.template = newTemplate // 提供新的模板，操作会根据此模板来设置
 * baseListenerImpl.addTemplateImpl(event)
 * ```
 *
 * @see ListenerImpl
 * @see ListenerImpl.NoTemplateImpl
 * */
internal abstract class BaseListenerImpl<BaseEvent : Event>(
    var template: MutableMap<String, Any>,
) : NoTemplateImpl<BaseEvent> {


    abstract val eventClass: KClass<BaseEvent>

    /**
     *  @suppress **注意** <br> 请不要在子类的 [addTemplateImpl] 中调用本函数，否则可能会造成栈溢出！
     * */
    final override fun addTemplate(event: BaseEvent, template: MutableMap<String, Any>) {
        this.template = template
        addTemplateImpl(event)
    }

    override fun onceExecute(
        filter: String,
        eventChannel: EventChannel<Event>,
        run: () -> Unit,
        priority: EventPriority,
    ) {

        var runBoolean = true
        if (filter.isNotEmpty()) {
            runBoolean = ListenerImpl.evaluate(filter, template)
        }
        eventChannel.subscribeOnce(
            eventClass
        ) {
            if (runBoolean) {
                run.invoke()
            }
        }
    }


    override fun execute(eventChannel: EventChannel<Event>, run: () -> Unit, priority: EventPriority) {
        eventChannel.subscribeAlways(
            eventClass
        ) {
            run.invoke()
        }
    }

    override fun filterExecute(
        filter: String,
        eventChannel: EventChannel<Event>,
        run: () -> Unit,
        priority: EventPriority,
    ) {
        eventChannel.subscribeAlways(
            eventClass
        ) {
            if (ListenerImpl.evaluate(
                    filter, template
                )
            ) run.invoke()
        }
    }
}