package com.pigeonyuze.util

@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class FunctionArgsSize(
    val sizes : IntArray
)

@Repeatable
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ArgComment(
    val size : Int,
    val comment : Array<String>
)

@Target(AnnotationTarget.CLASS)
@Repeatable
annotation class SerializerData(val buildIndex: Int, val serializerJSONType: SerializerType){
    enum class SerializerType{
        MESSAGE,
        SUBJECT_ID,
        EVENT_ALL,
        SENDER_NAME,
        SENDER_NICK,
        SENDER_ID,
        CONTACT
    }

}

@Target(AnnotationTarget.CLASS)
@NonGroup
annotation class ReadObject

@Target(AnnotationTarget.CLASS)
@NonGroup
annotation class SettingObject

@Target(AnnotationTarget.CLASS)
annotation class NonGroup

@DslMarker
annotation class DslParameterReader

@DslMarker
annotation class DslEventTemplateBuilder