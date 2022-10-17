package com.example.letmeknow

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.letmeknow.messages.BaseMessage
import com.example.letmeknow.messages.SignUpMessage
import com.example.letmeknow.messages.SignUpResponseMessage
import com.example.letmeknow.messages.UserMessage
import com.example.letmeknow.util.Encoder
import com.example.letmeknow.util.KeyProvider
import com.example.letmeknow.util.KeyValueMapper

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import java.lang.Exception
import java.security.Key
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.spec.KeySpec
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
public class ExampleInstrumentedTest {
    open class StubKeyProvider:KeyProvider{
        override fun getKey(keyId: String): Key {
            return SecretKeySpec(arrayOf<Byte>(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15).toByteArray(),"AES")
        }

    }
    class ExtendedStubKeyProvider:StubKeyProvider(){
        private val keyPair=KeyPairGenerator.getInstance("RSA").genKeyPair()
        override fun getKey(keyId: String): Key {
            if(keyId=="SERVER_PUBLIC"){
               return keyPair.public
            }
            else if(keyId=="SERVER_PRIVATE"){
                return keyPair.private
            }
            else return super.getKey(keyId)
        }

        override fun initCipher(cipher: Cipher, encryptMode: Int, keyId: String, iv: IvParameterSpec) {
          if(keyId=="SERVER_PUBLIC" || keyId=="SERVER_PRIVATE"){
              cipher.init(encryptMode,getKey(keyId))
          }
            else{
                super.initCipher(cipher, encryptMode, keyId, iv)
          }

        }

    }
    public   class StubServer(capacity:Int){
        public val queue:BlockingQueue<ByteArray> = ArrayBlockingQueue(capacity)
        public  fun receive():ByteArray{
            return queue.poll(5,TimeUnit.SECONDS)
        }
        public fun send(array:ByteArray){
            otherSide!!.queue.offer(array)
        }
        public var otherSide:StubServer?=null
    }
    @Test
    fun testSignUp(){
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val client=StubServer(1)
        val server=StubServer(1)
        val userID=UUID(1,1)
        client.otherSide=server
        server.otherSide=client
        val stubKeyProvider=ExtendedStubKeyProvider()
        val signUpSent=SignUpMessage("compf","test123")
        val encoder=Encoder()
        val signUpBytes= encoder.convertToBytes(signUpSent,appContext,"SignUpMessage",stubKeyProvider)
        client.send(signUpBytes)
        val serverThread=Thread({->
            try {
                val bytesReceived=server.receive()
                val signupReceived=encoder.convertToMessage(signUpBytes,appContext,"SignUpMessage",stubKeyProvider)
                val signUpResponseMessage=SignUpResponseMessage(userID)
                val bytesSent= encoder.convertToBytes(signUpResponseMessage,appContext,"SignUpMessageResponse",stubKeyProvider)
                server.send(bytesSent)
            }catch (exception:Exception){
                exception.printStackTrace()
            }


        });
        serverThread.start()
        val bytesReceived=client.receive()
        val signUpResponseReceived=encoder.convertToMessage(bytesReceived,appContext,"SignUpMessageResponse",stubKeyProvider ) as SignUpResponseMessage;
        assertEquals(signUpResponseReceived.userId,userID)
    }
    @Test
    fun testMessageConversion() {

        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val encoder=Encoder()
        val msg=UserMessage(UUID(1,1),UUID(10,18),2)
        msg.time=5112
        val keyProvider=StubKeyProvider()
        val bytes=encoder.convertToBytes(msg,appContext,"UserMessage",keyProvider)

        val convMessage=encoder.convertToMessage(bytes,appContext,"UserMessage",keyProvider) as UserMessage
        assertEquals(convMessage.from,msg.from)
        assertEquals(convMessage.to,msg.to)
        assertEquals(convMessage.time,msg.time)
        assertEquals((convMessage as UserMessage).msgId,(msg as UserMessage).msgId)
        assertNotEquals(convMessage,msg)


    }
}