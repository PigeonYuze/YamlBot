package com.pigeonyuze.listener.impl

import com.pigeonyuze.listener.EventListener
import com.pigeonyuze.listener.impl.ListenerImpl.NoTemplateImpl
import com.pigeonyuze.listener.impl.template.EventTemplate
import com.pigeonyuze.listener.impl.template.EventTemplateBuilder
import com.pigeonyuze.listener.impl.template.EventTemplateValues
import com.pigeonyuze.listener.impl.template.buildEventTemplate
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.EventPriority
import javax.script.Invocable
import javax.script.ScriptEngineManager
import kotlin.reflect.KClass
import net.mamoe.mirai.event.Listener as MiraiListener

/**
 * ## 监听功能的实现
 *
 * 由此支持事件的监听功能
 *
 * 内置有对 [EventListener] 中的支持
 *
 * 使用 [addTemplateImpl] 实现向预定模板添加内容
 *
 * 如果不需要添加模板可参照 [NoTemplateImpl] 或直接不调用 [addTemplateImpl]
 *
 * @see NoTemplateImpl
 * @see BaseListenerImpl
 * */
interface ListenerImpl<K : Event> {

    /**
     * 内部实现 向[template]提供参数
     *
     * @see addTemplate
     * */
    fun addTemplateImpl(event: K, template: MutableMap<String, Any>)

    /**
     * 事件的支持的模板
     *
     * 每一个模板都对应原事件的一个函数
     *
     * @see EventTemplateValues
     * @see EventTemplate
     * @see EventTemplateBuilder
     * */
    val eventTemplate: EventTemplateValues<K>

    fun execute(
        eventChannel: EventChannel<Event>,
        run: suspend ListenerImpl<K>.(Event) -> Unit,
        priority: EventPriority, /* = EventPriority.NORMAL */
    )

    fun filterExecute(
        filter: String,
        eventChannel: EventChannel<Event>,
        run: suspend ListenerImpl<K>.(Event) -> Unit,
        priority: EventPriority,/* = EventPriority.NORMAL */
    )

    fun onceExecute(
        filter: String = "",
        eventChannel: EventChannel<Event>,
        run: suspend ListenerImpl<K>.(Event) -> Unit,
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
        override fun addTemplateImpl(event: K, template: MutableMap<String, Any>) {
            addTemplateImpl(event)
        }

        fun addTemplateImpl(event: K)
    }

    companion object {

        @Suppress("UNCHECKED_CAST")
                /* It may not good, but... */
                /**
                 * 避免泛型为`*`时 事件为[Nothing]从而无法提供事件的解决办法
                 *
                 * 从外部调用应优先考虑本函数
                 *
                 * 内部实现提供为 [addTemplateImpl]
                 * */
        fun <K : Event> ListenerImpl<K>.addTemplate(event: Event, template: MutableMap<String, Any>) {
            addTemplateImpl(event as? K ?: return, template)
        }

        const val equalExpression = "=="
        const val unequalExpression = "!="
        const val instanceOfExpression = "is"

        private fun jsEvaluate(expression: String): Boolean {
            val manager = ScriptEngineManager()
            val jsInstance = manager.getEngineByName("JavaScript")
            jsInstance.eval(
                """
                function callByEvaluate_TempFunction() {
                    return $expression;
                }
            """.trimIndent()
            )
            val invocable = jsInstance as Invocable
            return invocable.invokeFunction("callByEvaluate_TempFunction").toString() == "true"
        }

        fun evaluate(expression: String, template: MutableMap<String, Any>): Boolean {
            var result = false
            var leftOperand = false
            var leftValue: String? = null
            var rightValue: String? = null
            var expressionKind: EvaluateTokenKind? = null

            for (token in expression.split(' ')) {
                if (token.startsWith("%js:") && token.endsWith("%")) {
                    val value = jsEvaluate(token.removePrefix("%js:").removeSuffix("%"))
                    leftOperand = value
                }
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
 * - 修改模板请直接修改 [template] 而不是 [addTemplateImpl]
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

    /**
     * 该项默认支持 [Event.intercept]
     *
     * 若要新增函数请**重新实现** [Event.intercept]
     *
     * @see ListenerImpl.eventTemplate
     * */
    override val eventTemplate: EventTemplateValues<BaseEvent>
        get() = buildEventTemplate {
            "intercept" execute {
                intercept()
            }
        }

    /**
     *  当启动监听时，此参数会被初始化为对应的监听器
     *
     *  **注意**
     *
     *  此属性被声明为是`lateinit`的，当调用监听函数([onceExecute],[execute],[filterExecute]) 时才会被初始化
     *
     *  在没有开始监听之前并不会初始化此属性
     *
     *  若在未初始化时调用会导致错误[UninitializedPropertyAccessException]
     * */
    lateinit var eventListener: MiraiListener<BaseEvent>

    abstract val eventClass: KClass<BaseEvent>

    /**
     *  @suppress **注意**
     *
     *  请不要在子类的 [addTemplateImpl] 中调用本函数，否则可能会造成栈溢出！
     * */
    final override fun addTemplateImpl(event: BaseEvent, template: MutableMap<String, Any>) {
        this.template = template
        addTemplateImpl(event)
    }

    override fun onceExecute(
        filter: String,
        eventChannel: EventChannel<Event>,
        run: suspend ListenerImpl<BaseEvent>.(Event) -> Unit,
        priority: EventPriority,
    ) {

        var runBoolean = true
        if (filter.isNotEmpty()) {
            runBoolean = ListenerImpl.evaluate(filter, template)
        }
        eventListener = eventChannel.subscribeOnce(
            eventClass
        ) {
            if (runBoolean) {
                run.invoke(this@BaseListenerImpl, this)
            }
        }
    }


    override fun execute(
        eventChannel: EventChannel<Event>,
        run: suspend ListenerImpl<BaseEvent>.(Event) -> Unit,
        priority: EventPriority,
    ) {
        eventListener = eventChannel.subscribeAlways(
            eventClass
        ) {
            run.invoke(this@BaseListenerImpl, this)
        }
    }

    override fun filterExecute(
        filter: String,
        eventChannel: EventChannel<Event>,
        run: suspend ListenerImpl<BaseEvent>.(Event) -> Unit,
        priority: EventPriority,
    ) {
        eventListener = eventChannel.subscribeAlways(
            eventClass
        ) {
            if (ListenerImpl.evaluate(filter, template)) {
                run.invoke(this@BaseListenerImpl, this)
            }
        }
    }
}