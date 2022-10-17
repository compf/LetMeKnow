package com.example.letmeknow.messages

import com.example.letmeknow.util.KeyProvider
import com.example.letmeknow.util.KeyValueMapper
import com.example.letmeknow.util.MyStruct
import java.util.*
import kotlin.collections.ArrayList
import kotlin.time.milliseconds

public  abstract class BaseMessage ():KeyValueMapper {
    public var time:Long=Calendar.getInstance().timeInMillis;




    override fun getValue(name: String): Any {
       return when(name){
           "time"->time
           "messageType"->MessageClassManager.getId(this::class)
           else->0
       }

    }

    override fun getKeys(): Set<String> {
        return setOf("messageType","time")
    }

    override fun setValue(name: String,value:Any): Unit {
         when(name){
            "messageType"->{}
            "time"->time=value as Long
        }

    }




}