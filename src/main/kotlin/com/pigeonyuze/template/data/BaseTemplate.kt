package com.pigeonyuze.template.data

import com.pigeonyuze.template.Parameter
import com.pigeonyuze.template.Template
import com.pigeonyuze.template.TemplateImpl
import com.pigeonyuze.util.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*
import java.util.*
import kotlin.random.Random
import kotlin.reflect.KClass

object BaseTemplate : Template {
    override suspend fun callValue(functionName: String, args: Parameter): Any {
        return BaseTemplateImpl.findFunction(functionName)!!.execute(args)
    }

    override fun functionExist(functionName: String): Boolean {
        return BaseTemplateImpl.findFunction(functionName) != null
    }

    override fun findOrNull(functionName: String): TemplateImpl<*>? {
        return BaseTemplateImpl.findFunction(functionName)
    }

    override fun values(): List<TemplateImpl<*>> {
        return BaseTemplateImpl.list
    }


    private sealed interface BaseTemplateImpl<K : Any> : TemplateImpl<K> {
        override val name: String
        override val type: KClass<K>
        override suspend fun execute(args: Parameter): K

        companion object {

            val list: List<TemplateImpl<*>> = listOf(
                RandomFunction,
                RandomText,
                CreateJsonFunction,
                ParseJsonFunction,
            )

            fun findFunction(functionName: String): TemplateImpl<*>? =
                list.filter { it.name == functionName }.getOrNull(0)

            private fun error(name: String, args: Int): Nothing = error("Cannot find " + fun(): String {
                val usage = StringJoiner(",", "${name}(", ")")
                for (i in (0..args)) {
                    usage.add("arg${i + 1}")
                }
                return usage.toString()
            }.invoke())
        }

        @FunctionArgsSize([-1])
        object RandomText : BaseTemplateImpl<String> {

            override val name: String
                get() = "randomText"
            override val type: KClass<String>
                get() = String::class

            override suspend fun execute(args: Parameter): String {
                return args.random()
            }

        }
        @FunctionArgsSize([0,1,2,3])
        object RandomFunction : BaseTemplateImpl<Int> {
            override val type: KClass<Int>
                get() = Int::class
            override val name: String
                get() = "random"

            @ArgComment(0,["由0到2147483647的随机数"])
            @ArgComment(1,["由0到参数1的随机数"])
            @ArgComment(2,["随机数的起点(包括该项)","随机数的起点(包括该项)"])
            @ArgComment(3,["随机数的起点(包括该项)","随机数的终点(包含该项)","是否包含负数(默认不包含)"])
            override suspend fun execute(args: Parameter): Int {
                return when (args.size) {
                    0 -> random()
                    1 -> random(args.getInt(0))
                    2 -> random(args.getInt(0), args.getInt(1))
                    3 -> random(args.getInt(0), args.getInt(1), args.getBoolean(2))
                    else -> error(name, args.size)
                }
            }

            private fun random(value1: Int, value2: Int, value3: Boolean = false): Int {
                val random = (value1..value2).random()
                if (value3) {
                    return if (Random.nextBoolean()) {
                        -random
                    } else random
                }
                return random
            }

            private fun random(value1: Int) = (0..value1).random()
            private fun random() = (0..Int.MAX_VALUE).random()
        }

        @FunctionArgsSize([-1])
        object ParseJsonFunction : BaseTemplateImpl<String> {
            override val name: String
                get() = "parseJson"
            override val type: KClass<String>
                get() = String::class

            override suspend fun execute(args: Parameter): String {
                val json = args.getOrNull(0) ?: error(name, args.size)
                return coroutineScope {
                    async {
                        var jsonElement = Json.parseToJsonElement(json)
                        for (arg in args.subList(1, args.lastIndex)) {
                            if (arg !is String) continue
                            jsonElement = if (arg.toIntOrNull() != null) { //is number
                                jsonElement.jsonArray.getOrNull(arg.toInt()) ?: jsonElement.jsonNull
                            } else if (jsonElement is JsonObject) {
                                jsonElement.jsonObject[arg] ?: error("Cannot find $arg in $jsonElement")
                            } else {
                                jsonElement.jsonPrimitive
                            }
                        }
                        val fieldData = jsonElement.toString()
                        return@async if (fieldData.startsWith('"') && fieldData.endsWith('"')) {
                            fieldData.dropFirstAndLast()
                        } else fieldData
                    }
                }.await()
            }

        }


        object CreateJsonFunction : BaseTemplateImpl<String> {
            override val name: String
                get() = "createJson"
            override val type: KClass<String>
                get() = String::class

            override suspend fun execute(args: Parameter): String {
                return coroutineScope {
                    async { run(args) }
                }.await()
            }

            private fun run(args: Parameter): String {
                val jsonMap = mutableMapOf<String, JsonElement>()
                val jsonArray = mutableListOf<JsonElement>()
                val isMapObject: Boolean = args[0].contains("=")
                var lastValueIsNotMap = false
                var mayKey = ""
                for (arg in args) {
                    if (isMapObject) {
                        val index = arg.indexOf("=")
                        if (index == -1) {
                            if (!lastValueIsNotMap) {
                                lastValueIsNotMap = true
                                mayKey = arg
                            } else {
                                lastValueIsNotMap = false
                                jsonMap[mayKey] = JsonPrimitive(arg)
                            }
                        }
                        val key = arg.substring(0, index)
                        val value = arg.substring(index + 1, arg.length)
                        jsonMap[key] = value.toJsonElement()
                    } else {
                        jsonArray.add(arg.toJsonElement())
                    }
                }
                return if (jsonMap.isEmpty()) JsonArray(jsonArray).toString() else JsonObject(jsonMap).toString()
            }

            private fun createJsonArray(value: String): List<JsonElement> {
                val jsonElementList = mutableListOf<JsonElement>()
                for (string in value.listToStringDataToList()) {
                    jsonElementList.add(string.toJsonElement())
                }
                return jsonElementList
            }


            private fun String.toJsonElement(): JsonElement {
                val value = this
                return if (value.matches("^\\d*\$".toRegex())) {
                    JsonPrimitive(value.toLong())
                } else if (value.matches("^(-)?\\d+(\\.\\d+)?\$".toRegex())) {
                    JsonPrimitive(value.toDouble())
                } else if (value.matches("true|false".toRegex())) {
                    JsonPrimitive(value.toBoolean())
                } else if (value.startsWith("[") && value.endsWith("]")) {
                    JsonArray(createJsonArray(value))
                } else if (value.contains("=")){
                    val jsonMap = mutableMapOf<String,JsonElement>()
                    val map = value.keyAndValueStringDataToMap(0)
                    for ((mapKey,mapValue) in map){
                        jsonMap[mapKey] = mapValue.toJsonElement()
                    }
                    JsonObject(jsonMap)
                }else JsonPrimitive(value)
            }
        }

    }


}