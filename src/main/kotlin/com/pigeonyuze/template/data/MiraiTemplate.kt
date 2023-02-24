package com.pigeonyuze.template.data

import com.pigeonyuze.BotsTool
import com.pigeonyuze.YamlBot
import com.pigeonyuze.command.element.NullObject
import com.pigeonyuze.command.element.illegalArgument
import com.pigeonyuze.runConfigsReload
import com.pigeonyuze.template.Parameter
import com.pigeonyuze.template.Template
import com.pigeonyuze.template.TemplateImpl
import com.pigeonyuze.template.parameterOf
import com.pigeonyuze.util.FunctionArgsSize
import com.pigeonyuze.util.SerializerData
import io.github.kasukusakura.silkcodec.AudioToSilkCoder
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.file.AbsoluteFileFolder.Companion.extension
import net.mamoe.mirai.contact.file.AbsoluteFileFolder.Companion.nameWithoutExtension
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.MessageChain.Companion.serializeToJsonString
import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.Executors
import kotlin.reflect.KClass

object MiraiTemplate : Template {
    override suspend fun callValue(functionName: String, args: Parameter): Any {
        return MiraiTemplateImpl.findFunction(functionName)!!.execute(args)
    }

    override fun functionExist(functionName: String): Boolean {
        return MiraiTemplateImpl.findFunction(functionName) != null
    }

    override fun findOrNull(functionName: String): TemplateImpl<*>? {
        return MiraiTemplateImpl.findFunction(functionName)
    }

    override fun values(): List<TemplateImpl<*>> {
        return MiraiTemplateImpl.list
    }

    sealed interface MiraiTemplateImpl<K : Any> : TemplateImpl<K> {

        companion object {
            val list: List<MiraiTemplateImpl<*>> = listOf(
                UploadFunction,
                SendFunction,
                EventValueFunction,
                ReloadConfigFunction,
                DownlandFunction,
                GroupFilesFunction,
            )

            fun findFunction(functionName: String) = list.filter { it.name == functionName }.getOrNull(0)

            private fun error(name: String, args: Int): Nothing = error("Cannot find " + fun(): String {
                val usage = StringJoiner(",", "${name}(", ")")
                for (i in (0..args)) {
                    usage.add("arg${i + 1}")
                }
                return usage.toString()
            }.invoke())
        }


        override suspend fun execute(args: Parameter): K
        override val type: KClass<K>
        override val name: String

        @SerializerData(2, SerializerData.SerializerType.SUBJECT_ID)
        object UploadFunction : MiraiTemplateImpl<Message> {
            override suspend fun execute(args: Parameter): Message {
                return when (args.size) {
                    2 -> upload(args[0], args.getLong(1))
                    3 -> upload(args[0], args[1], args.getLong(2))
                    else -> error(name, args.size)
                }
            }


            override val type: KClass<Message>
                get() = Message::class
            override val name: String
                get() = "upload"

            private suspend fun upload(path: String, type: String, group: Long) : Message {
                val settingType = when (type) {
                    "image" -> Image
                    "file" -> FileMessage
                    "audio" -> Audio
                    else -> error("$type is not a valid type")
                }
                val willSendFile = File(path)
                if (!willSendFile.exists()) {
                    error("Cannot Find File $path")
                }
                willSendFile.toExternalResource().use { resource ->
                    val subject = BotsTool.getGroupOrNull(group)!!
                    return when (settingType) {
                        Image ->
                            subject.uploadImage(resource)
                        FileMessage ->
                            subject.files.uploadNewFile("/${willSendFile.name}", resource).toMessage()
                        Audio ->
                            subject.uploadAudio(
                                switchFileToSilk(resource)
                            )
                        else -> error("Cannot find type :$type")
                    }
                }
            }

            private suspend fun upload(path: String, group: Long) : Message{
                val fileType = when (path.substring(path.lastIndexOf("."))) {
                    "amr", "silk", "mp3", "wav" -> "audio"
                    "png", "jpg", "jpeg", "gif" -> "image"
                    else -> "file"
                }
                return upload(path, fileType, group)
            }

