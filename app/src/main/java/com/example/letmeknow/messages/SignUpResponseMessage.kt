package com.example.letmeknow.messages

import java.util.*

class SignUpResponseMessage(public var userId:UUID=UUID.randomUUID()):BaseMessage() {
    public override fun getValue(id:String):Any{
        return when(id){
            "userH"->userId.mostSignificantBits
            "userL"->userId.leastSignificantBits
            else ->super.getValue(id)
        }
    }
    public override fun  setValue(id:String, newValue:Any):Unit{
        when(id){
            "userL"->userId= UUID(newValue.toString().toLong(),userId.leastSignificantBits)
            "userH"->userId= UUID(userId.mostSignificantBits,newValue.toString().toLong())
            else ->super.setValue(id,newValue)
        }
    }

    override fun getKeys(): Set<String> {
        return setOf("userH","userL").union(super.getKeys())
    }
}