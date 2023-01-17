package com.pigeonyuze.util


@MustBeDocumented
@Target(AnnotationTarget.CLASS)
annotation class FunctionArgsSize(
    val sizes : IntArray
)

@Repeatable
@Target(AnnotationTarget.FUNCTION)
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
        COMMAND_ID
    }

}