            private fun switchFileToSilk(externalResource: ExternalResource): ExternalResource {
                if (externalResource.formatName in listOf("amr", "silk")) return externalResource
                val path = "${YamlBot.dataFolderPath}/silk/${externalResource.md5}.silk"
                if (File(path).exists()) return File(path).toExternalResource()
                val threadPool = Executors.newCachedThreadPool()
                val stream = AudioToSilkCoder(threadPool)
                BufferedOutputStream(FileOutputStream(path)).use { fso ->
                    stream.connect(
                        "ffmpeg",
                        externalResource.inputStream(),
                        fso
                    )
                }
                return File(path).toExternalResource()
            }
        }

        @FunctionArgsSize([1, 2])
        @SerializerData(2, SerializerData.SerializerType.SUBJECT_ID)
        object SendFunction : MiraiTemplateImpl<Unit> {
            override suspend fun execute(args: Parameter) {
                when (args.size) {
                    2 -> send(args[0], args.getLong(1))
                    3 -> send(args[0], args[1], args.getLong(2))
                    else -> error(name, args.size)
                }
            }
            override val type: KClass<Unit>
                get() = Unit::class
            override val name: String
                get() = "send"

            private suspend fun send(path: String, type: String, group: Long) {
                val settingType = when (type) {
                    "image" -> Image
                    "file" -> FileMessage
                    "audio" -> Audio
                    else -> error("$type is not a valid type")
                }
                val message = UploadFunction.execute(parameterOf(path, type, group.toString()))
                val subject = BotsTool.getGroupOrNull(group)!!
                when (settingType) {
                    Image ->
                        message.sendTo(subject)
                    FileMessage ->
                        return //当返回fileMessage时已经被发送了
                    Audio ->
                        message.sendTo(subject)
                    else -> {}
                }
            }

            private suspend fun send(path: String, group: Long) {
                val fileType = when (path.substring(path.lastIndexOf("."))) {
                    "amr", "silk", "mp3", "wav" -> "audio"
                    "png", "jpg", "jpeg", "gif" -> "image"
                    else -> "file"
                }
                send(path, fileType, group)
            }
        }

        @SerializerData(0, SerializerData.SerializerType.MESSAGE)
        @SerializerData(1, SerializerData.SerializerType.SUBJECT_ID)
        @FunctionArgsSize([0, 1, 2])
        object DownlandFunction : MiraiTemplateImpl<String> {
            override suspend fun execute(args: Parameter): String {
                return when (args.size) {
                    2 -> save(message = args.getMessage(0), group = args.getLong(1))
                    3 -> save(message = args.getMessage(0), group = args.getLong(1), saveName = args[2])
                    4 -> save(
                        message = args.getMessage(0),
                        group = args.getLong(1),
                        saveName = args[2],
                        downlandObject = args[3]
                    )
                    else -> error(name, args.size)
                }
            }


            private suspend fun save(
                saveName: String = "${YamlBot.dataFolderPath}/$${System.currentTimeMillis()}_call_downland",
                message: Message,
                downlandObject: String = when (message) {
                    is Image -> "image"
                    is Audio -> "audio"
                    else -> "file"
                },
                group: Long,
            ): String {
                val savePath =
                    if (File(saveName).exists()) {
                        "$saveName${System.currentTimeMillis()}"
                    } else saveName
                val url = when (downlandObject) {
                    "image" -> message.toMessageChain()[Image.Key]!!.queryUrl()
                    "audio" -> (message.toMessageChain()[Audio.Key]!! as OnlineAudio).urlForDownload
                    "file" -> message.toMessageChain()[FileMessage.Key]!!.toAbsoluteFile(
                        BotsTool.getGroupOrNullJava(
                            group
                        )!!
                    )!!.getUrl()!!
                    else -> error("Cannot find $downlandObject")
                }

                return HttpTemplate.call("downland", parameterOf(url, savePath)) as String
            }


            override val type: KClass<String>
                get() = String::class
            override val name: String
                get() = "downland"
        }

        @FunctionArgsSize([1])
        @SerializerData(1, SerializerData.SerializerType.EVENT_ALL)
        object EventValueFunction : MiraiTemplateImpl<Any> {
            override suspend fun execute(args: Parameter): Any {
                if (args.size != 1) error(name, args.size)
                return value(args[0], args.getMessageEvent(1))
            }

