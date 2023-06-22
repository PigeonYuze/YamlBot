package com.pigeonyuze.util.decode

import com.pigeonyuze.LoggerManager
import com.pigeonyuze.isDebugging0
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

    protected companion object {

        inline fun <reified N : Any> Validated<BeautifulError, N>.unsafeByThrow(): N {
            onFailure { throw it }
            return (this as Validated.Valid<N>).value
        }

        fun <N,R> Validated<BeautifulError, N>.transformOrDefault(transform: (N) -> R,defaultValue: R? = null) = when (this) {
            is Validated.Valid -> transform.invoke(this.value)
            is Validated.Invalid -> defaultValue ?: throw error
        }

        inline fun <reified N : Node> Node.checkType(
            name: String, superNode: Node? = null
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

        inline fun <reified E : Throwable, reified A : Any, R> Validated<E, A>.runOrThrow(crossinline block: (A) -> R) =
            fold(ifInvalid = { throw it }, ifValid = { block.invoke(it) })

        infix fun <A> Validated<BeautifulError, A>.byThrow(error: BeautifulError): Validated<BeautifulError, A> {
            onFailure {
                error.addCause(it)
            }
            return this
        }

        inline fun <reified E : Enum<E>> StringNode.decodeToEnum(values: Array<E>): Validated<BeautifulError, E> {
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
            ).invalid()
        }

        fun ArrayNode.toStringList(arrayName: String) = this.elements.map {
            it.checkType<StringNode>("<$arrayName-elements>").getUnsafe().value
        }
        /**
         * If it is failure, run [onFailure] and check why
         * @param commandCallerErr Add an error to it
         * */
        inline fun <A> Validated<BeautifulError, A>.checkCauseMissing(
            commandCallerErr: BeautifulError,
            crossinline onFailure: (BeautifulError) -> Unit
        ): Validated<BeautifulError, A> {
            this.onFailure {
                it.because<ErrorTrace.MissingParameter>() || kotlin.run {
                    commandCallerErr.addCause(it)
                    return@onFailure
                }
                onFailure.invoke(it)
            }
            return this
        }
        fun byDefaultLogger(error: BeautifulError,fromParse: String,az: String) =
            if (!isDebugging0) LoggerManager.loggingWarn(
                "Parse-$fromParse",
                "Wrong yaml format, cannot parse as $az object. Automatically use default value\n\t${error.causes.last().errorTraceString}"
            ) else println("WARN [Parse-$fromParse] Wrong yaml format, cannot parse as $az object. Automatically use default value\n\t${error.causes.last().errorTraceString}")
    }
}