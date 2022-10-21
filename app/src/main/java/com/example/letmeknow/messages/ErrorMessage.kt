package com.example.letmeknow.messages

class ErrorMessage(var message: String = "") : BaseMessage() {
    override fun getKeys(): Set<String> {
        return setOf(message).union(super.getKeys())
    }

    override fun getValue(name: String): Any {
        if (name.equals("message")) {
            return message
        } else return super.getValue(name)
    }

    override fun setValue(name: String, value: Any) {
        if (name.equals("message")) {
            message = value as String
        } else super.setValue(name, value)
    }
}