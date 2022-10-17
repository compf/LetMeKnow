package com.example.letmeknow.messages;

import kotlin.reflect.typeOf

public open class UserMessage(var msgId:Short=0) : BaseMessage(){

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
        return setOf("messageId").union((super.getKeys()))
    }

}
