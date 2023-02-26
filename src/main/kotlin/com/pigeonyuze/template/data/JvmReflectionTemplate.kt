package com.pigeonyuze.template.data

import com.pigeonyuze.command.element.NullObject
import com.pigeonyuze.template.Parameter
import com.pigeonyuze.template.Template
import com.pigeonyuze.template.TemplateImpl
import com.pigeonyuze.util.keyAndValueStringDataToMap
import com.pigeonyuze.util.listToStringDataToList
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.jvmErasure

object JvmReflectionTemplate : Template {

    override fun values(): List<TemplateImpl<*>> {
        return JvmReflectionTemplateImpl.list
    }

    override fun functionExist(functionName: String): Boolean {
        return findOrNull(functionName) != null
    }

    override fun findOrNull(functionName: String): TemplateImpl<*>? {
        return JvmReflectionTemplateImpl.list.firstOrNull { it.name == functionName }
    }

    override suspend fun callValue(functionName: String, args: Parameter): Any {
        return findOrNull(functionName)!!.execute(args)
    }

    sealed interface JvmReflectionTemplateImpl<K : Any> : TemplateImpl<K> {
        override val name: String
        override val type: KClass<K>
        override suspend fun execute(args: Parameter): K

        companion object {

            val list = listOf<TemplateImpl<*>>(
                ClearCache,
                /* Kotlin */
                KotlinReflection.KotlinReflectionConstruct,
                KotlinReflection.KotlinReflectionFunction,
                KotlinReflection.KotlinRefectionField,
                KotlinReflection.KotlinRefectionInstanceField,
                KotlinReflection.KotlinRefectionSetField,
                KotlinReflection.KotlinRefectionSetInstanceField,
            )

            private fun nameCast(simpleName: String, castObj: Any): Any {
                if (castObj::class.simpleName == simpleName) return castObj
                val anyString = castObj.toString()
                return when {
                    anyString == "int" || anyString == "Int" -> anyString.toIntOrNull() ?: throwError(
                        simpleName,
                        anyString
                    )
                    anyString == "float" || anyString == "Float" -> anyString.toFloatOrNull() ?: throwError(
                        simpleName,
                        anyString
                    )
                    anyString == "char" || anyString == "Char" -> anyString.toByteOrNull()?.toInt()?.toChar()
                        ?: throwError(
                            simpleName,
                            anyString
                        )
                    anyString == "byte" || anyString == "Byte" -> anyString.toByteOrNull() ?: throwError(
                        simpleName,
                        anyString
                    )
                    anyString == "double" || anyString == "Double" -> anyString.toDoubleOrNull() ?: throwError(
                        simpleName,
                        anyString
                    )
                    anyString == "BigDecimal" -> anyString.toBigDecimalOrNull() ?: throwError(simpleName, anyString)
                    anyString == "long" || anyString == "Long" -> anyString.toLongOrNull() ?: throwError(
                        simpleName,
                        anyString
                    )
                    anyString.endsWith("list") -> anyString.listToStringDataToList(1)
                    anyString.endsWith("set") -> anyString.listToStringDataToList(1).toSet()
                    anyString.endsWith("map") -> anyString.keyAndValueStringDataToMap(1)
                    /* TODO: Build for either class
                    anyString.startsWith("[build]") -> { //try to build class for value
                        val buildValue = anyString.substringAfter("[build]")
                        val buildObj = buildValue.substringBefore("(")
                        if (buildObj != simpleName) throwError(simpleName, anyString)

                    }*/
                    else -> throwError(simpleName, anyString)
                }
            }

            private fun throwError(type: String, what: Any): Nothing =
                throw ClassCastException("Should be $type, but does not provide $what that does not support conversion to $type")

            /**
             * 用于缓存限定为`final`值的`map`
             *
             * 当目标被标记为`final`时，在第一次访问后它的值并不会改变
             *
             * 可使用此查询后直接获取值
             *
             * @suppress **注意** 此项应只限用于变量/常量缓存，如果函数为`final`返回的值可能会不同
             * */
            private val finalValueMap = mutableMapOf<String, Any>()

            private val classValueMap = mutableMapOf<String, Class<*>>()
            private val kClassValueMap = mutableMapOf<String, KClass<*>>()
        }

