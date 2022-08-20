package com.example.letmeknow.messages

class AcknowledgementMessage: BaseMessage() {
    override fun getFields(): Array<Any> {
        return emptyArray()
    }

    override fun applyFields(objects: Array<Any>) {
        //do nothing
    }

    override fun getFormatString(): String {
        return ""
    }
}