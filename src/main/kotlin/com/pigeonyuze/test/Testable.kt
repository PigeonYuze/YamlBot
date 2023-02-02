package com.pigeonyuze.test

import com.pigeonyuze.template.data.GroupAnnouncementsTemplate
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.event.AbstractEvent

/**
 * 用于功能的测试
 *
 * 你可以参照 [GroupAnnouncementsTemplate] 的写法
 * */
internal interface Testable {
    /**
     * 用于测试每一个功能, 如果需要参数请实现 [getEventForTestAllFunction]
     *
     * 在运行时会优先运行 [getEventFilter] 获取筛选器，再运行
     * */
    suspend fun getAllTestObj(): List<suspend () -> Any>

    /**
     * 如果事件符合 [getEventFilter] 后会调用此项 你可以通过此处获取事件
     *
     * @return 检测是否被重写 如果你重写了此项请返回一个非`null`值 不然
     * */
    suspend fun getEventForTestAllFunction(event: AbstractEvent): List<suspend () -> Any> {
        throw Error("No impl!")
    }

    /**
     * 返回符合 `return` 的事件
     * */
    suspend fun getEventFilter(): (AbstractEvent.() -> Boolean)? {
        return null
    }

    companion object {
        suspend fun Testable.runTest(event: AbstractEvent) {
            val filter = this.getEventFilter() ?: { true }
            if (!filter.invoke(event)) {
                return
            }

            kotlin.runCatching {
                coroutineScope {

                    launch {
                        for ((index, value) in this@runTest.getEventForTestAllFunction(event).withIndex()) {
                            println("Run Test When $index")
                            value.invoke()
                            println("Stop Test When $index")
                            delay(2000L)
                        }

                    }
                }
            }.recoverCatching {  // no impl
                if (it.message != "No impl!") {
                    throw it
                }
                for ((index, value) in this.getAllTestObj().withIndex()) {
                    println("Run Test When $index")
                    value.invoke()
                    println("Stop Test When $index")
                    delay(2000L)
                }
            }
        }
    }
}