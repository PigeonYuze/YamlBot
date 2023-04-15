package com.pigeonyuze.template.data

import com.pigeonyuze.command.element.NullObject
import com.pigeonyuze.template.Parameter
import com.pigeonyuze.template.Template
import com.pigeonyuze.template.TemplateImpl
import com.pigeonyuze.template.TemplateImpl.Companion.canNotFind
import com.pigeonyuze.template.TemplateImpl.Companion.nonImpl
import com.pigeonyuze.template.parameterOf
import com.pigeonyuze.test.Testable
import com.pigeonyuze.util.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.active.ActiveChart
import net.mamoe.mirai.contact.active.ActiveHonorInfo
import net.mamoe.mirai.contact.active.ActiveHonorList
import net.mamoe.mirai.contact.active.GroupActive
import net.mamoe.mirai.contact.announcement.*
import net.mamoe.mirai.data.GroupHonorType
import net.mamoe.mirai.event.AbstractEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.io.File
import java.util.*
import kotlin.reflect.KClass


object GroupAnnouncementsTemplate : Template, Testable {
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

    override suspend fun getEventForTestAllFunction(event: AbstractEvent): List<suspend () -> Any> {
        val msgEvent = event as GroupMessageEvent
        lateinit var onlineAnnouncement: OnlineAnnouncement
        return listOf(
            {
                onlineAnnouncement = GroupAnnouncementsTemplateImpl.PushFunction.execute(
                    Parameter(
                        listOf(
                            msgEvent.subject,
                            "这是对公告功能的上传测试\n换行\nOPEN ALL",
                            mapOf(
                                "pinned" to true,
                                "sendToNewMember" to true,
                                "nameEdit" to true,
                                "show" to true,
                                "require" to true,
                            )
                        )
                    )
                )
            },
            {
                GroupAnnouncementsTemplateImpl.ReadFunction.execute(
                    Parameter(
                        listOf(
                            msgEvent.subject,
                            "这是对公告功能的上传测试",
                            "fid"
                        )
                    )
                )
            },
            {
                GroupAnnouncementsTemplateImpl.DeleteFunction.execute(
                    Parameter(
                        listOf(
                            msgEvent.subject,
                            onlineAnnouncement.fid
                        )
                    )
                )
            }
        )
    }

    override suspend fun getEventFilter(): AbstractEvent.() -> Boolean {
        return {
            this is MessageEvent
        }
    }


    sealed interface GroupAnnouncementsTemplateImpl<K : Any> : TemplateImpl<K> {
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
                            "image" -> this.image ?: NullObject
                            "isPinned" -> this.isPinned
                            "sendToNewMember" -> this.sendToNewMember
                            "showPopup" -> this.showPopup
                            "showEditCard" -> this.showEditCard
                            "requireConfirmation" -> this.requireConfirmation
                            else -> canNotFind(next.toString(), "OAnnouncementParameters")
                        }
                    }
                }
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
                            "sender" -> this.sender ?: NullObject
                            "publicationTime", "time" -> this.publicationTime
                            "parameters" -> this.parameters
                            else -> canNotFind(next.toString(), "OnlineAnnouncement")
                        }
                    }
                }
            }
        }

    }
}

object GroupActiveTemplate : Template {

    override fun values(): List<TemplateImpl<*>> {
        return GroupActiveTemplateImpl.list
    }

    override suspend fun callValue(functionName: String, args: Parameter): Any {
        return GroupActiveTemplateImpl.list.first { it.name == functionName }.execute(args)
    }

    override fun findOrNull(functionName: String): TemplateImpl<*>? {
        return GroupActiveTemplateImpl.list.filter { it.name == functionName }.getOrNull(0)
    }

    override fun functionExist(functionName: String): Boolean {
        return findOrNull(functionName) != null
    }

    sealed interface GroupActiveTemplateImpl<K : Any> : TemplateImpl<K> {
        override val name: String
        override val type: KClass<K>
        override suspend fun execute(args: Parameter): K

