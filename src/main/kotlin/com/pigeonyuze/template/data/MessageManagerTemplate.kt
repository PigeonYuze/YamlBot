package com.pigeonyuze.template.data

import com.pigeonyuze.BotsTool
import com.pigeonyuze.template.Parameter
import com.pigeonyuze.template.Template
import com.pigeonyuze.template.TemplateImpl
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.getMember
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.PlainText
import kotlin.reflect.KClass

object MessageManagerTemplate : Template {
    override fun values(): List<TemplateImpl<*>> {
        return MessageManagerTemplateImpl.list
    }

    override suspend fun callValue(functionName: String, args: Parameter): Any {
        return findOrNull(functionName)!!.execute(args)
    }

    override fun findOrNull(functionName: String): TemplateImpl<*>? {
        return MessageManagerTemplateImpl.findTemplateOrNull(functionName)
    }

    override fun functionExist(functionName: String): Boolean {
        return findOrNull(functionName) != null
    }

    sealed interface MessageManagerTemplateImpl<K : Any> : TemplateImpl<K> {
        override suspend fun execute(args: Parameter): K

        companion object {
            val list = listOf<TemplateImpl<*>>(
                SendMessageToGroup,
                SendMessageToFriend,
                SendMessageToAllFriends,
                SendMessageToAllGroups,
                NudgeFriend,
                NudgeGroupMember
            )

            fun findTemplateOrNull(name: String) = list.firstOrNull { it.name == name }
        }

        object SendMessageToGroup : MessageManagerTemplateImpl<Unit> {
            override val name: String
                get() = "sendMessageToGroup"

            override val type: KClass<out Unit>
                get() = Unit::class

            override suspend fun execute(args: Parameter) {
                args.read {
                    val groupIdOrGroup = next()
                    var groupId = -1L
                    val messageOrPlainText = next()
                    val group = if ((groupIdOrGroup is String && groupIdOrGroup.toLongOrNull()
                            ?.also { groupId = it } != null) || (groupIdOrGroup as? Long)?.also { groupId = it } != null
                    ) {
                        BotsTool.getGroupOrNull(groupId)!!
                    } else groupIdOrGroup as Group

                    if (messageOrPlainText is Message) {
                        group.sendMessage(messageOrPlainText)
                    } else group.sendMessage(PlainText(messageOrPlainText.toString()))
                }

            }
        }

        object SendMessageToFriend : MessageManagerTemplateImpl<Unit> {
            override val name: String
                get() = "sendMessageToFriend"

            override val type: KClass<out Unit>
                get() = Unit::class

            override suspend fun execute(args: Parameter) {
                args.read {
                    val friendIdOrFriend = next()
                    val messageOrPlainText = next()
                    var friendId = -1L
                    val friend = if ((friendIdOrFriend is String && friendIdOrFriend.toLongOrNull()
                            ?.also { friendId = it } != null) || (friendIdOrFriend as? Long)?.also {
                            friendId = it
                        } != null
                    ) {
                        BotsTool.getFriendOrNull(friendId)
                    } else friendIdOrFriend as Friend

                    if (messageOrPlainText is Message) {
                        friend?.sendMessage(messageOrPlainText)
                    } else friend?.sendMessage(PlainText(messageOrPlainText.toString()))
                }
            }
        }

        object SendMessageToAllGroups : MessageManagerTemplateImpl<Int> {
            override val name: String
                get() = "sendMessageToAllGroups"

            override val type: KClass<Int>
                get() = Int::class

            override suspend fun execute(args: Parameter): Int {
                val groups = BotsTool.getAllGroup()
                var done = 0
                args.read {
                    val messageOrPlainText = next()
                    if (messageOrPlainText is Message) {
                        groups.forEach {
                            kotlin.runCatching {
                                it.sendMessage(messageOrPlainText)
                                done++
                            }
                        }
                    } else groups.forEach {
                        kotlin.runCatching {
                            it.sendMessage(PlainText(messageOrPlainText.toString()))
                            done++
                        }
                    }
                }
                return done
            }
        }

        object SendMessageToAllFriends : MessageManagerTemplateImpl<Int> {
            override val name: String
                get() = "sendMessageToAllFriends"

            override val type: KClass<Int>
                get() = Int::class

            override suspend fun execute(args: Parameter): Int {
                val friends = BotsTool.getAllFriend()
                var done = 0
                args.read {
                    val messageOrPlainText = next()
                    if (messageOrPlainText is Message) {
                        friends.forEach {
                            kotlin.runCatching {
                                it.sendMessage(messageOrPlainText)
                                done++
                            }
                        }
                    } else friends.forEach {
                        kotlin.runCatching {
                            it.sendMessage(PlainText(messageOrPlainText.toString()))
                            done++
                        }
                    }
                }
                return done
            }

        }

        object NudgeGroupMember : MessageManagerTemplateImpl<Unit> {
            override val name: String
                get() = "nudgeGroupMember"
            override val type: KClass<out Unit>
                get() = Unit::class

            override suspend fun execute(args: Parameter) {
                args.read {
                    val groupId = long()
                    val memberId = long(1)
                    BotsTool.getGroupOrNull(groupId)?.getMember(memberId)?.nudge()
                }
            }
        }

        object NudgeFriend : MessageManagerTemplateImpl<Unit> {
            override val name: String
                get() = "nudgeFriend"
            override val type: KClass<out Unit>
                get() = Unit::class

            override suspend fun execute(args: Parameter) {
                args.read {
                    val friendId = long()
                    BotsTool.getFriendOrNull(friendId)?.nudge()
                }
            }
        }
    }
}