package com.pigeonyuze.template.data

import com.pigeonyuze.template.Parameter
import com.pigeonyuze.template.Template
import com.pigeonyuze.template.TemplateImpl
import com.pigeonyuze.template.buildTemplates
import com.pigeonyuze.util.SerializerData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.last
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.roaming.RoamingMessage
import net.mamoe.mirai.contact.roaming.RoamingMessageFilter
import net.mamoe.mirai.message.data.MessageChain
import javax.script.ScriptEngineManager

object RoamingSupportedTemplate : Template {
    override fun values(): List<TemplateImpl<*>> {
        return values
    }

    private val values = buildTemplates {
        add {
            this named "get" provided SerializerData(0, SerializerData.SerializerType.CONTACT) called {
                it.getRoamingMessage()
            }
        }

        add {
            this named "roamingMessages" provided SerializerData(0, SerializerData.SerializerType.CONTACT) called {
                roamingMessagesImpl(it)
            }
        }

        add {
            this named "getAllMessages" provided SerializerData(0, SerializerData.SerializerType.CONTACT) called {
                it.getRoamingMessage().getAllMessages(readToRoamingMessageFilter(it.getOrNull(1)))
            }
        }

        "createRoamingMessageFilter" executed {
            readToRoamingMessageFilter(it[0])
        }

        "readRoamingMessage" executed {
            readRoamingMessage(it)
        }
    }

    private suspend fun readRoamingMessage(args: Parameter): RoamingMessage {
        val flow = args.getOrNull<Flow<RoamingMessage>>(0)
            ?: throw IllegalArgumentException("Cannot find flow<RoamingMessage> in index:0,from $args !")
        val indexOrNull = args.getOrNull<Int>(1)
        var ret = flow.last()
        flow.collectIndexed { index, value ->
            if ((indexOrNull != null && index == indexOrNull)) {
                ret = value
            }
        }
        return ret
    }

    private suspend fun roamingMessagesImpl(it: Parameter): Flow<MessageChain> {
        val roamingMessages = it.getRoamingMessage()
        val timeStart = it.getLong(1)
        val timeEnd = it.getLong(2)
        val filterDescription = it.getOrNull<String>(3)
        val filter = readToRoamingMessageFilter(filterDescription)

        return roamingMessages.getMessagesIn(
            timeStart, timeEnd, filter
        )
    }

    private fun readToRoamingMessageFilter(filterDescription: String?): RoamingMessageFilter {
        var filter = RoamingMessageFilter.ANY
        when (filterDescription) {
            null, "ALL" -> {}
            "RECEIVED" -> filter = RoamingMessageFilter.RECEIVED
            "SENT" -> filter = RoamingMessageFilter.SENT
            else -> {
                filter = RoamingMessageFilter { msg ->
                    val manager = ScriptEngineManager()
                    val jsInstance = manager.getEngineByName("JavaScript")
                    val bindings = jsInstance.createBindings()
                    bindings["botId"] = msg.bot.id
                    bindings["senderId"] = msg.sender
                    bindings["contactId"] = msg.contact.id
                    bindings["ids"] = msg.ids
                    bindings["target"] = msg.target
                    bindings["time"] = msg.time
                    bindings["internalIds"] = msg.internalIds
                    jsInstance.eval(
                        filterDescription,
                        bindings
                    ).toString().toBoolean()
                }
            }

        }
        return filter
    }

    private fun Parameter.getRoamingMessage() = when (val contact = getContact(0)) {
        is Friend -> contact.roamingMessages
        is Group -> contact.roamingMessages
        else -> throw NotImplementedError("Mirai does not support getting RoamingSupported with this contact($contact), but supports getting a list of RoamingSupported: Group, Friend")
    }
}