package com.example.letmeknow.messages

import java.util.*

open class UserMessage(var from: UUID = UUID.randomUUID(), var to: UUID = UUID.randomUUID(), var msgId: Short = 0) :
    BaseMessage() {

    override fun getValue(id: String): Any {
        return when (id) {
            "fromH" -> from.mostSignificantBits
            "fromL" -> from.leastSignificantBits
            "toH" -> to.mostSignificantBits
            "toL" -> to.leastSignificantBits

            "messageId" -> msgId
            else -> super.getValue(id)
        }
    }

    override fun setValue(id: String, newValue: Any): Unit {
        when (id) {
            "fromH" -> from = UUID(newValue.toString().toLong(), from.leastSignificantBits)
            "fromL" -> from = UUID(from.mostSignificantBits, newValue.toString().toLong())
            "toH" -> to = UUID(newValue.toString().toLong(), to.leastSignificantBits)
            "toL" -> to = UUID(to.mostSignificantBits, newValue.toString().toLong())
            "messageId" -> msgId = newValue.toString().toShort()
            else -> super.setValue(id, newValue)
        }
    }

    override fun getKeys(): Set<String> {
        return setOf("messageId", "fromH", "fromL", "toH", "toL").union(super.getKeys())
    }

}
