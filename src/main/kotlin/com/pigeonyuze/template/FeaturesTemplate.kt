package com.pigeonyuze.template

import com.pigeonyuze.BotsTool
import com.pigeonyuze.com.pigeonyuze.LoggerManager
import com.pigeonyuze.command.Command.Companion.toMiraiMessage
import com.pigeonyuze.command.illegalArgument
import com.pigeonyuze.util.SerializerData
import com.pigeonyuze.util.SerializerData.SerializerType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import java.awt.Rectangle
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import javax.imageio.ImageIO
import kotlin.reflect.KClass

object FeaturesTemplate : Template {
    override suspend fun callValue(functionName: String, args: List<String>): Any {
        return FeaturesTemplateImpl.findFunction(functionName)!!.execute(args)
    }

    override fun functionExist(functionName: String): Boolean {
        return FeaturesTemplateImpl.findFunction(functionName) != null
    }

    override fun findOrNull(functionName: String): TemplateImpl<*>? {
        return FeaturesTemplateImpl.findFunction(functionName)
    }

    override fun values(): List<TemplateImpl<*>> {
        return FeaturesTemplateImpl.list
    }

    private sealed interface FeaturesTemplateImpl<K : Any> : TemplateImpl<K>{
        override val type: KClass<K>
        override val name: String
        override suspend fun execute(args: List<String>): K

        companion object{
            val list: List<FeaturesTemplateImpl<*>> = listOf(
                RandomFileFunction,
                RandomCutImageFunction,
                RepetitionFunction,
                DataFunction,
            )

            fun findFunction(functionName: String) = list.filter { it.name == functionName }.getOrNull(0)

        }

        object RandomFileFunction : FeaturesTemplateImpl<String>{
            override val type: KClass<String>
                get() = String::class
            override val name: String
                get() = "randomFile"
            override suspend fun execute(args: List<String>): String {
                val path = args[0]
                return File(path).listFiles()?.random()?.absolutePath ?: path
            }

        }

        object RandomCutImageFunction : FeaturesTemplateImpl<String>{
            override val type: KClass<String>
                get() = String::class
            override val name: String
                get() = "randomCutImage"


            override suspend fun execute(args: List<String>): String {
                return coroutineScope {
                    async {
                        run(args)
                    }
                }.await()
            }

            private fun run(args: List<String>) : String{
                val inputStream = File(args[0]).inputStream()
                val out = File(args[1]).outputStream()
                val w = args[2].toInt()
                val y = args.getOrNull(3)?.toInt()?: w
                val formatName = args.getOrNull(4) ?: args[0].substring(args[0].lastIndexOf("."))
                randomCutPNGImage(inputStream,out,w,y,formatName)
                return args[1]
            }


            private fun randomCutPNGImage(input: InputStream, out: OutputStream, objectW: Int, objectY: Int,formatName: String) {
                val imageStream = ImageIO.createImageInputStream(input)
                imageStream.use {
                    val readers = ImageIO.getImageReadersByFormatName(formatName)
                    val reader = readers.next()
                    reader.setInput(imageStream, true)
                    val param = reader.defaultReadParam
                    val random = Random()
                    val tempX = reader.getWidth(0)
                    val tempY = reader.getHeight(0)
                    val x = random.nextInt(if (tempX > objectW) tempX - objectW else tempX)
                    val y = random.nextInt(if (tempY > objectY) tempY - objectY else tempY)
                    val rect = Rectangle(x, y, objectW, objectY)
                    param.sourceRegion = rect
                    val bi = reader.read(0, param)
                    ImageIO.write(bi, formatName, out)
                }
            }

        }

        @SerializerData(0, SerializerType.SUBJECT_ID)
        @SerializerData(1,SerializerType.MESSAGE)
        object RepetitionFunction : FeaturesTemplateImpl<Unit>{
            override val type: KClass<Unit>
                get() = Unit::class
            override val name: String
                get() = "复读"

            override suspend fun execute(args: List<String>) {
                val group = args[0].toLong()
                BotsTool.getGroupOrNull(group)?.sendMessage(args[1].toMiraiMessage())
            }

        }

        @SerializerData(0,SerializerType.SENDER_ID)
        @SerializerData(1,SerializerType.COMMAND_ID)
        object DataFunction : FeaturesTemplateImpl<Any>{
            override val type: KClass<Any>
                get() = Any::class
            override val name: String
                get() = "data"

            private var hashCode_ : Int = -1

            private var saveObjectDataSize_ : Int = -1

            // read or write data
            // arg[0] : save with sender id
            // arg[1] : save with id
            // arg[2] : type(put or set or read)
            // arg[3,or more] : save data.. may is null
            override suspend fun execute(args: List<String>) : Any {
                val commandHashCode = args[1]
                val target = args[0].toLong()
                if (hashCode_ == -1) {
                    hashCode_ = commandHashCode.toInt() //下次试着改成Config设置
                }
                if (saveObjectDataSize_ == -1){
                    saveObjectDataSize_ = args.size - 3 //args包含的其他类别
                }
                val saveData = args.subList(3,args.lastIndex)
                when (args[2]) {
                    "read" -> {
                        return Save.saveData[target]?._data ?: "null"
                    }
                    "put" -> {
                        Save.saveData[target] = SaveObject(target, saveData)
                        return saveData
                    }
                    "set" -> {
                        val oldData = Save.saveData[target] ?: kotlin.run {
                            LoggerManager.loggingWarn(
                                "DataSet",
                                "No object to be modified was found in the data store of information $hashCode_,created automatically.  at target:$target"
                            )
                            Save.saveData[target] = SaveObject(target, saveData)
                            return false
                        }

                        for ((index, data) in saveData.withIndex()) {
                            if (data == "null") continue
                            oldData[index] = data
                        }
                        return true
                    }
                    "rm" -> {
                        Save.saveData.remove(target)
                        return Unit
                    }
                    else -> illegalArgument("Cannot find ${args[2]}")
                }
            }

            private object Save : AutoSavePluginConfig("SAVE_DATA_$hashCode_"){
                var saveData : MutableMap<Long,SaveObject> by value(mutableMapOf())
            }

            @kotlinx.serialization.Serializable //it maybe not good
             data class SaveObject(
                val target: Long,

                @get:JvmName("getOnlyReadData")
                val data: List<String>
            ){
                @get:JvmName("getData")
                @kotlinx.serialization.Transient
                val _data = data.toMutableList()

                operator fun set(index: Int,newValue: String) = _data.set(index,newValue)
            }

        }


    }
}