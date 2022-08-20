package com.example.letmeknow.contacts

import com.example.letmeknow.util.BiMap
import java.util.stream.Stream

class Contact (val id:Int,val name:String, val messages: BiMap<Short, String> = BiMap()){
    fun registerMessage(msgId:Short,msg:String){
        messages.put(msgId,msg)
    }
    fun registerMessage(msg:String){
        registerMessage(getNextMessageId(),msg)
    }

    /**
     * Get the next message id which can be used by registerMessage
     * @return 0 if no messages are registered otherwise the max id of the registered messages plus 1
     */
    fun getNextMessageId():Short{
       return  ((messages.iterA().maxOrNull()?:-1)+1).toShort()
    }
}