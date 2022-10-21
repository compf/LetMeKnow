package com.example.letmeknow.messages

class MessageTextMessage(var text: String) : UserMessage() {


    override fun getValue(name: String): Any {
        return when (name) {
            "text" -> text
            else -> super.getValue(name)

        }
    }

    override fun setValue(id: String, newValue: Any) {
        when (id) {
            "text" -> text = newValue.toString()
            else -> super.setValue(id, newValue)

        }
    }


}