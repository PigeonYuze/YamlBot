package com.pigeonyuze.account

import com.pigeonyuze.util.keyAndValueStringDataToMap
import com.pigeonyuze.util.listToStringDataToList
import com.pigeonyuze.util.makeStringToAny
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.math.BigDecimal
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.starProjectedType

@Serializable
data class UserElement(
    val name: String,
    val type: String,
    val defaultValue: String
    ) {

    fun value(): String = try {
        _value.toString()
    } catch (e: Exception) { //_value 未赋值
        defaultValue
    }

    override fun toString(): String {
        return "$name = ${value()}"
    }

    @Transient
    private lateinit var _value: Any

    @Transient
    private var _type: KClass<*> = Nothing::class

    @Transient
    private var isNumber: Boolean = false

    private fun typeCastClass(classString: String = type) = when (classString.lowercase()) {
        "string", "str" -> String::class
        "long" -> Long::class
        "int", "integer" -> Int::class
        "byte" -> Byte::class
        "short" -> Short::class
        "float" -> Float::class
        "double" -> Double::class
        "boolean", "bool" -> Boolean::class
        "date" -> Date::class
        "set" -> Set::class
        "map" -> Map::class
        "list" -> List::class
        else -> Nothing::class
    }


    fun set(userElement: UserElement){
        _value = userElement._value
    }

    fun call(functionName: String, vararg varargs: Any = arrayOf()): Any? {
        val function = _type.memberFunctions.find {
            it.name.equals(functionName,true)
        } ?: error("Cannot find function $functionName")
        if (function.parameters[0].kind == KParameter.Kind.INSTANCE) {
            if (function.parameters.size == 1) return function.call(_value)
            return function.call(_value, varargs)
        }
        return function.call(varargs)
    }

    fun plusNewValue(userElement: UserElement) {
        _value = this + userElement
    }

    fun minusNewValue(userElement: UserElement) {
        _value = this - userElement
    }

    /**
     *
     * @return 相加后的[Any.toString]
     * */
    operator fun plus(userElement: UserElement): Any {
        if (userElement._type != this._type) {
            if (isNumber && userElement.isNumber) {
                return BigDecimal(userElement.value()).add(BigDecimal(value()))
            }
            if (_type == List::class && userElement._type == Set::class) {
                return ((_value as List<*>) + (userElement._value as Set<*>))
            }
            if (_type == Set::class && userElement._type == List::class) {
                return ((_value as Set<*>) + (userElement._value as List<*>))
            }
            if (_type == Date::class && userElement.isNumber) {
                return Date(
                    BigDecimal(((_value as Date).time)).add(BigDecimal(userElement.value())).longValueExact()
                )
            }
            error("Cannot add ${userElement._type} and $_type")
        }
        if (isNumber) {
            return BigDecimal(userElement.value()).add(BigDecimal(value())).toPlainString()
        } else if (userElement._type == String::class) {
            return value() + userElement.value()
        } else if (userElement._type == List::class) {
            return ((userElement._value as List<*>) + _value as List<*>)
        } else if (userElement._type == Set::class) {
            return ((userElement._value as Set<*>) + _value as Set<*>)
        } else if (userElement._type == Map::class) {
            return ((userElement._value as Map<*, *>) + _value as Map<*, *>)
        } else if (userElement._type == Date::class) {
            return ((userElement._value as Date).time + (_value as Date).time)
        } else if (userElement._type == Boolean::class) {
            return ((userElement._value as Boolean) || (_value as Boolean)) //布尔就不加了 来个 ||
        }
        error("Cannot add ${userElement._type} and $_type")
    }

    /**
     *
     * @return 相减后的[Any.toString]
     * */
    operator fun minus(userElement: UserElement): Any {
        if (userElement._type != this._type) {
            if (isNumber && userElement.isNumber) {
                return BigDecimal(userElement.value()).minus(BigDecimal(value()))
            }
            if (_type == List::class && userElement._type == Set::class) {
                return ((_value as List<*>) - (userElement._value as Set<*>))
            }
            if (_type == Set::class && userElement._type == List::class) {
                return ((_value as Set<*>) - (userElement._value as List<*>).toSet())
            }
            if (_type == Date::class && userElement.isNumber) {
                return Date(
                    BigDecimal(((_value as Date).time)).minus(BigDecimal(userElement.value())).longValueExact()
                )
            }
            error("Cannot minus ${userElement._type} and $_type")
        }
        if (isNumber) {
            return BigDecimal(userElement.value()).minus(BigDecimal(value())).toPlainString()
        } else if (userElement._type == String::class) {
            return value().replace(userElement.value(), "")
        } else if (userElement._type == List::class) {
            return ((userElement._value as List<*>) - (_value as List<*>).toSet())
        } else if (userElement._type == Set::class) {
            return ((userElement._value as Set<*>) - _value as Set<*>)
        } else if (userElement._type == Map::class) {
            return ((userElement._value as Map<*, *>) - _value as Map<*, *>)
        } else if (userElement._type == Date::class) {
            return ((userElement._value as Date).time - (_value as Date).time)
        } else if (userElement._type == Boolean::class) {
            return ((userElement._value as Boolean) || (_value as Boolean)) //布尔就不加了 来个 ||
        }
        error("Cannot minus ${userElement._type} and $_type")
    }

    init {
        initValue()
        _type = typeCastClass()
        isNumber = _type.supertypes.contains(Number::class.starProjectedType)
    }

    fun initValue() {
        val typeCastClass = typeCastClass()
        val supertypes = typeCastClass.supertypes
        val isNumber = supertypes.contains(Number::class.starProjectedType)
        val isBoolean = typeCastClass == Boolean::class
        val javaNumberClassName =
            "java.lang.${if (typeCastClass == Int::class) "Integer" else typeCastClass.simpleName}"
        val retObject: Any
        if (typeCastClass == Nothing::class) {
            error("cannot find class: $type")
        }
        if (defaultValue == "new") {
            retObject = new(isNumber, javaNumberClassName, isBoolean, typeCastClass)
            _value = retObject
            return
        }
        retObject = if (isNumber) {
            Class.forName(javaNumberClassName).getDeclaredConstructor(String::class.java).newInstance(
                if (defaultValue.last() in ('0'..'9')) defaultValue else defaultValue.dropLast(1)
            )
        } else {
            when (typeCastClass) {
                List::class -> defaultValue.listToStringDataToList().makeStringToAny()
                Map::class -> defaultValue.keyAndValueStringDataToMap().makeStringToAny()
                Set::class -> defaultValue.listToStringDataToList().makeStringToAny().toSet()
                String::class -> defaultValue
                Boolean::class -> defaultValue.toBoolean()
                else -> Date(defaultValue.toLong())
            }
        }
        _value = retObject
    }

    private fun new(
        isNumber: Boolean,
        javaNumberClassName: String,
        isBoolean: Boolean,
        typeCastClass: KClass<out Any>,
    ): Any {
        return if (isNumber)
            Class.forName(javaNumberClassName).getDeclaredConstructor(String::class.java).newInstance("0") //do not use version > 8 of jdk for build(it is @Deprecated(since = "9", forRemoval = true))
        else if (isBoolean) false
        else typeCastClass.createInstance()
    }


    operator fun compareTo(any: Any): Int {
        if (any is UserElement) {
            return this.compareTo(any)
        }
        if (any::class != _type) {
            error("Cannot ${any::class} compareTo $_type")
        }
        return sameTypeCompareTo(any)
    }


    operator fun compareTo(userElement: UserElement): Int {
        if (userElement._type != this._type) {
            if (isNumber && userElement.isNumber) {
                return BigDecimal(value()).compareTo(BigDecimal(userElement.value()))
            }
            if (_type == List::class && userElement._type == Set::class) {
                return ((_value as List<*>).size.compareTo((userElement._value as Set<*>).size))
            }
            if (_type == Set::class && userElement._type == List::class) {
                return ((_value as Set<*>).size.compareTo((userElement._value as List<*>).size))
            }
            if (_type == Date::class && userElement.isNumber) {
                return BigDecimal(((_value as Date).time)).compareTo(BigDecimal(userElement.value()))
            }
            error("Cannot ${userElement._type} compareTo $_type")
        }
        return sameTypeCompareTo(userElement._value)
    }

    private fun sameTypeCompareTo(any: Any): Int {
        if (isNumber) {
            return BigDecimal(value()).compareTo(BigDecimal(any.toString()))
        } else if (any::class == String::class) {
            return value().compareTo(any.toString())
        } else if (any::class == List::class) {
            return (_value as List<*>).size.compareTo((any as List<*>).size)
        } else if (any::class == Set::class) {
            return (_value as List<*>).size.compareTo((any as List<*>).size)
        } else if (any::class == Date::class) {
            return (_value as Date).time.compareTo((any as Date).time)
        } else if (any::class == Boolean::class) {
            return (_value as Boolean).compareTo((any as Boolean))
        }
        return 0
    }

}
