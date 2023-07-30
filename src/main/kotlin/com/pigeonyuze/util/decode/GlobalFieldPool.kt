package com.pigeonyuze.util.decode

import com.pigeonyuze.util.logger.ErrorTrace
import com.pigeonyuze.util.logger.runtimeErrorOf
import com.sksamuel.hoplite.Node
import com.sksamuel.hoplite.Pos

/**
 * 字段池是在初始化阶段所使用的工具
 *
 * 即负责将解析遇到的字段存入字段池
 *
 * 每一个字段都有对应的作用域，每一个作用域中不允许存在重复的字段
 *
 * **在结束初始化后，请手动调用** [free] **用于删除此处不必要的内存占用**
 *
 * 该类在设计时为减少内存占用，使用了 `null` 作为每一个作用域中的分割线，**请保证你的调用时期在 当前解析完毕 到  解析下一处 之间**
 *
 * @suppress 此类不是线程安全的 禁止使用 协程(虚拟线程) / 多线程 访问此类，这可能会造成数据失效
 * */
internal class GlobalFieldPool private constructor(){

    /**
     * 线程池实现
     * */
    @get:Synchronized
    private val pool = mutableListOf<FormatField?>()
    @get:Synchronized
    private var lastedContext: Node? = null

    @Synchronized
    fun intoPool(formatField: FormatField, context: Node) {
        if (lastedContext == null) {
            lastedContext = context
        } else if (lastedContext != context) {
            pool.add(null) // 每一个 `null` 表示一个作用域
            lastedContext = context
        }
        pool.add(formatField)
    }

    @Synchronized
    fun getFromPool(name: String, isDropFromPool: Boolean = false): FormatField {
        val currentScopeIndex = pool.lastIndexOf(null)
        val value = pool.subList(currentScopeIndex, pool.lastIndex)
        if (isDropFromPool) {
            pool.dropLast(pool.lastIndex - currentScopeIndex)
        }
        require(value.isNotEmpty())
        val objects = value.filter { it?.name == name }
        when {
            objects.size > 1 -> {
                throw runtimeErrorOf(
                    "Parsing ambiguity in field pool",
                    cause = ErrorTrace.CannotParseAsAnyone(Pos.NoPos, "Same fields are namesake: $objects")
                )
            }

            objects.isEmpty() -> throw RuntimeException("Internal error: Can not find `$name` from pool $value {fullPool=$pool}")
        }
        return objects.last()!!

    }

    fun free() {
        pool.clear()
        lastedContext = null
    }

    internal companion object {
        private val currentGlobalFieldPool0 = GlobalFieldPool()

        fun getGlobalFieldPool()
            = currentGlobalFieldPool0
    }
}