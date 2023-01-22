package com.pigeonyuze.template.data

import com.pigeonyuze.UserConfig
import com.pigeonyuze.UserData
import com.pigeonyuze.account.User
import com.pigeonyuze.template.Parameter
import com.pigeonyuze.template.Template
import com.pigeonyuze.template.TemplateImpl
import com.pigeonyuze.util.FunctionArgsSize
import com.pigeonyuze.util.SerializerData
import com.pigeonyuze.util.isBoolean
import com.pigeonyuze.util.isLong
import java.util.*
import kotlin.reflect.KClass

object UserTemplate : Template {
    override suspend fun callValue(functionName: String, args: Parameter): Any {
        return UserTemplateImpl.findFunction(functionName)!!.execute(args)
    }

    override fun functionExist(functionName: String): Boolean {
        return UserTemplateImpl.findFunction(functionName) != null
    }

    override fun findOrNull(functionName: String): TemplateImpl<*>? {
        return UserTemplateImpl.findFunction(functionName)
    }

    override fun values(): List<TemplateImpl<*>> {
        return UserTemplateImpl.list
    }


    private sealed interface UserTemplateImpl<K : Any> : TemplateImpl<K> {

        companion object {
            val list: List<UserTemplateImpl<*>> = listOf(
                ValueFunction,
                RegFunction,
                SetFunction,
                PlusFunction,
                MinusFunction,
                CallFunction,
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

        override val name: String
        override val type: KClass<K>
        override suspend fun execute(args: Parameter): K

        @FunctionArgsSize([2])
        @SerializerData(1, SerializerData.SerializerType.SENDER_ID)
        object ValueFunction : UserTemplateImpl<String> {
            override val name: String
                get() = "value"
            override val type: KClass<String>
                get() = String::class

            override suspend fun execute(args: Parameter): String {
                return when (args.size) {
                    2 -> value(args[0], args.getLong(1))
                    3 -> value(args[0], args.getLong(2))
                    else -> error(name, args.size)
                }
            }

            fun value(name: String,qqId: Long) : String{
                val user = UserData.userList.first { it.qqId == qqId }
                return user.findValue(name)
            }

        }

        @FunctionArgsSize([0])
        @SerializerData(0, SerializerData.SerializerType.SENDER_NAME)
        @SerializerData(1, SerializerData.SerializerType.SENDER_NICK)
        @SerializerData(2, SerializerData.SerializerType.SENDER_ID)
        object RegFunction : UserTemplateImpl<Boolean> {
            override val name: String
                get() = "reg"
            override val type: KClass<Boolean>
                get() = Boolean::class

            override suspend fun execute(args: Parameter): Boolean {
                if (args.size != 3) error(name, args.size)
                return reg(
                    when (UserConfig.userNickSource) {
                        "nick" -> args[1]
                        "name" -> args[2]
                        else -> UserConfig.userNickSource
                    }, args[2].toLong()
                )
            }

            private fun reg(name: String, qqId: Long): Boolean {
                if (UserData.userList.any { it.qqId == qqId }) return false
                UserData.userList.add(User(qqId, name, UserConfig.userStartIndex + UserData.userList.size + 1))
                return true
            }

        }

        @FunctionArgsSize([1, 2, 3])
        @SerializerData(3, SerializerData.SerializerType.SENDER_ID) //
        object SetFunction : UserTemplateImpl<Unit> {
            override val name: String
                get() = "set"
            override val type: KClass<Unit>
                get() = Unit::class

            override suspend fun execute(args: Parameter) {
                when (args.size) { //以下逻辑比较复杂 我也很难整明白 你看注释就行 by Pigeon_Yuze.
                    4 -> {
                        //args: (name) (forAllUserRun) (newValue) ($auto-plus--sender_id$)
                        if (args[1].isBoolean()) set(args[0], args[2], args.getBoolean(1))
                        //args: (name) (object-qqid) (newValue) ($auto-plus--sender_id$)
                        else set(args[0], args.getLong(1), args[2])
                    }
                    3 -> {
                        //args: (name) (object-qqid) ($auto-plus--sender_id$) -> init
                        if (args[1].isLong()) set(args[0], args.getLong(1))
                        //args: (name) (newValue) ($auto-plus--sender_id$)
                        else set(args[0], args.getLong(2),args[1])
                    }
                    2 -> { //args: (name) ($auto-plus--sender_id$) -> init
                        set(args[0],args.getLong(1))
                    }
                    else -> { //什么都不是 不支持的参数
                        error(name,args.size)
                    }
                }
                if (args.size != 2) error(name,args.size)
            }

            fun set(name: String,newValue: String,isForAllUser: Boolean){ //for all
                if (!isForAllUser)  return
                for (user in UserData.userList) {
                    user.set(name,newValue)
                }
            }

            fun set(name: String,qqId: Long,newValue: String){
                UserData.userList.first { it.qqId == qqId }.set(name, newValue)
            }

            fun set(name: String, qqId: Long) {
                val userElement = UserData.userList.first { it.qqId == qqId }.find(name)
                userElement.initValue()
            }

        }

        @FunctionArgsSize([2, 3])
        @SerializerData(3, SerializerData.SerializerType.SENDER_ID) //
        object PlusFunction : UserTemplateImpl<Unit> {
            override val name: String
                get() = "plus"
            override val type: KClass<Unit>
                get() = Unit::class

            override suspend fun execute(args: Parameter) {
                when (args.size) { //和上面的判断逻辑差不多 by Pigeon_Yuze.
                    4 -> {
                        //args: (name) (object-qqid) (newValue) ($auto-plus--sender_id$)
                        plus(args[0], args.getLong(1), args[2])
                    }
                    3 -> {
                        //args: (name) (newValue) ($auto-plus--sender_id$)
                        plus(args[0], args.getLong(2), args[1])
                    }
                    else -> {
                        error(name, args.size)
                    }
                }
            }


            fun plus(name: String, qqId: Long, newValue: String) {
                UserData.userList.first { it.qqId == qqId }.plus(name, newValue)
            }

        }

        @FunctionArgsSize([1, 2, 3])
        @SerializerData(3, SerializerData.SerializerType.SENDER_ID) //
        object MinusFunction : UserTemplateImpl<Unit> {
            override val name: String
                get() = "minus"
            override val type: KClass<Unit>
                get() = Unit::class

            override suspend fun execute(args: Parameter) {
                when (args.size) { //和上面的逻辑差不多 by Pigeon_Yuze.
                    4 -> {
                        //args: (name) (object-qqid) (newValue) ($auto-plus--sender_id$)
                        minus(args[0], args.getLong(1), args[2])
                    }
                    3 -> {
                        //args: (name) (newValue) ($auto-plus--sender_id$)
                        minus(args[0], args.getLong(2), args[1])
                    }
                    else -> {
                        error(name, args.size)
                    }
                }
            }


            fun minus(name: String,qqId: Long,newValue: String){
                UserData.userList.first { it.qqId == qqId }.plus(name,newValue)
            }
        }

        @FunctionArgsSize([-1])
        @SerializerData(0, SerializerData.SerializerType.SENDER_ID)
        object CallFunction : UserTemplateImpl<Any> {
            override val name: String
                get() = "callElementFunction"
            override val type: KClass<Any>
                get() = Any::class

            override suspend fun execute(args: Parameter): Any {
                if (args.size < 3) error(name, args.size)
                val senderID = args.getLong(0)
                val inArgsExistObjectId = args[1].isLong()
                val userElement =
                    UserData.userList.first { it.qqId == senderID }.find(if (inArgsExistObjectId) args[2] else args[1])
                val functionName = if (inArgsExistObjectId) args[3] else args[2]
                val callArgs = if (inArgsExistObjectId) args.subList(3,args.lastIndex) else args.subList(2,args.lastIndex)
                return userElement.call(functionName,callArgs) ?: "null"
            }
        }
    }

}