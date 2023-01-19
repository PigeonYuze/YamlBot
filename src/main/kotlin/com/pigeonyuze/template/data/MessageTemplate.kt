package com.pigeonyuze.template.data

import com.pigeonyuze.template.Parameter
import com.pigeonyuze.template.Template
import com.pigeonyuze.template.TemplateImpl
import com.pigeonyuze.util.SerializerData
import com.pigeonyuze.util.SerializerData.SerializerType
import com.pigeonyuze.util.listToStringDataToList
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.code.MiraiCode
import net.mamoe.mirai.message.data.ForwardMessage
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.buildForwardMessage
import kotlin.reflect.KClass

@ExperimentalStdlibApi
object MessageTemplate : Template {
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


    private sealed interface MessageTemplateImpl<K : Any> : TemplateImpl<K> {

        companion object {
            val list: List<MessageTemplateImpl<*>> = listOf(
                CreateForwardMessageAndSend,
                ReadForwardMessage
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
    }

}