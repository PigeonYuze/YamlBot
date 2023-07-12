package com.pigeonyuze

import com.pigeonyuze.account.User
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value

object UserData : AutoSavePluginData("UserData"){
    val userList : MutableList<User> by value(mutableListOf(User(0,"testUser",1)))
}
