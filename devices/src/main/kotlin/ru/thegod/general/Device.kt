package ru.thegod.general

interface Device<MessageType> {

    fun receive(message: MessageType): MessageType

    fun send(): MessageType 

}

// class router()
// map devices; address -> device

