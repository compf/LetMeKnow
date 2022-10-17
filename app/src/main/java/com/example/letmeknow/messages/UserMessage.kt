package com.example.letmeknow.messages;

import java.util.*
import kotlin.reflect.typeOf

public open class UserMessage(public var from:UUID=UUID.randomUUID() , public var to: UUID=UUID.randomUUID(),var msgId:Short=0) : BaseMessage(){

    public override fun getValue(id:String):Any{
        return when(id){
            "fromH"->from.mostSignificantBits
            "fromL"->from.leastSignificantBits
            "toH"->to.mostSignificantBits
            "toL"->to.leastSignificantBits

            "messageId"->msgId
            else ->super.getValue(id)
        }
    }
    public override fun  setValue(id:String, newValue:Any):Unit{
        when(id){
            "fromH"->from= UUID(newValue as Long,from.leastSignificantBits)
            "fromL"->from= UUID(from.mostSignificantBits,newValue as Long)
            "toH"->to= UUID(newValue as Long,to.leastSignificantBits)
            "toL"->to= UUID(to.mostSignificantBits,newValue as Long)
            "messageId"->msgId=newValue as Short
            else ->super.setValue(id,newValue)
        }
    }

    override fun getKeys(): Set<String> {
        return setOf("messageId","fromH","fromL","toH","toL").union(super.getKeys())
    }

}
