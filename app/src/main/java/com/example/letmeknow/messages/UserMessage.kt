package com.example.letmeknow.messages;

import kotlin.reflect.typeOf

public open class UserMessage(var msgId:Short=0) : BaseMessage(){
    override fun getFields(): Array<Any> {
        return arrayOf(msgId)
    }

    override fun applyFields(objects: Array<Any>) {
        msgId=objects[0] as Short
    }
    public override fun getValue(id:String):Any{
        return when(id){
            "messageId"->msgId
            else ->super.getValue(id)
        }
    }
    public override fun  setValue(id:String, newValue:Any):Unit{
        when(id){
            "messageId"->msgId=newValue as Short
            else ->super.setValue(id,newValue)
        }
    }

    override fun getKeys(): Set<String> {
        return setOf("mesageId")
    }

    override fun getFormatString(): String {
        return "h"
    }
}
