package com.example.letmeknow.messages

class MessageTextMessage(var msgId:Short,var msg:String):BaseMessage() {
    override fun getFields(): Array<Any> {
        return  arrayOf(msg.length,msg)
    }

    override fun applyFields(objects: Array<Any>) {
        msg=objects[1].toString()
    }

    override fun getFormatString(): String {
        return "h${msg.length}s"
    }
}