        companion object {
            /*
            * Impl 与 非 Impl 的区别：
            * Impl 需要用户提供符合规定的值进行解析
            * 反之则由函数获取当前状况自动解析
            * 一般状况下，所有调用了 `getActiveFromParameter` 的所有类都基本为 Impl
            * */
            val list = listOf<TemplateImpl<*>>(
                QueryHonorHistory,
                GetInstance,
                ReadActiveObj,
                /* Impl */
                ReadActiveObjImpl,
                ReadActiveHonorList,
                ReadActiveHonorInfoImpl,
                ReadActiveChartImpl,
                QueryHonorHistoryImpl,
                SettingActiveObjImpl
            )

            suspend fun getActiveFromParameter(args: Parameter): GroupActive {
                val active = (args.getContact(0) as Group).active
                coroutineScope {
                    launch {
                        active.refresh() //just refresh
                    }.join()
                }
                return active
            }
        }

        @SerializerData(0, SerializerData.SerializerType.CONTACT)
        object GetInstance : GroupActiveTemplateImpl<GroupActive> {
            override val name: String
                get() = "get"
            override val type: KClass<GroupActive>
                get() = GroupActive::class

            override suspend fun execute(args: Parameter): GroupActive {
                return getActiveFromParameter(args)
            }
        }

        @SerializerData(0, SerializerData.SerializerType.CONTACT)
        object ReadActiveObj : GroupActiveTemplateImpl<Any> {
            override val name: String
                get() = "readActiveObj"
            override val type: KClass<Any>
                get() = Any::class

            override suspend fun execute(args: Parameter): Any {
                return ReadActiveObjImpl.execute(
                    parameterOf(
                        getActiveFromParameter(args),
                        args[1]
                    )
                )
            }
        }

        @SerializerData(0, SerializerData.SerializerType.CONTACT)
        object QueryHonorHistory : GroupActiveTemplateImpl<ActiveHonorList> {
            override val name: String
                get() = "queryHonorHistory"
            override val type: KClass<ActiveHonorList>
                get() = ActiveHonorList::class

            override suspend fun execute(args: Parameter): ActiveHonorList {
                return QueryHonorHistoryImpl.execute(
                    parameterOf(
                        getActiveFromParameter(args),
                        args[1]
                    )
                )
            }
        }

        object QueryHonorHistoryImpl : GroupActiveTemplateImpl<ActiveHonorList> {
            override val name: String
                get() = "queryHonorHistoryImpl"
            override val type: KClass<ActiveHonorList>
                get() = ActiveHonorList::class

            override suspend fun execute(args: Parameter): ActiveHonorList {
                return getActiveFromParameter(args).queryHonorHistory(nameMapping(args[1]))
            }

            val nameIdMapping = mapOf(
                "学术新星" to GroupHonorType.BRONZE_ID,
                "快乐源泉" to GroupHonorType.EMOTION_ID,
                "至尊学神" to GroupHonorType.GOLDEN_ID,
                "群聊炽焰" to GroupHonorType.LEGEND_ID,
                "群聊之火" to GroupHonorType.PERFORMER_ID,
                "善财福禄寿" to GroupHonorType.RED_PACKET_ID,
                "壕礼皇冠" to GroupHonorType.RICHER_ID,
                "顶尖学霸" to GroupHonorType.SILVER_ID,
                "冒尖小春笋" to GroupHonorType.STRONG_NEWBIE_ID,
                "龙王" to GroupHonorType.TALKATIVE_ID,
                "一笔当先" to GroupHonorType.WHIRLWIND_ID
            )

            private fun nameMapping(name: String): GroupHonorType {
                val id = name.toIntOrNull() ?: nameIdMapping[name] ?: canNotFind(name, "NameMapping")
                if (id !in 0..11) {
                    nonImpl("Impl_NewGroupHonorTypeObj", "nameMapping", "the id $id should from 0 to 11")
                }
                return GroupHonorType(id)
            }
        }

        @ReadObject
        object ReadActiveHonorList : GroupActiveTemplateImpl<Any> {
            override val name: String
                get() = "readActiveHonorList"
            override val type: KClass<Any>
                get() = Any::class

            override suspend fun execute(args: Parameter): Any {
                return args.read {
                    0 read {
                        this as ActiveHonorList
                        when (val next = next()) {
                            "current" -> this.current ?: NullObject
                            "type" -> this.type
                            "records" -> this.records
                            "last" -> this.records.last()
                            "first" -> this.records.first()
                            "size" -> this.records.size
                            next.toString().isNumber() -> this.records[next.toString().toInt()]
                            else -> canNotFind(next.toString(), "ActiveHonorList")
                        }
                    }
                }
            }
        }

