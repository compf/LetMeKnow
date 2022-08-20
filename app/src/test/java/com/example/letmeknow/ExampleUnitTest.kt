package com.example.letmeknow

import com.example.letmeknow.contacts.Contact
import com.example.letmeknow.messages.BaseMessage
import com.example.letmeknow.messages.PingMessage
import com.example.letmeknow.util.MyStruct
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun testMyStructUnpacking() {
        var dmp: Short =100
        var formatString="ih4s"
       var data= MyStruct.pack(formatString,42,dmp,"test");
        var unpacked=MyStruct.unpack(formatString,data)
        var unpacked_int=unpacked[0].to(Int).first;
        var unpacked2=unpacked[1].to(Short).first;
        var unpacked3=unpacked[2].toString();
        assertEquals("Not equal",42,unpacked_int)
        assertEquals("Not equal",dmp,unpacked2)
        assertEquals("Not equal","test",unpacked3)
    }
    var called=false
    @Test
    fun testSendReceivePingMessage(){
        val ping=PingMessage();

        val successHandler={msg:BaseMessage->run{called=true}}
        val failureHandler={msg:BaseMessage->run{fail()}}

        val msgHandler=MessageHandler(successHandler,failureHandler)
        msgHandler.start();
        msgHandler.offerMessage(ping)
        Thread.sleep(2500)
        assertTrue(called)

    }
    @Test
    fun testContactRegisterMessage(){
        val contact= Contact(50,"Timo")
        var id:Short=contact.getNextMessageId()
        assertEquals("Must be 0", 0.toShort(),id)
        contact.registerMessage("Hello")
        id++
        assertEquals("Id must be 1 as one message registered",id,contact.getNextMessageId())

    }


}