package com.pigeonyuze.template.data

import com.pigeonyuze.command.element.NullObject
import com.pigeonyuze.template.Parameter
import com.pigeonyuze.template.Template
import com.pigeonyuze.template.TemplateImpl
import com.pigeonyuze.template.TemplateImpl.Companion.canNotFind
import com.pigeonyuze.util.SerializerData
import com.pigeonyuze.util.SerializerData.SerializerType
import com.pigeonyuze.util.listToStringDataToList
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.code.MiraiCode
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.MiraiExperimentalApi
import net.mamoe.mirai.utils.MiraiInternalApi
import java.io.File
import kotlin.reflect.KClass


object MessageTemplate : Template {

    const val IMAGE_ID_REGEX = """(\{[\da-fA-F]{8}-([\da-fA-F]{4}-){3}[\da-fA-F]{12}}\..{3,5})"""

    override suspend fun callValue(functionName: String, args: Parameter): Any {
        return findOrNull(functionName)!!.execute(args)
    }

    override fun functionExist(functionName: String): Boolean {
        return MessageTemplateImpl.findFunction(functionName) != null
    }

    override fun findOrNull(functionName: String): TemplateImpl<*>? {
        return MessageTemplateImpl.findFunction(functionName)
    }

    override fun values(): List<TemplateImpl<*>> {
        return MessageTemplateImpl.list
    }

    sealed interface MessageTemplateImpl<K : Any> : TemplateImpl<K> {

        companion object {
            val list: List<MessageTemplateImpl<*>> = listOf( //每一种信息都应该含有create和read
                CreateForwardMessageAndSend,
                ReadForwardMessage,
                CreateFlashImageAndSend,
                ReadFlashImage,
                CreateMusicShareAndSend,
                CreateFaceMessage,
                ReadFaceMessage,
                CreateDiceMessageAndSend,
                ReadDiceMessage,
                CreateLightAppAndSend,
                ReadMusicShare,
                SendRockPaperScissors,
                ReadRockPaperScissors,
                //only read:
                ReadAudio,
                ReadImage
            )

            fun findFunction(functionName: String) = list.filter { it.name == functionName }.getOrNull(0)
        }

        override val type: KClass<K>
        override val name: String
        override suspend fun execute(args: Parameter): K

        @SerializerData(0, SerializerType.EVENT_ALL)
        object CreateForwardMessageAndSend : MessageTemplateImpl<Unit> {
            override val type: KClass<Unit>
                get() = Unit::class
            override val name: String
                get() = "sendCreateForwardMessage"

            override suspend fun execute(args: Parameter) {
                val subject = args.getMessageEvent(0).subject
                val messageDslString = args.getList(1)
                val setting = if (args.lastIndex == 1) mapOf() else args.getMap(2)
                impl(subject, messageDslString, setting)
            }

            private suspend fun impl(subject: Contact, dslString: List<String>, setting: Map<String, String>) {
                subject.sendMessage(
                    settingMessage(buildForwardMessageImpl(dslString, subject), setting) //get and build message
                )
            }

            private fun buildForwardMessageImpl(
                statementMessageContent: List<String>,
                subject: Contact,
            ): ForwardMessage =
                buildForwardMessage(subject) {//dsl build
                    for (statement in statementMessageContent) {
                        val senderId: Long = statement.substringBefore(" ").toLong() //发送者id必须位于第一
                        val saysMessage: Message =
                            MiraiCode.deserializeMiraiCode(statement.substringAfterLast(" says ")) //内容必须最后
                        //中间的可选配置
                        val name: String = statement.substringAfter(" named \"", "").substringBefore("\" ", "")
                        val time: Int? = statement.substringAfter(" at \"", "").substringBefore("\" ", "").toIntOrNull()
                        if (time == null) {
                            if (name.isEmpty()) {
                                senderId says saysMessage
                                continue
                            }
                            senderId named name says saysMessage
                            continue
                        }
                        //time != null
                        if (name.isEmpty()) {
                            senderId at time says saysMessage
                            continue
                        }
                        senderId at time named name says saysMessage
                    }
                }

            private fun settingMessage(
                forwardMessage: ForwardMessage,
                setting: Map<String, String>,
            ): ForwardMessage {
                var forward = forwardMessage
                for ((name, newValue) in setting) {
                    when (name) {
                        "preview" -> {
                            forward = forward.copy(preview = newValue.listToStringDataToList(0))
                        }
                        "title" -> {
                            forward = forward.copy(title = newValue)
                        }
                        "brief" -> {
                            forward = forward.copy(brief = newValue)
                        }
                        "source" -> {
                            forward = forward.copy(source = newValue)
                        }
                        "summary" -> {
                            forward = forward.copy(summary = newValue)
                        }
                        else -> continue
                    }
                }
                return forward
            }
        }

        //要求用户自行提供信息，因为使用 [SerializerData] 得到的永远是触发的原信息
        //而原信息中不可能存在转发消息
        object ReadForwardMessage : MessageTemplateImpl<Any> {
            override val type: KClass<Any>
                get() = Any::class
            override val name: String
                get() = "readForwardMessage"