        object ClearCache : JvmReflectionTemplateImpl<Unit> {
            override val name: String
                get() = "clearCache"
            override val type: KClass<Unit>
                get() = Unit::class

            override suspend fun execute(args: Parameter) {
                finalValueMap.clear()
                classValueMap.clear()
                kClassValueMap.clear()
            }
        }


        sealed interface KotlinReflection<K : Any> : JvmReflectionTemplateImpl<K> {
            companion object {
                private fun fieldFunction(kotlinClass: KClass<*>, name: String, type: String): KFunction<*> {
                    val valOrVar =
                        kotlinClass.functions.filter { it.name.startsWith("<get") || it.name.startsWith("<set") }
                    val getFunction =
                        valOrVar.filter { it.name == "<$type $name>" } //kotlin val or var get function name
                    if (getFunction.size != 1) {
                        conflictingOverloadsFunctionToString(getFunction)
                    }
                    return valOrVar.first()
                }

                private fun conflictingOverloadsFunctionToString(function: List<KFunction<*>>) =
                    "Conflicting overloads: ${
                        functionsToString(function)
                    }"

                private fun functionsToString(function: List<KFunction<*>>) = function.joinToString(
                    " , ",
                    transform = {

                        var str = ""

                        str += it.visibility?.name ?: "<unknown visibility>"
                        if (it.isInfix) {
                            str += " infix "
                        }
                        if (it.isOperator) {
                            str += "operator "
                        }
                        str += it.name

                        str
                    }
                )

                private fun functionsSizeCheck(function: List<KFunction<*>>, name: String) {
                    if (function.filter {
                            it.annotations.filterIsInstance<JvmName>()
                                .firstOrNull()?.name == name
                        }.size != 1) {
                        throw NoSuchMethodError(conflictingOverloadsFunctionToString(function))
                    }
                }

                private fun functionRunOrNull(
                    kFunction: KFunction<*>,
                    parameter: List<*>,
                    fromObj: Any? = null,
                    extension: Any? = null,
                ): Any? {
                    val args = kFunction.parameters
                    if (args.size - 2 <= parameter.size) return null //if instance

                    val runArgs = arrayOf<Any>(args.size)
                    for ((index, value) in parameter.withIndex()) {
                        val thisParameter = args[index]
                        if (thisParameter.kind == KParameter.Kind.INSTANCE) {
                            runArgs[index] = fromObj
                                ?: throw IllegalArgumentException("Function $kFunction parameters need INSTANCE object,but cannot find it")
                            continue
                        }
                        if (thisParameter.kind == KParameter.Kind.EXTENSION_RECEIVER) {
                            runArgs[index] = extension
                                ?: throw IllegalArgumentException("Function $kFunction need EXTENSION_RECEIVER object,but cannot find it")
                            continue
                        }
                        val name = thisParameter.type.jvmErasure.simpleName ?: "<anonymous object>"
                        runArgs[index] = nameCast(name, value!!)
                    }

                    return kFunction.call(runArgs)
                }

            }

            object KotlinRefectionField : KotlinReflection<Any> {
                override val name: String
                    get() = "fieldKotlin"
                override val type: KClass<Any>
                    get() = Any::class

                override suspend fun execute(args: Parameter): Any {
                    return args.read {
                        val name = args[0]
                        if (finalValueMap.containsKey(name)) return@read finalValueMap[name]
                        val classPackage = args[1]
                        val kotlinClass = kClassValueMap.getOrPut(classPackage) { Class.forName(classPackage).kotlin }

                        val objectFunction = fieldFunction(kotlinClass, name, "get")
                        val value = objectFunction.call() ?: NullObject //get function do not need args
                        if (objectFunction.isFinal) finalValueMap[name] = objectFunction
                        return@read value
                    }.lastReturnValue
                }


            }

            object KotlinRefectionInstanceField : KotlinReflection<Any> {
                override val name: String
                    get() = "instanceFieldKotlin"
                override val type: KClass<Any>
                    get() = Any::class

