package com.pigeonyuze.template.data

import com.pigeonyuze.template.Parameter
import com.pigeonyuze.template.Template
import com.pigeonyuze.template.TemplateImpl
import com.pigeonyuze.util.SerializerData
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.announcement.*
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.io.File
import kotlin.reflect.KClass


object GroupAnnouncementsTemplate : Template {
    override fun values(): List<TemplateImpl<*>> {
        return GroupAnnouncementsTemplateImpl.list
    }

    override suspend fun callValue(functionName: String, args: Parameter): Any {
        return findOrNull(functionName)!!.execute(args)
    }

    override fun findOrNull(functionName: String): TemplateImpl<*>? {
        return GroupAnnouncementsTemplateImpl.list.filter { it.name == functionName }.getOrNull(0)
    }

    override fun functionExist(functionName: String): Boolean {
        return findOrNull(functionName) != null
    }


    private sealed interface GroupAnnouncementsTemplateImpl<K : Any> : TemplateImpl<K> {
        companion object {
            val list = listOf<TemplateImpl<*>>(
                PushFunction,
                ReadFunction,
                DeleteFunction,
                ReadParameter,
                ReadOnlineAnnouncement
            )


            fun getAnnouncementsFromParameter(args: Parameter): Announcements {
                return (args.getContact(0) as Group).announcements
            }

        }

        override val type: KClass<K>
        override val name: String
        override suspend fun execute(args: Parameter): K

        @SerializerData(0, SerializerData.SerializerType.CONTACT)
        object PushFunction : GroupAnnouncementsTemplateImpl<OnlineAnnouncement> {
            override val name: String
                get() = "push"
            override val type: KClass<OnlineAnnouncement>
                get() = OnlineAnnouncement::class

            override suspend fun execute(args: Parameter): OnlineAnnouncement {
                val announcements = getAnnouncementsFromParameter(args)
                val announcementParameters = AnnouncementParametersBuilder()
                val content = args[1]
                args.read {
                    var useImage = false
                    2 map map@{ // setting
                        for ((key, value) in this@map) {
                            when (key) {
                                "pinned" -> announcementParameters.isPinned = value.toBoolean()
                                "sendToNewMember" -> announcementParameters.sendToNewMember = value.toBoolean()
                                "nameEdit" -> announcementParameters.showEditCard = value.toBoolean()
                                "show" -> announcementParameters.showPopup = value.toBoolean()
                                "require" -> announcementParameters.requireConfirmation = value.toBoolean()
                                "useImage" -> useImage = value.toBoolean()
                            }
                        }
                    }
                    if (useImage) { // upload image (need useImage = true)
                        val path = next().toString()
                        val file = File(path)
                        file.toExternalResource().use {
                            announcementParameters.image = announcements.uploadImage(it)
                        }
                    }
                }

                val offlineAnnouncement = OfflineAnnouncement(content, announcementParameters.build())
                return announcements.publish(offlineAnnouncement)
            }
        }

        @SerializerData(0, SerializerData.SerializerType.CONTACT)
        object ReadFunction : GroupAnnouncementsTemplateImpl<Any> {
            override val name: String
                get() = "read"
            override val type: KClass<Any>
                get() = Any::class

            override suspend fun execute(args: Parameter): Any {
                val announcements = getAnnouncementsFromParameter(args)
                val find = args[1]
                val readData = args[2]
                val onlineAnnouncement: OnlineAnnouncement = announcements.asFlow().filter {
                    (it.fid == find) || (it.content.lines()[0] == find)
                }.firstOrNull() ?: error("Cannot find $find")

                return ReadOnlineAnnouncement.execute(
                    Parameter(
                        listOf(
                            onlineAnnouncement,
                            readData
                        )
                    )
                )

            }
        }

        @SerializerData(0, SerializerData.SerializerType.CONTACT)
        object DeleteFunction : GroupAnnouncementsTemplateImpl<Unit> {
            override val type: KClass<Unit>
                get() = Unit::class
            override val name: String
                get() = "delete"

            override suspend fun execute(args: Parameter) {
                val announcements = getAnnouncementsFromParameter(args)
                val fid = args[1]
                announcements.delete(fid)
            }
        }

        object ReadParameter : GroupAnnouncementsTemplateImpl<Any> {
            override val name: String
                get() = "readParameter"
            override val type: KClass<Any>
                get() = Any::class

            override suspend fun execute(args: Parameter): Any {
                return args.read {
                    0 read {
                        this as AnnouncementParameters
                        when (val next = next()) {
                            "image" -> this.image ?: "NULL"
                            "isPinned" -> this.isPinned
                            "sendToNewMember" -> this.sendToNewMember
                            "showPopup" -> this.showPopup
                            "showEditCard" -> this.showEditCard
                            "requireConfirmation" -> this.requireConfirmation
                            else -> error("Cannot find $next in AnnouncementParameters")
                        }
                    }
                }.lastReturnValue!!
            }
        }

        object ReadOnlineAnnouncement : GroupAnnouncementsTemplateImpl<Any> {
            override val name: String
                get() = "readOnlineAnnouncement"
            override val type: KClass<Any>
                get() = Any::class

            override suspend fun execute(args: Parameter): Any {
                return args.read {
                    0 read {
                        this as OnlineAnnouncement
                        when (val next = next()) {
                            "fid" -> this.fid
                            "content" -> this.content
                            "group" -> this.group
                            "allConfirmed", "allRead" -> this.allConfirmed
                            "confirmedMembersCount", "readCount" -> this.confirmedMembersCount
                            "sender" -> this.sender ?: "NULL"
                            "publicationTime", "time" -> this.publicationTime
                            "parameters" -> this.parameters
                            else -> error("Cannot find $next in OnlineAnnouncement")
                        }
                    }
                }.lastReturnValue!!
            }
        }

    }
}