            override suspend fun execute(args: Parameter): Any {
                val message = args.getMessage(0) as ForwardMessage
                val read = args[1]
                val use = args[2]
                val indexData = args.getOrNull(3)?.toIntOrNull()
                return when (read) {
                    "config" -> readConfig(message, use)
                    else -> readData(message, indexData!!, use)
                }
            }


            private fun readData(forwardMessage: ForwardMessage, index: Int, use: String): Any {
                val node = forwardMessage.nodeList[index]
                return when (use) {
                    "message", "messageChain" -> node.messageChain
                    "time" -> node.time
                    "senderId", "id" -> node.senderId
                    "senderName", "name" -> node.senderName
                    else -> error("Cannot find $use in `ForwardMessage` message $index content.You can use: 'message','messageChain','time','senderId','id','senderName','name'")
                }
            }

            private fun readConfig(forwardMessage: ForwardMessage, use: String): Any = when (use) {
                "brief" -> forwardMessage.brief
                "summary" -> forwardMessage.summary
                "source" -> forwardMessage.source
                "title" -> forwardMessage.title
                "preview" -> forwardMessage.preview
                else -> error("Cannot find $use in `ForwardMessage` message config.You can use: 'brief','summary','source','title','preview'")
            }


        }

        @SerializerData(0, SerializerType.EVENT_ALL)
        object CreateMusicShareAndSend : MessageTemplateImpl<Unit> {
            override val type: KClass<Unit>
                get() = Unit::class
            override val name: String
                get() = "sendCreateMusicShare"

            override suspend fun execute(args: Parameter) {
                val subject = args.getMessageEvent(0).subject
                val kind = MusicKind.valueOf(args[1])
                val title = args[2]
                val summary = args[3]
                val jumpUrl = args[4]
                val pictureUrl = args[5]
                val musicUrl = args[6]
                val brief = args.getOrNull(7) ?: "[分享]$title"

                subject.sendMessage(
                    MusicShare(kind, title, summary, jumpUrl, pictureUrl, musicUrl, brief)
                )
            }
        }

        object ReadMusicShare : MessageTemplateImpl<Any> {
            override val name: String
                get() = "readMusicShare"
            override val type: KClass<Any>
                get() = Any::class

            override suspend fun execute(args: Parameter): Any {
                val message = args.getMessage(0) as MusicShare
                return when (args[1]) {
                    "kind", "musicKind" -> message.kind
                    "musicUrl", "music" -> message.musicUrl
                    "jumpUrl", "jump" -> message.jumpUrl
                    "pictureUrl", "picture" -> message.pictureUrl
                    "summary" -> message.summary
                    "brief" -> message.brief
                    "title" -> message.title
                    "content" -> message.content
                    else -> error("Cannot find ${args[1]} from MusicShare")
                }
            }
        }

        @SerializerData(0, SerializerType.EVENT_ALL)
        object CreateFlashImageAndSend : MessageTemplateImpl<Unit> {
            override val type: KClass<Unit>
                get() = Unit::class
            override val name: String
                get() = "sendCreateFlashImage"


            override suspend fun execute(args: Parameter) {
                val event = args.getMessageEvent(0)
                val pathOrMiraiCode = args[1]
                val imageId = IMAGE_ID_REGEX.toRegex().matchEntire(pathOrMiraiCode)?.groupValues?.get(1)
                val image = if (imageId == null) {
                    File(pathOrMiraiCode).toExternalResource().use {
                        event.subject.uploadImage(it)
                    }
                } else Image(imageId)
                event.subject.sendMessage(image.flash())
            }

        }

        object ReadFlashImage : MessageTemplateImpl<Any> {
            override val type: KClass<Any>
                get() = Any::class
            override val name: String
                get() = "readFlashImage"

            override suspend fun execute(args: Parameter): Any {
                val flashImage = args.getMessage(0) as FlashImage

                return when (args[1]) {
                    "content" -> flashImage.content
                    "image" -> flashImage.image
                    "miraiCode" -> flashImage.serializeToMiraiCode()
                    "imageId" -> flashImage.image.imageId
                    "imageType" -> flashImage.image.imageType
                    "size" -> flashImage.image.size
                    "height" -> flashImage.image.height
                    "width" -> flashImage.image.width
                    "md5" -> flashImage.image.md5
                    "queryUrl" -> flashImage.image.queryUrl()
                    else -> error("Cannot get ${args[1]} from flashImage")
                }
            }

        }

        @SerializerData(0, SerializerType.EVENT_ALL)
        object CreateFaceMessage : MessageTemplateImpl<Face> {
            override val type: KClass<Face>
                get() = Face::class
            override val name: String
                get() = "createFaceMessage"

            override suspend fun execute(args: Parameter): Face {
                val idOrName = args[0]
                val id = idOrName.toIntOrNull()
                    ?: if (idOrName.startsWith("[") && idOrName.endsWith("]")) Face.names.indexOf(idOrName)
                    else Face.names.indexOf("[$idOrName]")
                return Face(id)
            }
        }

