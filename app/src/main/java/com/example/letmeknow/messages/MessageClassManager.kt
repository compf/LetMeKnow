package com.example.letmeknow.messages

import com.example.letmeknow.util.BiMap
import kotlin.reflect.KClass

class MessageClassManager {
    companion object{
        val bimap= BiMap<KClass<out BaseMessage>,Short>()



        init{
            val classes= arrayOf(UserMessage::class,MessageTextMessage::class,AcknowledgementMessage::class,
            PingMessage::class)
            for( i in 1 .. classes.size){
                register(classes[i-1], i.toShort())
            }


        }

        fun register(msg:KClass<out BaseMessage>,id:Short) :Short{
           if(!bimap.containsEither(msg,id)){
               bimap.put(msg,id)
           }

            return id
        }
        fun newInstance(id:Short):BaseMessage{
           return bimap.getBA(id)?.java?.newInstance()!!

        }

        fun getId(kClass: KClass<out BaseMessage>): Short {
            return bimap.getAB(kClass)!!
        }

    }

}