package com.pigeonyuze.template

import com.pigeonyuze.LoggerManager
import com.pigeonyuze.command.Command
import com.pigeonyuze.command.element.illegalArgument
import com.pigeonyuze.util.*
import com.pigeonyuze.util.SerializerData.SerializerType.*
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.events.FriendEvent
import net.mamoe.mirai.event.events.GroupEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.Message
import net.mamoe.yamlkt.YamlList
import net.mamoe.yamlkt.YamlLiteral
import net.mamoe.yamlkt.YamlMap
import net.mamoe.yamlkt.YamlNull

@Suppress("UNUSED")
class Parameter constructor() {
    val value = mutableListOf<Any>()
    private val _stringValue = mutableListOf<String>()

    val stringValueList
        get() = _stringValue

    private constructor(value: List<Any>, _stringValue: List<String>) : this() {
        this.value.addAll(value)
        this._stringValue.addAll(_stringValue)
    }

    constructor(elements: Iterable<Any>) : this() {
        for (any in elements) {
            value.add(any)
            _stringValue.add(any.toString())
        }
    }

    constructor(elements: Array<out Any>) : this() {
        for (any in elements) {
            value.add(any)
            _stringValue.add(any.toString())
        }
    }

    val size
        get() = value.size

    //region Add function
    fun add(yamlMap: YamlMap) {
        value.add(yamlMap.toAnyOrNull() as Map<*, *>)
        _stringValue.add(yamlMap.toString())
    }

    fun add(message: Message) {
        value.add(message)
        _stringValue.add(message.contentToString())
    }

    fun add(yamlList: YamlList) {
        value.add(yamlList.toAnyOrNull() as List<*>)
        _stringValue.add(yamlList.toString())
    }

    fun add(yamlLiteral: YamlLiteral) {
        value.add(yamlLiteral.content)
        _stringValue.add(yamlLiteral.content)
    }

    fun add(string: String) {
        value.add(string)
        _stringValue.add(string)
    }
    //endregion

    private val itr: MutableIterator<Any>
        get() = value.iterator()

    private var nextValue: Any? = null

    val lastIndex
        get() = value.lastIndex

    operator fun hasNext(): Boolean {
        while (null == nextValue && itr.hasNext()) {
            nextValue = itr.next()
        }
        return null != nextValue
    }

    operator fun next(): Any? {
        if (hasNext()) {
            val value = nextValue
            nextValue = null
            return value
        }
        throw NoSuchElementException()
    }

    operator fun iterator() = _stringValue.iterator()
    private operator fun set(index: Int, newValue: Any) = value.set(index, newValue)
    fun withIndex() = value.withIndex()
    operator fun get(index: Int) = _stringValue[index]
    fun subList(fromIndex: Int, toIndex: Int = lastIndex): MutableList<Any> {
        if (fromIndex < 0) throw IndexOutOfBoundsException("fromIndex = $fromIndex")
        if (toIndex > size) throw IndexOutOfBoundsException("toIndex = $toIndex")
        require(fromIndex <= toIndex) {
            "fromIndex($fromIndex) > toIndex($toIndex)"
        }
        val list = mutableListOf<Any>()
        for (index in (fromIndex..toIndex)) {
            list.add(value[index])
        }
        return list
    }


    fun subArgs(fromIndex: Int, toIndex: Int = value.lastIndex) = Parameter(value.subList(fromIndex, toIndex))
    fun random() = this[(0..size).random()]
    fun errorType(index: Int): Nothing = illegalArgument("Error parameter type in $index of Parameter$this")
    fun parseElement(templateCall: MutableMap<String, Any?>): Parameter { //不对本值进行任何修改，具有唯一性
        val ret = Parameter() //new bug... wait for me,pls!
        for (arg in value) {
            var data = arg
            runIfIsInstance<String>(arg) {
                if (startsWith("%call-") && endsWith("%")) {
                    LoggerManager.loggingTrace("Parameter-parseElement", "Find `call` evaluate,start parse element.")
                    data = templateCall[drop(6).dropLast(1)]
                        ?: throw IllegalArgumentException("Cannot find value --> $arg from $templateCall")
                    return@runIfIsInstance
                }
                if (!contains("%call-")) {
                    return@runIfIsInstance
                }
                data = Command.parseData(this, templateCall) //may not good
            }
            ret.value.add(data)
            ret._stringValue.add(data.toString())
        }
        LoggerManager.loggingDebug("Parameter-parseElement", "Done. -> ${ret.debugToString()}")
        return ret
    }

    private inline fun <reified K> runIfIsInstance(obj: Any, run: K.() -> Unit) {
        if (obj !is K) {
            return
        }
        run.invoke(obj)
    }

