package com.example.letmeknow.messages

import com.example.letmeknow.util.KeyValueMapper
import java.util.*

abstract class BaseMessage : KeyValueMapper {
    var time: Long = Calendar.getInstance().timeInMillis


    override fun getValue(name: String): Any {
        return when (name) {
            "time" -> time
            "messageType" -> MessageClassManager.getId(this::class)
            else -> 0
        }

    }

    override fun getKeys(): Set<String> {
        return setOf("messageType", "time")
    }

    override fun setValue(name: String, value: Any): Unit {
        when (name) {
            "messageType" -> {
            }
            "time" -> time = value.toString().toLong()
        }

    }


}