                override suspend fun execute(args: Parameter): Any {
                    return args.read {
                        val name = args[0]
                        if (finalValueMap.containsKey(name)) return@read finalValueMap[name]
                        val classObj = next()
                        val kotlinClass = classObj::class

                        val objectFunction = fieldFunction(kotlinClass, name, "get")
                        val value = objectFunction.call(classObj) ?: NullObject //get function do not need args
                        if (objectFunction.isFinal) finalValueMap[name] = objectFunction
                        return@read value
                    }.lastReturnValue
                }


            }

            object KotlinRefectionSetField : KotlinReflection<Boolean> {
                override val name: String
                    get() = "setFieldKotlin"
                override val type: KClass<Boolean>
                    get() = Boolean::class

                override suspend fun execute(args: Parameter): Boolean {
                    return args.read {
                        val name = args[0]
                        val className = args[1]
                        val newValue = next()
                        if (finalValueMap.containsKey(name)) return@read false

                        val kotlinClass = kClassValueMap.getOrPut(className) { Class.forName(className).kotlin }

                        val objectFunction = fieldFunction(kotlinClass, name, "set")
                        objectFunction.call(newValue) //call set
                        return@read true
                    }.lastReturnValue as Boolean
                }
            }

            object KotlinRefectionSetInstanceField : KotlinReflection<Boolean> {
                override val name: String
                    get() = "instanceSetFieldKotlin"
                override val type: KClass<Boolean>
                    get() = Boolean::class

                override suspend fun execute(args: Parameter): Boolean {
                    return args.read {
                        val name = args[0]
                        val classObj = next()
                        val newValue = next()
                        if (finalValueMap.containsKey(name)) return@read false

                        val kotlinClass = classObj::class

                        val objectFunction = fieldFunction(kotlinClass, name, "set")
                        objectFunction.call(classObj, newValue) //call set
                        return@read true
                    }.lastReturnValue as Boolean
                }
            }

            /**
             * 用于反射一个已有对象的函数
             * */
            object KotlinReflectionFunction : KotlinReflection<Any> {
                override val name: String
                    get() = "function"
                override val type: KClass<Any>
                    get() = Any::class

                override suspend fun execute(args: Parameter): Any {
                    return args.read {
                        val name = args[0]
                        val parameter = list(1)
                        /* Function in object */
                        val obj = next()
                        val kotlinClass = obj::class
                        val functions =
                            kotlinClass.functions.filter {
                                if (it.parameters.size != parameter.size) return@filter false
                                it.name == name || it.annotations.filterIsInstance<JvmName>()
                                    .firstOrNull()?.name == name
                            }

                        if (functions.isEmpty()) {
                            throw NoSuchMethodError("Cannot found functions $name with args: $parameter")
                        }
                        if (functions.size != 1) { //too many function,try to use JvmName equals
                            functionsSizeCheck(functions, name)
                        }

                        val function = functions.first()

                        if (function.isExternal) {
                            val external = nextOrNull()
                                ?: throw NullPointerException("function is external,but parameter cannot find external class")
                            return@read functionRunOrNull(
                                function,
                                parameter,
                                obj,
                                external
                            )
                        }

                        return@read functionRunOrNull(
                            function,
                            parameter,
                            obj
                        )
                    }.lastReturnValue
                }


            }

            object KotlinReflectionConstruct : KotlinReflection<Any> {
                override val name: String
                    get() = "constructKotlin"
                override val type: KClass<Any>
                    get() = Any::class

                override suspend fun execute(args: Parameter): Any {
                    return args.read {
                        val name = args[0]
                        val kotlinClass = kClassValueMap.getOrPut(name) { Class.forName(name).kotlin }

                        //try to build
                        val constructorArg = list(2)
                        val all = kotlinClass.constructors
                        var instanceAny: Any? = null
                        for (constructor in all) {
                            val parameters = constructor.parameters
                            if (parameters.size != constructorArg.size) continue
                            instanceAny = functionRunOrNull(
                                /* Constructor must not be extension function! */
                                /* Constructor doesn't need TO INSTANCE */
                                constructor,
                                constructorArg
                            ) ?: continue
                        }
                        if (instanceAny == null) {
                            throw ClassCastException("Cannot build ${kotlinClass.qualifiedName}")
                        }
                        return@read instanceAny
                    }.lastReturnValue
                }

            }

        }

    }


}