    //region Get function
    fun getOrNull(index: Int): String? {
        return if (index > lastIndex) null
        else this[index]
    }

    internal fun debugToString() = value.joinToString {
        "$it: ${it::class.simpleName}"
    }

    fun getLong(index: Int): Long {
        if (value[index] is Long) return value[index] as Long
        return _stringValue[index].toLongOrNull() ?: errorType(index)
    }

    fun getContact(index: Int): Contact {
        val value = value[index]
        if (value is Contact) return value
        errorType(index)
    }

    fun getInt(index: Int): Int {
        return _stringValue[index].toIntOrNull() ?: errorType(index)
    }

    fun getBoolean(index: Int): Boolean {
        return _stringValue[index].toBooleanStrictOrNull() ?: errorType(index)
    }

    fun getMap(index: Int): Map<String, String> {
        val value = value[index]
        if (value is Map<*, *>) return value.stringMap()
        if (value is String) return value.keyAndValueStringDataToMap()
        errorType(index)
    }


    fun getMessage(index: Int): Message {
        val value = value[index]
        if (value is Message) return value
        errorType(index)
    }

    fun getList(index: Int): List<String> {
        val value = value[index]
        if (value is List<*>) return value.stringList()
        if (value is String) return value.listToStringDataToList()
        errorType(index)
    }

    fun getMessageEvent(index: Int): MessageEvent {
        val value = value[index]
        if (value is MessageEvent) return value
        errorType(index)
    }

    inline fun <reified K> getOrNull(index: Int): K? {
        if (index > this.lastIndex) return null
        return value[index] as? K
    }

    @Suppress("UNCHECKED_CAST")
    fun <K : Event> getEventAndDrop(): K {
        for ((index, any) in value.withIndex()) {
            val event = any as? K ?: continue
            value.removeAt(index)
            return event
        }
        throw NoSuchElementException("No element with event was found in the parameter list")
    }
    //endregion

    private fun setAndAddOldValue(index: Int, element: Any) {
        val oldValue = value[index]
        value[index] = element
        if (value.lastIndex >= index + 1) {
            value[index + 1] = oldValue
        } else value.add(oldValue)
    }

    suspend fun <T> read(run: suspend ParameterValueReader.() -> T): T {
        val reader = ParameterValueReader()
        return run.invoke(reader)
    }


    fun setValueByCommand(annotation: SerializerData, event: Event): Parameter {
        val ret = Parameter(value, _stringValue)
        val plusElement: Any = when (annotation.serializerJSONType) {
            MESSAGE -> if (event !is MessageEvent && annotation.isByNullWhenEvent) NullObject else (event as MessageEvent).message
            SUBJECT_ID -> if (event !is MessageEvent && annotation.isByNullWhenEvent) NullObject else (event as MessageEvent).message
            EVENT_ALL -> event
            SENDER_NAME -> if (event !is MessageEvent && annotation.isByNullWhenEvent) NullObject else (event as MessageEvent).message
            SENDER_NICK -> if (event !is MessageEvent && annotation.isByNullWhenEvent) NullObject else (event as MessageEvent).message
            SENDER_ID -> if (event !is MessageEvent && annotation.isByNullWhenEvent) NullObject else (event as MessageEvent).message
            CONTACT -> ((event as? GroupEvent)?.group ?: (event as? FriendEvent)?.friend
            ?: (event as? MessageEvent)?.subject)
                ?: if (annotation.isByNullWhenEvent) NullObject else throw NullPointerException("Cannot read event")//仅有这些支持联系人的查询
        }

        if (lastIndex >= (annotation.buildIndex) && annotation.buildIndex != -1) {
            ret.setAndAddOldValue(annotation.buildIndex, plusElement)
        } else {
            ret.value.add(plusElement)
        }
        return ret
    }

    override fun equals(other: Any?): Boolean {
        return value == other
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return value.toString()
    }

