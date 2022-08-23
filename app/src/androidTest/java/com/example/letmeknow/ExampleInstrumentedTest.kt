package com.example.letmeknow

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.letmeknow.messages.BaseMessage
import com.example.letmeknow.messages.UserMessage
import com.example.letmeknow.util.Encoder
import com.example.letmeknow.util.KeyProvider
import com.example.letmeknow.util.KeyValueMapper

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import java.util.*
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
public class ExampleInstrumentedTest {
    class StubKeyProvider:KeyProvider{
        override fun getKey(keyId: String, mapper: KeyValueMapper): SecretKey {
            return SecretKeySpec(arrayOf<Byte>(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15).toByteArray(),"AES")
        }

    }
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val encoder=Encoder()
        val msg=UserMessage(2)
        msg.to= UUID(1,1)
        msg.from= UUID(10,18)
        msg.time=5112
        val keyProvider=StubKeyProvider()
        val bytes=encoder.convertToBytes(msg,appContext,"UserMessage",keyProvider)

        val convMessage=encoder.convertToMessage(bytes,appContext,"UserMessage",keyProvider)
        assertEquals(convMessage.from,msg.from)
        assertEquals(convMessage.to,msg.to)
        assertEquals(convMessage.time,msg.time)
        assertEquals((convMessage as UserMessage).msgId,(msg as UserMessage).msgId)


    }
}