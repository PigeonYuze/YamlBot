package com.pigeonyuze.util

import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageOrigin
import net.mamoe.mirai.message.data.MessageSource
import net.mamoe.mirai.message.data.SingleMessage
import net.mamoe.mirai.utils.MiraiExperimentalApi



fun String.listToStringDataToList(dropStart: Int = 1): List<String> {
    val spiltAllComma: MutableList<String> = this.substring(dropStart,length-1).replace(", ", ",")
        .split(",".toRegex()).toMutableList() //拆分所有的','
    val spiltAllCommaTemp = mutableListOf<String>()
    var start = -1
    var tempString = ""
    for ((i, s) in spiltAllComma.withIndex()) {
        if (s.contains('(')) {
            start = i
            tempString = tempString.plus(s).plus(",")
        } else if (s.contains(')')) {
            start = 0
            tempString = tempString.plus(s)
            spiltAllCommaTemp.add(tempString)
            tempString = ""
        } else if (start != -1) {
            tempString = tempString.plus(s).plus(",")
        } else spiltAllCommaTemp.add(s)
    }
    return if (this.contains(",")) (spiltAllCommaTemp.ifEmpty { spiltAllComma }).dropLastWhile { it.isEmpty() } else listOf(
        this
    )
}

fun List<String>.makeStringToAny() : List<Any>{
    val ret = mutableListOf<Any>()
    for (element in this) {
       ret.add(element.toAny())
    }
    return ret
}

fun Map<String,String>.makeStringToAny() : Map<Any,Any>{
    val ret = mutableMapOf<Any,Any>()
    for ((key, value) in this) {
        ret[key.toAny()] = value.toAny()
    }
    return ret
}

private fun String.toAny(): Any {
    return if (this.matches("^(-)?\\d*\$".toRegex())) {
        (this.toIntOrNull() ?: this.toLong())
    } else if (this.matches("^(-)?\\d+(\\.\\d+)?\$".toRegex())) {
        (this.toDouble())
    } else if (this.matches("true|false".toRegex())) {
        (this.toBoolean())
    } else if (this.startsWith("[") && this.endsWith("]")) {
        (this.listToStringDataToList().makeStringToAny())
    } else if (this.contains("=")) {
        val map = this.keyAndValueStringDataToMap()
        (map)
    } else (this)
}


fun String.keyAndValueStringDataToMap(drop: Int = 0): Map<String, String> {
    val ret = mutableMapOf<String, String>()
    val list =
        if (this.contains(",")) this.substring(drop, (if (drop == 0) length else length - 1)).replace(" ", "")
            .split(",".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray().toList() else listOf(this)
    for (str in list) {
        val index = str.indexOf("=")
        val valueStartIndex = if (str[index + 1] == '=') index + 2 else index + 1
        ret[str.substring(0, index)] = str.substring(valueStartIndex, str.length)
    }
    return ret
}

@OptIn(MiraiExperimentalApi::class)
inline fun <reified M : SingleMessage> MessageChain.onlyElement(): Boolean {
    this.forEach {
        if (it !is M && it !is MessageSource && it !is MessageOrigin) return false
    }
    return true
}
//fun YamlElement.toAnyOrNull() : Any? =
//    when(this){
//        is YamlPrimitive -> this.content
//        is YamlMap -> {
//            val anyMap = mutableMapOf<Any,Any?>()
//            for ((key,value) in this.content){
//                anyMap[key.toAnyOrNull()!!] = value.toAnyOrNull()
//            }
//            anyMap
//        }
//        is YamlList -> {
//            val anyList = mutableListOf<Any?>()
//            for (value in this.content){
//                anyList.add(value.toAnyOrNull())
//            }
//            anyList
//        }
//    }anyList

fun String.isLong() = this.toLongOrNull() != null

fun String.isBoolean() = this.toBooleanStrictOrNull() != null

fun String.dropFirstAndLast() = this.substring(1, length - 1)