    inner class ParameterValueReader {
        private val value: List<Any> = this@Parameter.value

        operator fun get(index: Int): String {
            readIndex = index
            return value[index].toString()
        }

        var readIndex: Int = -1

        val nonNegativeIndex = if (readIndex < 0) 0 else readIndex

        fun hasNext() = nonNegativeIndex < value.lastIndex

        @DslParameterReader
        suspend infix fun <K> Int.read(run: suspend Any.() -> K): K {
            this@ParameterValueReader.readIndex = this

            return run(this@ParameterValueReader.value[readIndex])
        }

        @DslParameterReader
        infix fun <K> Int.long(run: Long.() -> K): K {
            this@ParameterValueReader.readIndex = this

            val runValue = if (value[this] is Long) value[this] as Long
            else _stringValue[this].toLongOrNull() ?: errorType(this)
            return run.invoke(runValue)
        }

        fun long(index: Int = nonNegativeIndex): Long {
            this@ParameterValueReader.readIndex = index

            return if (value[index] is Long) value[index] as Long
            else _stringValue[index].toLongOrNull() ?: errorType(index)
        }

        infix fun longOrNull(index: Int): Long? {
            this@ParameterValueReader.readIndex = index

            return if (value[index] is Long) value[index] as Long
            else _stringValue[index].toLongOrNull()
        }

        @DslParameterReader
        infix fun <K> Int.int(run: Int.() -> K): K {
            this@ParameterValueReader.readIndex = this
            return run(
                _stringValue[this].toIntOrNull() ?: errorType(this)
            )
        }

        @DslParameterReader
        infix fun <K> Int.boolean(run: Boolean.() -> K): K {
            this@ParameterValueReader.readIndex = this
            return run(
                _stringValue[this].toBooleanStrictOrNull() ?: errorType(this)
            )
        }

        fun bool(index: Int): Boolean {
            this@ParameterValueReader.readIndex = index
            return _stringValue[index].toBooleanStrictOrNull() ?: errorType(index)
        }

        @DslParameterReader
        infix fun <K> Int.map(run: Map<String, String>.() -> K): K {
            this@ParameterValueReader.readIndex = this
            val value = value[this]
            return run(
                when (value) {
                    is Map<*, *> -> value.stringMap()
                    is String -> value.keyAndValueStringDataToMap()
                    else -> errorType(this)
                }
            )
        }

        fun map(index: Int = nonNegativeIndex): Map<out Any?, Any?> {
            this@ParameterValueReader.readIndex = index
            return when (val value = value[index]) {
                is Map<*, *> -> value
                is String -> value.keyAndValueStringDataToMap().makeStringToAny()
                else -> errorType(index)
            }
        }

        @DslParameterReader
        infix fun <K> Int.message(run: Message.() -> K): K {
            this@ParameterValueReader.readIndex = this
            val value = value[this]
            return run.invoke(
                if (value is Message) value
                else errorType(this)
            )
        }

        @DslParameterReader
        infix fun <K> Int.list(run: List<String>.() -> K): K {
            this@ParameterValueReader.readIndex = this
            val value = value[this]
            return run.invoke(
                when (value) {
                    is List<*> -> value.stringList()
                    is String -> value.listToStringDataToList()
                    else -> errorType(this)
                }
            )
        }

        fun list(index: Int = nonNegativeIndex): List<*> {
            this@ParameterValueReader.readIndex = index
            return when (val value = value[index]) {
                is List<*> -> value
                is String -> value.listToStringDataToList()
                else -> errorType(index)
            }
        }

        fun listOrNull(index: Int = readIndex): List<*>? {
            if (index > value.lastIndex) {
                return null
            }
            return list(index)
        }

        @DslParameterReader
        infix fun <K> Int.messageEvent(run: MessageEvent.() -> K): K {
            this@ParameterValueReader.readIndex = this
            val value = value[this]
            return run.invoke(
                if (value is MessageEvent) value
                else errorType(this)
            )
        }

        fun int(index: Int): Int {
            this@ParameterValueReader.readIndex = index
            return _stringValue[index].toIntOrNull() ?: errorType(index)
        }

        fun intOrNull(index: Int = nonNegativeIndex): Int? {
            this@ParameterValueReader.readIndex = index
            return _stringValue[index].toIntOrNull()
        }

        fun next(): Any {
            readIndex++

            return value[readIndex]
        }

        fun nextOrNull(): Any? {
            readIndex++
            return value.getOrNull(readIndex)
        }

        override fun toString(): String {
            return "ParameterValueReader($value, readIndex=$readIndex)"
        }
    }

    companion object {


        fun Parameter.addAny(any: Any) {
            this.value.add(any)
        }

        fun Parameter.removeFirst(): Parameter {
            this.value.removeFirst()
            if (_stringValue.isNotEmpty()) this._stringValue.removeFirst()
            return this
        }
    }
}


fun parameterOf(vararg element: Any): Parameter {
    return Parameter(element)
}

fun YamlList.asParameter(): Parameter {
    val parameter = Parameter()
    for (element in this) {
        when (element) {
            is YamlMap -> parameter.add(element)
            is YamlLiteral -> parameter.add(element)
            YamlNull -> parameter.add("null")
            is YamlList -> parameter.add(element)
        }
    }
    return parameter
}

fun List<String>.asParameter(): Parameter {
    return Parameter(this)
}

