package ru.thegod.general


interface WrappableTo<MessageType> {

    fun wrap(to: Class<MessageType>): MessageType
}