        object ReadFaceMessage : MessageTemplateImpl<Any> {
            override val type: KClass<Any>
                get() = Any::class
            override val name: String
                get() = "readFaceMessage"

            override suspend fun execute(args: Parameter): Any {
                val message = args.getMessage(0) as Face
                return when (args[1]) {
                    "name" -> message.name
                    "id" -> message.id
                    "content" -> message.content
                    else -> error("Cannot find ${args[1]} from FaceMessage")
                }
            }
        }

        @SerializerData(0, SerializerType.EVENT_ALL)
        object CreateDiceMessageAndSend : MessageTemplateImpl<Unit> {
            override val type: KClass<Unit>
                get() = Unit::class
            override val name: String
                get() = "sendDice"

            override suspend fun execute(args: Parameter) {
                val event = args.getMessageEvent(0)
                val value = args.getOrNull(1)?.toIntOrNull() ?: (1..6).random()
                event.subject.sendMessage(
                    Dice(value)
                )
            }
        }

        object ReadDiceMessage : MessageTemplateImpl<Any> {
            override val type: KClass<Any>
                get() = Any::class
            override val name: String
                get() = "readDice"

            override suspend fun execute(args: Parameter): Any {
                val message = args.getMessage(0) as Dice
                return when (args[1]) {
                    "value" -> message.value
                    "name" -> message.name
                    "content" -> message.content
                    else -> error("Cannot find ${args[1]} from Dice")
                }
            }
        }

        @SerializerData(0, SerializerType.EVENT_ALL)
        object CreateLightAppAndSend : MessageTemplateImpl<Unit> {
            override val type: KClass<Unit>
                get() = Unit::class
            override val name: String
                get() = "sendCreateLightApp"

            override suspend fun execute(args: Parameter) {
                val subject = args.getMessageEvent(0).subject
                val content = args[1]
                subject.sendMessage(LightApp(content))
            }
        }

        @SerializerData(0, SerializerType.CONTACT)
        object SendRockPaperScissors : MessageTemplateImpl<Unit> {
            override val name: String
                get() = "sendRockPaperScissors"
            override val type: KClass<Unit>
                get() = Unit::class

            override suspend fun execute(args: Parameter) {
                val contact = args.getOrNull<Contact>(0) ?: throw IllegalArgumentException()
                val msg = args.getOrNull(1)?.run { RockPaperScissors.valueOf(this) } ?: RockPaperScissors.random()
                contact.sendMessage(msg)
            }
        }

        object ReadRockPaperScissors : MessageTemplateImpl<Any> {
            override val name: String
                get() = "readRockPaperScissors"
            override val type: KClass<Any>
                get() = Any::class

            @OptIn(MiraiExperimentalApi::class, MiraiInternalApi::class)
            override suspend fun execute(args: Parameter): Any {
                val msg = args.getOrNull<RockPaperScissors>(0) ?: throw IllegalArgumentException()
                return when (args[1]) {
                    "content" -> msg.content
                    "name" -> msg.name
                    "id" -> msg.id
                    "internalId" -> msg.internalId
                    "eliminates" -> {
                        val otherRockPaper = args.getOrNull<RockPaperScissors>(2)
                            ?: throw IllegalArgumentException("Getter `eliminates` needs other RockPaperScissors obj")
                        (msg eliminates otherRockPaper) ?: NullObject
                    }
                    else -> canNotFind(args[1], "RockPaperScissors")
                }
            }
        }

        //region 已经在 [MiraiTemplate] 中定义了上传与发送 只需要解析的信息类型
        object ReadImage : MessageTemplateImpl<Any> {
            override val type: KClass<Any>
                get() = Any::class
            override val name: String
                get() = "readImage"

            override suspend fun execute(args: Parameter): Any {
                val image = args.getMessage(0) as Image
                return when (args[1]) {
                    "content" -> image.content
                    "miraiCode" -> image.serializeToMiraiCode()
                    "imageId" -> image.imageId
                    "imageType" -> image.imageType
                    "size" -> image.size
                    "height" -> image.height
                    "width" -> image.width
                    "md5" -> image.md5
                    "queryUrl" -> image.queryUrl()
                    else -> error("Cannot get ${args[1]} from Image")
                }
            }

        }

        object ReadAudio : MessageTemplateImpl<Any> {
            override val type: KClass<Any>
                get() = Any::class
            override val name: String
                get() = "readAudio"

            override suspend fun execute(args: Parameter): Any {
                val audio = args.getMessage(0) as OnlineAudio
                return when (args[1]) {
                    "codec" -> audio.codec
                    "content" -> audio.content
                    "filename" -> audio.filename
                    "md5" -> audio.fileMd5
                    "size" -> audio.fileSize
                    "length" -> audio.length
                    "downloadUrl" -> audio.urlForDownload
                    else -> error("Cannot get ${args[1]} from Audio")
                }
            }
        }
        //endregion

    }

}