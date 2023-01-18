package com.pigeonyuze.template

import com.pigeonyuze.command.Command.Companion.parseData
import com.pigeonyuze.command.illegalArgument
import com.pigeonyuze.util.SerializerData
import com.pigeonyuze.util.SerializerData.SerializerType.*
import com.pigeonyuze.util.keyAndValueStringDataToMap
import com.pigeonyuze.util.stringMap
import com.pigeonyuze.util.toAnyOrNull
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.Message
import net.mamoe.yamlkt.YamlList
import net.mamoe.yamlkt.YamlLiteral
import net.mamoe.yamlkt.YamlMap
import net.mamoe.yamlkt.YamlNull

class Parameter constructor() {
    private val value = mutableListOf<Any>()
    private val _stringValue = mutableListOf<String>()

    val stringValueList
        get() = _stringValue

    private constructor(value: List<Any>, _stringValue: List<String>) : this() {
        this.value.addAll(value)
        this._stringValue.addAll(_stringValue)
    }

    constructor(elements: Iterable<Any>) : this() {
        value.addAll(elements)
    }

    constructor(elements: Array<out Any>) : this() {
        value.addAll(elements)
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
    fun subList(fromIndex: Int, toIndex: Int): MutableList<Any> {
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


    fun subArgs(fromIndex: Int, toIndex: Int) = Parameter(value.subList(fromIndex, toIndex))
    fun random() = this[(0..size).random()]
    private fun errorType(index: Int): Nothing = illegalArgument("Error parameter type in $index")
    fun parseElement(templateCall: MutableMap<String, Any?>): Parameter { //不对本值进行任何修改，具有唯一性
        val ret = Parameter()
        for (arg in _stringValue) {
            val data = if (arg.contains("%call-")) parseData(arg, templateCall) else arg
            ret.value.add(data)
            ret._stringValue.add(data)
        }
        return ret
    }

    //region Get function
    fun getOrNull(index: Int): String? {
        return if (index > lastIndex) null
        else this[index]
    }

    fun getLong(index: Int): Long {
        if (value[index] is Long) return value[index] as Long
        return _stringValue[index].toLongOrNull() ?: errorType(index)
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

    fun getMessageEvent(index: Int): MessageEvent {
        val value = value[index]
        if (value is MessageEvent) return value
        errorType(index)
    }
    //endregion

    private fun setAndAddOldValue(index: Int, element: Any) {
        val oldValue = value[index]
        value[index] = element
        value.add(oldValue)
    }

    fun setValueByCommand(annotation: SerializerData, event: MessageEvent): Parameter {
        val ret = Parameter(value, _stringValue)
        val plusElement: Any = when (annotation.serializerJSONType) {
            MESSAGE -> event.message
            SUBJECT_ID -> event.subject.id
            EVENT_ALL -> event
            SENDER_NAME -> event.senderName
            SENDER_NICK -> event.sender.nick
            SENDER_ID -> event.sender.id
            COMMAND_ID -> 0
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

fun parameterOf(vararg element: String): Parameter {
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