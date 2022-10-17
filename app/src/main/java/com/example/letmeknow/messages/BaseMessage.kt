package com.example.letmeknow.messages

import com.example.letmeknow.util.KeyProvider
import com.example.letmeknow.util.KeyValueMapper
import com.example.letmeknow.util.MyStruct
import java.util.*
import kotlin.collections.ArrayList
import kotlin.time.milliseconds

public  abstract class BaseMessage (public var from:UUID=UUID.randomUUID() , public var to: UUID=UUID.randomUUID()):KeyValueMapper {
    public var time:Long=Calendar.getInstance().timeInMillis;
    companion object{

    }



    override fun getValue(name: String): Any {
       return when(name){
           "messageType"->MessageClassManager.getId(this::class)
           "fromH"->from.mostSignificantBits
           "fromL"->from.leastSignificantBits
           "toH"->to.mostSignificantBits
           "toL"->to.leastSignificantBits
           "time"->time
           else->0
       }

    }

    override fun getKeys(): Set<String> {
        return setOf("messageType","fromH","fromL","toH","toL","time")
    }

    override fun setValue(name: String,value:Any): Unit {
         when(name){
            "messageType"->{}
            "fromH"->from=UUID(value as Long,from.leastSignificantBits)
            "fromL"->from=UUID(from.mostSignificantBits,value as Long)
             "toH"->to=UUID(value as Long,to.leastSignificantBits)
             "toL"->to=UUID(to.mostSignificantBits,value as Long)
            "time"->time=value as Long
        }

    }

    /**
     * Deserializes the byte array to the instance, the byte array should not contain the total length
     * @param the byte array given that should be unpacked
     */



}