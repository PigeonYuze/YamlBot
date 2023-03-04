package com.pigeonyuze.template.data

import com.pigeonyuze.command.element.NullObject
import com.pigeonyuze.template.Parameter
import com.pigeonyuze.template.Template
import com.pigeonyuze.template.TemplateImpl
import com.pigeonyuze.util.TaskException
import com.pigeonyuze.util.isPackageName
import com.pigeonyuze.util.keyAndValueStringDataToMap
import com.pigeonyuze.util.listToStringDataToList
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.*
import kotlin.reflect.full.*
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
                KotlinReflection.KotlinRefectionProperty,
                KotlinReflection.KotlinRefectionInstanceProperty,
                KotlinReflection.KotlinRefectionSetProperty,
                KotlinReflection.KotlinRefectionSetInstanceProperty,
                /* Java */
                JavaReflection.JavaReflectionMethod,
                JavaReflection.JavaReflectionField,
                JavaReflection.JavaReflectionConstruct,
                JavaReflection.JavaReflectionSetField
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

        sealed interface JavaReflection<K : Any> : JvmReflectionTemplateImpl<K> {
            companion object {
                private fun methodRunOrNull(
                    method: Method,
                    fromObj: Any?,
                    parameter: List<*>,
                ): Any? {
                    val args = method.parameters
                    if (args.size != parameter.size) return null

                    val runArgs = runSwitchArray(args, parameter)


                    return method.invoke(fromObj, runArgs)
                }

                private fun runSwitchArray(
                    args: Array<out java.lang.reflect.Parameter>,
                    parameter: List<*>,
                ): Array<Any> {
                    val runArgs = arrayOf<Any>(args.size)
                    for ((index, value) in parameter.withIndex()) {
                        val thisParameter = args[index]

                        val name = thisParameter.type.name
                        runArgs[index] = nameCast(name, value!!)
                    }
                    return runArgs
                }


            }

            object JavaReflectionConstruct : JavaReflection<Any> {
                override val name: String
                    get() = "constructJava"
                override val type: KClass<Any>
                    get() = Any::class

                override suspend fun execute(args: Parameter): Any {
                    return args.read {
                        val name = args[0]
                        val javaClass = classValueMap.getOrPut(name) { Class.forName(name) }

                        //try to build
                        val constructorArg = list(2)
                        val all = javaClass.constructors
                        var instanceAny: Any? = null
                        for (constructor in all) {
                            if (constructor.parameters.size != constructorArg.size) continue
                            constructor.isAccessible = true
                            instanceAny =
                                constructor.newInstance(runSwitchArray(constructor.parameters, constructorArg))
                        }
                        if (instanceAny == null) {
                            throw ClassCastException("Cannot build ${javaClass.simpleName}")
                        }
                        return@read instanceAny
                    }.lastReturnValue
                }
            }

            object JavaReflectionMethod : JavaReflection<Any> {
                override val name: String
                    get() = "method"
                override val type: KClass<Any>
                    get() = Any::class

                override suspend fun execute(args: Parameter): Any {
                    return args.read {
                        val methodName = args[0]
                        val parameter = list(1)
                        val obj = next()
                        if (obj is String && obj.isPackageName()) {
                            val javaClass = Class.forName(obj)
                            val method = getMethod(javaClass, methodName, parameter).first()
                            if (!Modifier.isStatic(method.modifiers)) {
                                throw TaskException("Cannot call method $methodName, it must be static!")
                            }
                            return@read methodRunOrNull(
                                method,
                                null,
                                parameter
                            )

                        }
                        val javaClass = obj.javaClass

                        val methods = getMethod(javaClass, methodName, parameter)

                        return@read methodRunOrNull(
                            methods.first(),
                            obj,
                            parameter
                        )
                    }.lastReturnValue
                }

                private fun getMethod(
                    javaClass: Class<out Any>,
                    methodName: String,
                    parameter: List<*>,
                ): List<Method> {
                    val methods =
                        javaClass.methods.filter { it.name == methodName || it.parameters.size == parameter.size }
                    if (methods.isEmpty()) {
                        throw NoSuchMethodError("Cannot found methods $methodName with args: $parameter")
                    }
                    if (methods.size != 1) {
                        throw NoSuchMethodError("Conflicting overloads: ${
                            methods.joinToString {
                                it.toString()
                            }
                        }")
                    }
                    return methods
                }
            }

            object JavaReflectionField : JavaReflection<Any> {
                override val name: String
                    get() = "fieldJava"
                override val type: KClass<Any>
                    get() = Any::class


                override suspend fun execute(args: Parameter): Any {
                    return args.read {
                        val name = args[0]
                        if (finalValueMap.containsKey(name)) return@read finalValueMap[name]

                        val obj = next()
                        val javaClass = obj.javaClass

                        val field = javaClass.getDeclaredField(name)

                        val fieldValue: Any = field.get(obj)
                        if (Modifier.isFinal(field.modifiers)) {
                            finalValueMap[name] = fieldValue
                        }

                        return@read fieldValue
                    }.lastReturnValue
                }
            }

            object JavaReflectionSetField : JavaReflection<Boolean> {
                override val name: String
                    get() = "setFieldJava"
                override val type: KClass<Boolean>
                    get() = Boolean::class


                override suspend fun execute(args: Parameter): Boolean {
                    return args.read {
                        val name = args[0]
                        if (finalValueMap.containsKey(name)) return@read false

                        val obj = next()
                        val newValue = next()
                        val javaClass = obj.javaClass

                        val field = javaClass.getDeclaredField(name)
                        if (Modifier.isFinal(field.modifiers)) {
                            return@read false
                        }
                        field.set(obj, newValue)

                        return@read true
                    }.lastReturnValue as Boolean
                }
            }
        }

        sealed interface KotlinReflection<K : Any> : JvmReflectionTemplateImpl<K> {
            companion object {
                /**
                 * 由一个字段获取其可能的 [KProperty]
                 *
                 * 通常来说，只要不是 `java class`，其他的通常为[KProperty1] (`getter`) 或 [KProperty2] (如：扩展函数)
                 *
                 * 如果为 `java class` 则为[KProperty0]
                 *
                 * @return 这个 `val/var` 的 [KProperty.Accessor] ([KFunction])
                 * */
                private fun propertiesAccessor(
                    kotlinClass: KClass<*>,
                    name: String,
                    isGetting: Boolean = true,
                ): KProperty.Accessor<*> {
                    /* Use memberProperties, staticProperties only for Java class */
                    val properties = kotlinClass.memberProperties.filter {
                        it.name == name
                    }

                    if (properties.size != 1) {
                        conflictingOverloadsFunctionToString(properties)
                    }

                    val property = properties.first()
                    if (property is KMutableProperty<*>) {
                        return if (isGetting) property.getter else property.setter
                    }
                    if (!isGetting) {
                        throw NoSuchMethodException("Cannot found property $name 's setting function!")
                    }
                    return property.getter
                }


                private suspend inline fun propertiesSetting(
                    kClass: KClass<*>,
                    name: String,
                    obj: Any? = null,
                    newValue: Any,
                ): Any? {
                    val accessor = propertiesAccessor(
                        kClass,
                        name,
                        false
                    ) as KFunction<*>
                    return accessor.callSuspendBy(
                        mutableMapOf(
                            /* The setting function valueParameters only has value */
                            /* kotlin code: val name set(value) = ...*/
                            accessor.valueParameters.first() to newValue
                        ).let {
                            if (accessor.instanceParameter == null) {
                                return@let it
                            }

                            it[accessor.instanceParameter!!] = obj ?: runCatching {
                                kClass.createInstance()
                            }.recoverCatching { error -> // If object class are no or many such constructors
                                throw TaskException(
                                    "Cannot get $kClass 's properties, because cannot create instance",
                                    error
                                )
                            }
                            it
                        }
                    )
                }

                /**
                 * 会自动将`val/const`加入[finalValueMap]
                 *
                 * @suppress 扩展函数
                 * */
                private suspend inline fun propertiesGetting(kClass: KClass<*>, name: String, obj: Any? = null): Any? {
                    val accessor = propertiesAccessor(
                        kClass,
                        name
                    ) as KProperty1.Getter<*, *> //No extension property impl!
                    val instance = obj ?: runCatching {
                        kClass.createInstance()
                    }.recoverCatching { // If object class are no or many such constructors
                        throw TaskException(
                            "Cannot get $kClass 's properties, because cannot create instance",
                            it
                        )
                    }
                    val value = accessor.callSuspend(instance)
                    /* val -> KProperty */
                    /* var -> KMutableProperty(super: KProperty) */
                    if (accessor !is KMutableProperty1<*, *> || accessor.isConst) {
                        finalValueMap[name] = value ?: NullObject
                    }
                    return value
                }

                private fun conflictingOverloadsFunctionToString(function: List<KCallable<*>>) =
                    "Conflicting overloads: ${
                        function.joinToString(
                            " , ",
                        )
                    }"


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

            object KotlinRefectionProperty : KotlinReflection<Any> {
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

                        return@read propertiesGetting(
                            kotlinClass,
                            name
                        ) ?: NullObject
                    }.lastReturnValue
                }


            }

            object KotlinRefectionInstanceProperty : KotlinReflection<Any> {
                override val name: String
                    get() = "instancePropertyKotlin"
                override val type: KClass<Any>
                    get() = Any::class

                override suspend fun execute(args: Parameter): Any {
                    return args.read {
                        val name = args[0]
                        if (finalValueMap.containsKey(name)) return@read finalValueMap[name]
                        val classObj = next()
                        val kotlinClass = classObj::class

                        return@read propertiesGetting(
                            kotlinClass,
                            name,
                            classObj
                        ) ?: NullObject
                    }.lastReturnValue
                }


            }

            object KotlinRefectionSetProperty : KotlinReflection<Boolean> {
                override val name: String
                    get() = "setPropertyKotlin"
                override val type: KClass<Boolean>
                    get() = Boolean::class

                override suspend fun execute(args: Parameter): Boolean {
                    return args.read {
                        val name = args[0]
                        val className = args[1]
                        val newValue = next()
                        if (finalValueMap.containsKey(name)) return@read false

                        val kotlinClass = kClassValueMap.getOrPut(className) { Class.forName(className).kotlin }

                        propertiesSetting(
                            kotlinClass,
                            className,
                            newValue = newValue
                        )
                        return@read true
                    }.lastReturnValue as Boolean
                }
            }

            object KotlinRefectionSetInstanceProperty : KotlinReflection<Boolean> {
                override val name: String
                    get() = "instanceSetPropertyKotlin"
                override val type: KClass<Boolean>
                    get() = Boolean::class

                override suspend fun execute(args: Parameter): Boolean {
                    return args.read {
                        val name = args[0]
                        val classObj = next()
                        val newValue = next()
                        if (finalValueMap.containsKey(name)) return@read false

                        val kotlinClass = classObj::class

                        propertiesSetting(
                            kotlinClass,
                            name,
                            classObj,
                            newValue
                        )
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