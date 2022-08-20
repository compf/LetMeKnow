package com.example.letmeknow.messages;

import kotlin.reflect.typeOf

public class UserMessage(var msgId:Short) : BaseMessage(){
    override fun getFields(): Array<Any> {
        return arrayOf(msgId)
    }

    override fun applyFields(objects: Array<Any>) {
        msgId=objects[0] as Short
    }

    override fun getFormatString(): String {
        return "h"
    }
}