        @ReadObject
        object ReadActiveObjImpl : GroupActiveTemplateImpl<Any> {
            override val name: String
                get() = "readActiveObjImpl"
            override val type: KClass<Any>
                get() = Any::class

            override suspend fun execute(args: Parameter): Any {
                return args.read {
                    0 read {
                        this as GroupActive
                        when (val type = args[1]) {
                            "isHonorVisible", "showActive" -> this.isHonorVisible
                            "isTemperatureVisible", "showTemperature" -> this.isTemperatureVisible
                            "isTitleVisible", "showTitle" -> this.isTitleVisible
                            "rankTitles" -> this.rankTitles
                            "temperatureTitles" -> this.temperatureTitles
                            "thisChart", "chat" -> this.queryChart()
                            "queryActiveRank", "thisRank" -> this.queryActiveRank()
                            else -> canNotFind(type, "GroupActive")
                        }
                    }
                }
            }
        }

        @SettingObject
        object SettingActiveObjImpl : GroupActiveTemplateImpl<Any> {
            override val name: String
                get() = "settingActiveObjImpl"
            override val type: KClass<Any>
                get() = Any::class

            override suspend fun execute(args: Parameter): Any {
                return args.read {
                    0 read {
                        this as GroupActive
                        when (val type = args[1]) {
                            "isHonorVisible", "showActive" -> this.setHonorVisible(bool(2))
                            "isTemperatureVisible", "showTemperature" -> this.setTemperatureVisible(bool(2))
                            "isTitleVisible", "showTitle" -> this.setTitleVisible(bool(2))
                            "rankTitles" -> this.setRankTitles(map(2).mapCast())
                            "temperatureTitles" -> this.setTemperatureTitles(map(2).mapCast())
                            else -> canNotFind(type, "SetGroupActiveValue")
                        }
                    }
                }
            }
        }

        @ReadObject
        object ReadActiveChartImpl : GroupActiveTemplateImpl<Any> {
            override val name: String
                get() = "readActiveChatImpl"
            override val type: KClass<Any>
                get() = Any::class

            override suspend fun execute(args: Parameter): Any {
                return args.read read@{
                    0 read time@{
                        this@time as ActiveChart
                        when (val str = next()) { //index 1
                            (toString().toIntOrNull() != null), is Int -> { //yyyy(int)-mm(int)
                                var yyyy = int(1)
                                val moth = intOrNull(2) ?: kotlin.run {
                                    val tmp = yyyy
                                    yyyy = Calendar.getInstance().get(Calendar.YEAR)
                                    tmp
                                }
                                val key = "$yyyy$moth"
                                readImpl(this@read, this@time, key)
                            }
                            else -> {
                                val key = str.toString()
                                readImpl(this@read, this@time, key)
                            }
                        }
                    }
                }
            }

            private fun readImpl(
                parameterValueReader: Parameter.ParameterValueReader,
                activeChart: ActiveChart,
                key: String,
            ) = when (val type = parameterValueReader.next().toString()) {
                "actives" -> activeChart.actives[key]!!
                "exit" -> activeChart.exit[key]!!
                "join" -> activeChart.join[key]!!
                "member" -> activeChart.members[key]!!
                "sentences" -> activeChart.sentences[key]!!
                else -> canNotFind(type, "ActiveChart")
            }
        }

        @ReadObject
        object ReadActiveHonorInfoImpl : GroupActiveTemplateImpl<Any> {
            override val name: String
                get() = "readActiveHonorInfoImpl"
            override val type: KClass<Any>
                get() = Any::class

            override suspend fun execute(args: Parameter): Any {
                return args.read {
                    0 read {
                        this as ActiveHonorInfo
                        when (val type = args[1]) {
                            "avatarUrl", "avatar" -> this.avatar
                            "memberName", "name" -> this.memberName
                            "member" -> this.member ?: NullObject
                            "historyDays", "days" -> this.historyDays
                            "maxTermDays", "max" -> this.maxTermDays
                            "nowTermDays", "termDays", "now" -> this.termDays
                            "memberId", "id" -> this.memberId
                            else -> canNotFind(type, "ActiveHonorInfo")
                        }
                    }
                }
            }
        }

    }

}
