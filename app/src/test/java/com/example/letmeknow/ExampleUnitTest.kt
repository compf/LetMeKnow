package com.example.letmeknow

import com.example.letmeknow.contacts.Contact
import com.example.letmeknow.util.MyStruct
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun testMyStructUnpacking() {
        val dmp: Short = 100
        val formatString = "ih4s"
        val data = MyStruct.pack(formatString, 42, dmp, "test")
        val unpacked = MyStruct.unpack(formatString, data)
        val unpackedInt = unpacked[0].to(Int).first
        val unpacked2 = unpacked[1].to(Short).first
        val unpacked3 = unpacked[2].toString()
        assertEquals("Not equal", 42, unpackedInt)
        assertEquals("Not equal", dmp, unpacked2)
        assertEquals("Not equal", "test", unpacked3)
    }

    var called = false


    @Test
    fun testContactRegisterMessage() {
        val contact = Contact(50, "Timo")
        var id: Short = contact.getNextMessageId()
        assertEquals("Must be 0", 0.toShort(), id)
        contact.registerMessage("Hello")
        id++
        assertEquals("Id must be 1 as one message registered", id, contact.getNextMessageId())

    }


}