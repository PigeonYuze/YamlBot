package com.pigeonyuze.test

import com.pigeonyuze.test.Testable.Companion.runTest
import net.mamoe.mirai.event.AbstractEvent
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import kotlin.reflect.KClass

internal object RunTest {

    private val waitForTest = listOf<Testable>( //将要进行测试的类

    )

    private val tempForClass = mutableMapOf<KClass<out AbstractEvent>, Boolean>()

    private var runOver = true

    // 请根据测试需要自行修改配置
    fun testSkipSetting(): AbstractEvent.() -> Boolean {
        return {
            this !is MessageEvent
        }
    }

    /**
     * @suppress 在运行此后 [waitForTest] 内所有内容将会被运行一次 <br> 并且如果前者没有运行完成将不会再次运行
     *
     * */
    fun run() {
        if (waitForTest.isEmpty()) return
        GlobalEventChannel.subscribeAlways<AbstractEvent> {
            if (!runOver) return@subscribeAlways

            val skipOrRun = tempForClass[this::class]
            if (skipOrRun == null) {
                val skip = testSkipSetting().invoke(this)
                tempForClass[this::class] = skip
                if (skip) return@subscribeAlways
            } else {
                if (skipOrRun) return@subscribeAlways
            }

            println("run start")
            for (testObj in waitForTest) {
                testObj.runTest(this)
            }
            println("run over")
        }
    }
}