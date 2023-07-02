package com.pigeonyuze.util.decode

import com.pigeonyuze.util.logger.BeautifulError
import com.pigeonyuze.util.logger.ErrorAbout
import com.pigeonyuze.util.logger.ErrorTrace
import com.pigeonyuze.util.logger.ErrorType
import com.sksamuel.hoplite.*
import com.sksamuel.hoplite.fp.Validated
import com.sksamuel.hoplite.fp.invalid
import com.sksamuel.hoplite.fp.valid

/**
 * ## 配置文件解析器
 *
 * 使用 `hoplite` 进行解析工作
 *
 * **通常地，此类只会在初始化时被调用**
 *
 * 支持多种配置语言
- `Json(.json)`
- `Yaml(.yaml&.yml)`
- `Toml(.toml)`
- `Hocon(.conf)`
- `Java Properties files(.props&.properties)`

 * @suppress 对下 (`version <= 1.7.0`) **不兼容**
 * @since 2.0.0
 * @see handle
 * @see parseImpl
 * @property K 解析的对象
 * */
internal abstract class ConfigDecoder<K : Any> {
    /**
     * ### 解析 [Node]
     * 一般来说，只会在初始化时被调用
     * - 当 [Node] 顶层为 [ArrayNode] 时， 解析每一个 [ArrayNode] 的成员为 [K]
     * - 当 [Node] 顶层为 [MapNode] 时， 解析整个 [MapNode] 为 [K]
     * - 如果都不是时， 抛出错误（指出原因为无法解析指定类型）
     *
     *
     * ### 不向下兼容
     *
     * - 在旧的 `YamlBot`项目中
     *  通常包含一个 `data` 作为 [MapNode] 的 `key`， 其`MapNode`的`value`为一个 [ArrayNode] 对象
     *  [ArrayNode] 中每一个对象存储的值是 [K] (**内置`AutoSavePluginConfig`的缺点**)
     * - 在此版本中，会将 [MapNode] 直接解析为 [K]
     * @see parseImpl
     * @throws BeautifulError 读取时发生错误
     * */
    fun handle(nodes: Node) {
        val objects = arrayListOf<K>()
        when (nodes) {
            is MapNode -> {
                /* command object or array of K */
                objects.add(parseImpl(nodes))
            }
            is ArrayNode -> {
                /* all nodes are K object */
                for (node in nodes.elements) {
                    objects.add(parseImpl(node.checkType<MapNode>("<root-array>").getUnsafe()))
                }
            }
            else -> {
                throw BeautifulError(
                    errorType = ErrorType.READING_CONFIG,
                    errorAbout = ErrorAbout.ByInstance(instanceObj),
                    ErrorTrace.WrongYamlTypeError(
                        nodes.pos,
                        nodes.simpleName,
                        "<root>",
                        "Array",
                        "Mapping"
                    )
                )
            }
        }
        handle(objects)
    }

    /**
     * 处理解析后得到的值
     * */
    protected abstract fun handle(objects: ArrayList<K>)

    /**
     * 解析每一个位于 [Node] 中的可能对象
     *
     * 当无法解析成功时，抛出错误
     *
     * @return 解析后得到的值
     * */
    protected abstract fun parseImpl(mapNode: MapNode): K
    abstract val instanceObj: String

    /**
     * Parse Tools
     *
     * Automatic handling of in-flight errors
     *
     * Function [invoke] that directly calls [Result] after processing a single argument
     *
     * Also, there are some built-in functions, you can parse your config easily.
     *
     * *e.g. You can call [get] to get the specified type Node*
     *
     * **NOTE: Please call [checkError] to check error in parsing before invoke the value of each parameter**
     *
     * @param parseObj The name of parsing object. If it needs to throw errors, it will output the name by [ErrorAbout.ByInstance]
     * @param superNode The super node of all parameters. When it misses anything to parse, will output it's [Node.pos] info.
     * @suppress [superNode] isn't a [MapNode].
     * */
    protected class ParseBuilder(internal val parseObj: String, internal val superNode: Node) {
        init {
            require(superNode is MapNode)
        }

        /**
         * Runtime errors
         * */
        var parsingError = BeautifulError(
            errorType = ErrorType.READING_CONFIG,
            errorAbout = ErrorAbout.ByInstance(parseObj)
        )
            private set

        /**
         * Check [parsingError].
         *
         * If there are some errors in [parsingError], It will throw these errors.
         * */
        fun checkError() {
            parsingError.isEmpty() || throw parsingError
        }

