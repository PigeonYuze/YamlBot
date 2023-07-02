package com.pigeonyuze.account

import com.pigeonyuze.util.setting.UserConfig

@kotlinx.serialization.Serializable
data class User(val qqId: Long, var name: String, val id: Int) {

    private val otherElements: MutableList<UserElement> = UserConfig.otherElements

    fun findValue(nameObject: String): String {
        if (!UserConfig.open) error("User does not open")
        return find(nameObject).value()
    }

    fun find(nameObject: String): UserElement {
        if (!UserConfig.open) error("User does not open")
        for (userElement in otherElements) {
            if (userElement.name != nameObject) continue
            return userElement
        }
        error("Cannot find value $nameObject")
    }

    fun plus(nameObject: String, plusValue: String) {
        if (!UserConfig.open) error("User does not open")
        for (userElement in otherElements) {
            if (userElement.name != nameObject) continue
            userElement.plusNewValue(userElement.copy(defaultValue = plusValue))
        }
        error("Cannot find value $nameObject")
    }

    fun minus(nameObject: String, plusValue: String) {
        if (!UserConfig.open) error("User does not open")
        for (userElement in otherElements) {
            if (userElement.name != nameObject) continue
            userElement.minusNewValue(userElement.copy(defaultValue = plusValue))
        }
        error("Cannot find value $nameObject")
    }

    fun set(nameObject: String, plusValue: String) {
        if (!UserConfig.open) error("User does not open")
        for (userElement in otherElements) {
            if (userElement.name != nameObject) continue
            userElement.set(userElement.copy(defaultValue = plusValue))
        }
        error("Cannot find value $nameObject")
    }

    override fun toString(): String {
        return "$name,id=$id,qqID=$qqId\n$otherElements"
    }


}