            suspend fun value(callValue: String, event: MessageEvent): String {
                val message = event.message
                val sender = event.sender
                val subject = event.subject
                val group = if (subject is Group) subject else null
                val ret: Any = when (callValue) {
                    "message" -> message.toString()
                    "messageString" -> message.contentToString()
                    "messageMiraiCode" -> message.serializeToMiraiCode()
                    "messageJson" -> message.serializeToJsonString()
                    "time" -> event.time
                    "date" -> Date(event.time * 1000L)
                    "senderName" -> event.senderName
                    "senderId" -> sender.id
                    "senderRemark" -> sender.remark
                    "senderAvatarUrl" -> sender.avatarUrl
                    "senderAge" -> sender.queryProfile().age
                    "senderLevelQQ" -> sender.queryProfile().qLevel
                    "senderEmail" -> sender.queryProfile().email
                    "senderNick" -> sender.queryProfile().nickname
                    "senderSex" -> sender.queryProfile().sex
                    "senderSign" -> sender.queryProfile().sign
                    "subjectId" -> subject.id
                    "subjectAvatarUrl" -> subject.avatarUrl
                    "groupName" -> group!!.name
                    "groupPerm" -> group!!.botPermission.name
                    "groupOwner" -> group!!.owner
                    else -> NullObject
                }
                return ret.toString()
            }

            override val type: KClass<Any>
                get() = Any::class
            override val name: String
                get() = "value"

        }

        @SerializerData(0, SerializerData.SerializerType.SUBJECT_ID)
        @FunctionArgsSize([2, 3])
        object GroupFilesFunction : MiraiTemplateImpl<Any> {
            override suspend fun execute(args: Parameter): Any {
                val group = BotsTool.getGroupOrNull(args.getLong(0)) ?: error("Cannot find group ${args[0]}!")
                val files = group.files
                val absoluteFileFlow = files.root.files()
                val fileName = args[1]
                val call = args[2]
                val arg = args.getOrNull(3)
                return coroutineScope {
                    async {
                        val file = absoluteFileFlow.filter { it.nameWithoutExtension == fileName }.first()
                        val ret: Any = when (call) {
                            "expiryTime" -> file.expiryTime
                            "md5" -> file.md5
                            "isFile" -> file.isFile
                            "size" -> {
                                val byte = file.size
                                when (arg) {
                                    "kilobyte", "kb" -> byte / 1024
                                    "megabyte", "mb" -> byte / 1024 / 1024
                                    "gigabyte", "gb" -> byte / 1024 / 1024 / 1024
                                    "terabyte", "tb" -> byte / 1024 / 1024 / 1024 / 1024
                                    else -> byte //either or null
                                }
                            }
                            "url" -> file.getUrl() ?: error("远程文件不存在")
                            "more" -> file.moveTo(
                                files.root.resolveFolder(arg ?: illegalArgument("未提供应该提供的参数")) ?: error("找不到群文件夹： $arg")
                            )
                            "rename" -> file.renameTo(arg ?: illegalArgument("未提供应该提供的参数"))
                            "sha1" -> file.sha1
                            "path" -> file.absolutePath
                            "exists" -> file.exists()
                            "uploadTime" -> file.uploadTime
                            "delete" -> file.delete()
                            "extension" -> file.extension
                            "nameWithExtension" -> file.nameWithoutExtension
                            "name" -> file.name
                            "downland" -> HttpTemplate.call(
                                "downland",
                                parameterOf(file.getUrl() ?: error("远程文件不存在"), arg ?: illegalArgument("未提供应该提供的参数"))
                            )
                            else -> error("Cannot find function $call")
                        }
                        return@async ret
                    }
                }.await()
            }

            override val type: KClass<Any>
                get() = Any::class
            override val name: String
                get() = "file"

        }

        @FunctionArgsSize([-1])
        object ReloadConfigFunction : MiraiTemplateImpl<Unit> {
            override suspend fun execute(args: Parameter) {
                runConfigsReload()
            }

            override val type: KClass<Unit>
                get() = Unit::class
            override val name: String
                get() = "reloadConfig"

        }


    }

}

