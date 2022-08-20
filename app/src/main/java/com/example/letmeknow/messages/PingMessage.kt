package com.example.letmeknow.messages

class PingMessage: BaseMessage() {
    override fun getFields(): Array<Any> {
       return arrayOf();
    }

    override fun applyFields(objects: Array<Any>) {
       // does nothing
    }

    override fun getFormatString(): String {
        return "";
    }
}