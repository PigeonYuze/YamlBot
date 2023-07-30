package com.pigeonyuze.util.decode

import com.pigeonyuze.command.element.*
import com.pigeonyuze.template.Template
import com.pigeonyuze.util.logger.ErrorAbout
import com.pigeonyuze.util.logger.ErrorTrace
import com.pigeonyuze.util.logger.runtimeErrorOf
import com.sksamuel.hoplite.Node
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * 在声明文件中声明的字段
 *
 * ### 什么是”字段“
 *
 * ”字段“ 是一切能被占位符 `%call-name%` 的源
 *
 * 运行时，替代占用符的源就是字段
 * @see FunctionField
 * */
sealed class FormatField private constructor() {
    abstract val name: String
    abstract val rootContext: Node

    /**
     * 由 [Template] 所声明的字段
     *
     * 通常在 [TemplateYML] 中声明
     *
     * 例如:
     *
     * ```yaml
     * use: IMPORT_TYPE_STRING
     * call: functionName
     * args: [...]
     * name: fieldName
     * ```
     *
     * */
    data class FunctionField(
        override val name: String,
        override val rootContext: Node,
        val functionImportType: ImportType,
        val functionName: String,
    ) : FormatField()


    companion object {
        fun FormatField.checkReturnType(needed: KClass<out Any>) =
            when (this) {
                is FunctionField -> {
                    val impl = functionImportType.getProjectClass().findOrNull(functionName) ?: throw runtimeErrorOf(
                        errorAbout = ErrorAbout.ByInstance("Template"),
                        cause = ErrorTrace.UnknownParameter(
                            rootContext.pos,
                            shouldBe = functionImportType.getProjectClass().values().map { it.name },
                            unknown = functionName
                        )
                    )
                    impl.type.isSubclassOf(needed)
                }
            }

        fun TemplateYML.ofField(rootContext: Node) = FunctionField(
            name, rootContext, this.use, this.call
        )
    }
}
