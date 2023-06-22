package com.pigeonyuze.util

import kotlin.reflect.KClass


inline fun <reified K, reified V> Map<*, *>.mapCast(): MutableMap<K, V> {
    val map = mutableMapOf<K, V>()
    for ((key, value) in this) {
        if (key !is K || value !is V) throw ClassCastException()
        map[key] = value
    }
    return map
}

fun String.listToStringDataToList(dropStart: Int = 0): List<String> {
    val spiltAllComma: MutableList<String> =
        this.substring(dropStart, length - dropStart).replace(", ", ",").split(",".toRegex()).toMutableList() //拆分所有的','
    val spiltAllCommaTemp = mutableListOf<String>()  //contains '(' or ')' then add
    var start = -1 //-1 == not open,0 == check over,1 == wait for plus
    var tempString = ""
    for (s in spiltAllComma) {
        if (s.contains('(')) {
            start = 1
            tempString = tempString.plus(s).plus(",")
        } else if (s.contains(')')) {
            start = 0
            tempString = tempString.plus(s)
            spiltAllCommaTemp.add(tempString)
            tempString = ""
        } else if (start == 1) { //wait for
            tempString = tempString.plus(s).plus(",")
        } else spiltAllCommaTemp.add(s)
    }
    if (start == 1) { //still wait
        spiltAllCommaTemp.addAll(tempString.split(","))
    }
    return if (this.contains(",")) (spiltAllCommaTemp.ifEmpty { spiltAllComma }).dropWhile { it.isEmpty() }
    else listOf(this)
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

fun String.isNumber(): Boolean {
    for (b in this) {
        when (b.code) {
            in 0x30..0x39 -> continue //0~9
            0x2B, 0x2D, 0x66, 0x62, 0x2E -> continue //d f . - +
            else -> return false
        }
    }
    return true
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



fun Map<*,*>.stringMap() : Map<String,String> {
    val result = mutableMapOf<String,String>()
    for ((k,v) in this){
        result[k.toString()] = v.toString()
    }
    return result
}

fun List<*>.stringList() : MutableList<String>{
    val result = mutableListOf<String>()
    for (value in this) {
        result.add(value.toString())
    }
    return result
}




fun String.isLong() = this.toLongOrNull() != null

fun String.isPackageName(): Boolean {
    for (b in this) {
        if (b == '.') continue
        if (b.code in 0x61..0x7a) continue /* a ~ z */
        if (b.code in 0x41..0x5a) continue /* A ~ Z */
        return false
    }
    return true
}

val KClass<*>.isObject get() = this.objectInstance != null

fun String.isBoolean() = this.toBooleanStrictOrNull() != null

fun String.dropFirstAndLast() = this.substring(1, length - 1)

