package com.pigeonyuze.template

import com.pigeonyuze.YamlBot
import com.pigeonyuze.com.pigeonyuze.LoggerManager
import com.pigeonyuze.util.FunctionArgsSize
import com.pigeonyuze.util.dropFirstAndLast
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.*
import kotlin.reflect.KClass

object HttpTemplate : Template {
    override suspend fun callValue(functionName: String, args: Parameter): Any {
        return HttpTemplateImpl.findFunction(functionName)!!.execute(args)
    }

    override fun functionExist(functionName: String): Boolean {
        return HttpTemplateImpl.findFunction(functionName) != null
    }

    override fun findOrNull(functionName: String): TemplateImpl<*>? {
        return HttpTemplateImpl.findFunction(functionName)
    }

    override fun values(): List<TemplateImpl<*>> {
        return HttpTemplateImpl.list
    }

    private sealed interface HttpTemplateImpl<K: Any> : TemplateImpl<K>{
        override val name: String
        override val type: KClass<K>
        override suspend fun execute(args: Parameter): K
        companion object {
            val list: List<HttpTemplateImpl<*>> = listOf(
                DownlandFunction,
                ApiFieldFunction,
                ApiContentFunction,
            )

            fun findFunction(functionName: String) = list.filter { it.name == functionName }.getOrNull(0)

            private fun error(name: String, args: Int): Nothing = error("Cannot find " + fun(): String {
                val usage = StringJoiner(",", "${name}(", ")")
                for (i in (0..args)) {
                    usage.add("arg${i + 1}")
                }
                return usage.toString()
            }.invoke())

            private fun httpRequest(
                http: String,
                param: Map<String, String>?,
            ): Response {
                LoggerManager.loggingTrace("Template-http","Request call $http,param: ${param.toString()}")
                var httpUrl = http
                if (param != null) {
                    val stringJoiner = StringJoiner("&", "$http?", "")
                    for ((key, value) in param) {
                        stringJoiner.add("${URLEncoder.encode(key, "utf-8")}=${URLEncoder.encode(value, "utf-8")}")
                    }
                    httpUrl = stringJoiner.toString()
                }
                val httpClient = OkHttpClient()
                val requestBody = Request.Builder()
                    .get()
                    .url(httpUrl)
                    .build()
                return httpClient.newCall(requestBody).execute()
            }

        }

        @FunctionArgsSize([1,2,3])
        object DownlandFunction : HttpTemplateImpl<String>{
            override val name: String
                get() = "downland"
            override val type: KClass<String>
                get() = String::class

            override suspend fun execute(args: Parameter): String {
                return when(args.size) {
                    1 -> downland(args[0])
                    2 -> downland(args[0],args[1])
                    3 -> downland(args[0],args[1],args.getMap(2))
                    else -> error(name,args.size)
                }
            }

            private fun downland(http: String): String{
                return downland(http, "${YamlBot.dataFolderPath}/$http")
            }


            private fun downland(http: String, saveName: String, param: Map<String,String>? = null): String{
                LoggerManager.loggingDebug("HttpTemplate-$name","Downland $http(param: ${param.toString()}) to $saveName")
                httpRequest(http, param).body!!.byteStream().use {
                    val fileOutputStream = FileOutputStream(saveName)
                    fileOutputStream.write(it.readAllBytes())
                    fileOutputStream.flush()
                    fileOutputStream.close()
                }
                return saveName
            }


        }

        @FunctionArgsSize([1,2])
        object ApiContentFunction  : HttpTemplateImpl<String>{
            override val name: String
                get() = "content"
            override val type: KClass<String>
                get() = String::class

            override suspend fun execute(args: Parameter): String {
                println(args.size)
                return when(args.size){
                    1 -> json(args[0])
                    2 -> json(args[0],args.getMap(1))
                    else -> error(name,args.size)
                }
            }

            private fun json(http: String,param: Map<String, String>? = null) = httpRequest(http,param).body!!.string()

        }
        @FunctionArgsSize([2,3])
        object ApiFieldFunction : HttpTemplateImpl<String>{
            override val name: String
                get() = "field"
            override val type: KClass<String>
                get() = String::class

            override suspend fun execute(args: Parameter): String {
                val fieldName: String

                val jsonString = when (args.size) {
                    2 -> {
                        fieldName = args[1]
                        ApiContentFunction.execute(parameterOf(args[0]))
                    }
                    3 -> {
                        fieldName = args[2]
                        ApiContentFunction.execute(args.subArgs(0, 1))
                    }
                    else -> {
                        error(name, args.size)
                    }
                }
                val jsonElement = Json.parseToJsonElement(jsonString)
                val fieldData =
                    if (jsonString.startsWith("[") && jsonString.endsWith("]")) jsonElement.jsonArray[fieldName.toInt()].toString()
                    else jsonElement.jsonObject[fieldName].toString()

                return if (fieldData.startsWith('"') && fieldData.endsWith('"')) {
                    fieldData.dropFirstAndLast()
                } else fieldData
            }
        }


    }
}