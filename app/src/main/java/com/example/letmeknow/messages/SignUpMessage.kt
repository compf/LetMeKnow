package com.example.letmeknow.messages

class SignUpMessage(var userName:String="",var authentication:String=""): BaseMessage() {
    override fun getKeys(): Set<String> {
        return setOf("messageType","time","authentication","userName")
    }

    override fun getValue(name: String): Any {
        return when(name){
            "authentication"->authentication
            "userName"->userName
            else ->super.getValue(name)
        }

    }

    override fun setValue(name: String, value: Any) {
         when(name){
            "authentication"->authentication=value as String;
            "userName"->userName=value as String
            else ->super.setValue(name,value)
        }
    }
}