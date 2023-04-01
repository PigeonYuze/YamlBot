package com.pigeonyuze.util

class TaskException : Throwable {
    constructor(msg: String) : super(msg)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor() : super()
}