        /**
         * Detect if the type of [Node] is [N]
         *
         * @param name The name of this node. Output it when an error occurs.
         * @param couldMissing Whether to allow [Undefined]
         *
         * @return The node as [N] or an error
         * */
        inline fun <reified N : Node> Node.checkType(
            name: String, couldMissing: Boolean = false,
        ): Result<N> {
            this !is N || return this.valid()
            val error = if (this == Undefined) { //missing
                BeautifulError(
                    errorType = ErrorType.READING_CONFIG,
                    errorAbout = ErrorAbout.ByInstance(parseObj),
                    ErrorTrace.MissingParameter(
                        trace = superNode.pos,
                        missingObj = name
                    )
                ).apply {
                    if (couldMissing) parsingError += this
                }
            } else {
                BeautifulError(
                    errorType = ErrorType.READING_CONFIG,
                    errorAbout = ErrorAbout.ByInstance(parseObj),
                    ErrorTrace.WrongYamlTypeError(
                        trace = pos,
                        readingObj = simpleName,
                        name = name,
                        shouldBe = arrayOf(N::class.simpleName ?: "Node")
                    )
                ).apply {
                    parsingError += this
                }
            }
            return error.invalid()
        }

        /**
         * Detect if equaling [StringNode] and each the name of [values]. If they are the same, this will return it
         * @return An enum [E] or an error
         * */
        inline fun <reified E : Enum<E>> StringNode.decodeToEnum(values: Array<E>): Result<E> {
            for (value in values) {
                if (this.value == value.name) return value.valid()
            }
            return BeautifulError(
                errorType = ErrorType.READING_CONFIG,
                errorAbout = ErrorAbout.ByInstance(E::class.simpleName ?: "<anonymous object>"),
                ErrorTrace.UnknownParameter(
                    trace = pos,
                    shouldBe = values.map { it.name },
                    unknown = this.value
                )
            ).apply { parsingError += this }.invalid()
        }

        /**
         * Makes all the values in [ArrayNode] as [String], and add to new [List]
         * */
        fun ArrayNode.toStringList(arrayName: String) =
            this.elements
                .map { it.checkType<StringNode>("<$arrayName-elements>") }
                .map { it.invoke().value }

        /**
         * Gets the value corresponding to the key and detects the type of the value
         *
         * @see checkType
         * */
        @JvmName("getWithType")
        inline operator fun <reified K : Node> get(key: String, isMaybeMissing: Boolean = false) =
            superNode[key].checkType<K>(key, isMaybeMissing)

        operator fun get(key: String): Node = superNode[key]

        /**
         * Get the value of [Result]
         * @return
         * - If it is invalid , then this will throw an error
         * - If it is valid, then this will return the value in the [Result]
         * */
        @Throws(BeautifulError::class)
        operator fun <K> Result<K>.invoke(): K {
            onFailure { throw it }
            return (this as Validated.Valid<K>).value
        }

        /**
         * Get the value of [Result]
         * @return
         * - If it is invalid , then this will throw an error
         * - If it is valid, then this will return the value by [transform]
         * */
        @Throws(BeautifulError::class)
        inline operator fun <K, R> Result<K>.invoke(transform: (K) -> R): R {
            onFailure { throw it }
            return transform((this as Validated.Valid<K>).value)
        }

        /**
         * Get the value of [Result]
         * @return
         * - If it is invalid , then this will return [defaultValue]
         * - If it is valid, then this will return the value by [transform]
         * */
        inline operator fun <K, R> Result<K>.invoke(defaultValue: R, transform: (K) -> R): R {
            return when (this) {
                is Validated.Valid -> transform(this.value)
                else -> defaultValue
            }
        }
    }

    /**
     * Create a [ParseBuilder] object, and parse an [MapNode] as [R]
     *
     * @param R the type of returned value
     * @see ParseBuilder
     * */
    protected inline fun <R> parseBuilder(parseObj: String, superNode: Node, run: ParseBuilder.() -> R): R {
        return run.invoke(ParseBuilder(parseObj, superNode))
    }

    protected companion object {
        inline fun <reified N : Node> Node.checkType(
            name: String, superNode: Node? = null,
        ): Validated<BeautifulError, N> {

            this !is N || return this.valid()
            return if (this == Undefined) { //missing
                BeautifulError(
                    errorType = ErrorType.READING_CONFIG,
                    errorAbout = ErrorAbout.ByInstance("Command"),
                    ErrorTrace.MissingParameter(
                        trace = superNode?.pos ?: Pos.NoPos,
                        missingObj = name
                    )
                ).invalid()
            } else {
                BeautifulError(
                    errorType = ErrorType.READING_CONFIG,
                    errorAbout = ErrorAbout.ByInstance("Command"),
                    ErrorTrace.WrongYamlTypeError(
                        trace = pos,
                        readingObj = simpleName,
                        name = name,
                        shouldBe = arrayOf(N::class.simpleName ?: "Node")
                    )
                ).invalid()
            }
        }

        fun <A> Result<A>.unsafeByThrow(): A {
            onFailure { throw it }
            return (this as Validated.Valid<A>).value
        }

    }
}

typealias Result<V> = Validated<BeautifulError, V>