package com.example.letmeknow.messages

class MessageTextMessage(var text:String):UserMessage() {
    override fun getFields(): Array<Any> {
        return  arrayOf(text.length,text)
    }

    override fun applyFields(objects: Array<Any>) {
        text=objects[1].toString()
    }

    override fun getValue(name: String): Any {
        return when(name){
            "text"->text
            else ->super.getValue(name)

        }
    }

    override fun setValue(id: String, newValue: Any) {
         when(id){
            "text"->text=newValue.toString()
            else ->super.setValue(id, newValue)

        }
    }

    override fun getFormatString(): String {
        return "h${text.length}s"
    }
}